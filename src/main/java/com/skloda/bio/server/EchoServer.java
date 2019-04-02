package com.skloda.bio.server;

import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class EchoServerHandler implements AutoCloseable {
    private ServerSocket serverSocket;
    private boolean serverFlag = true;

    EchoServerHandler() throws Exception {
        this.serverSocket = new ServerSocket(9999);   // 进行服务端的Socket启动
        System.out.println("ECHO服务器端已经启动了，该服务在" + 9999 + "端口上监听....");
        this.runServer();
    }

    void runServer() throws Exception {
        // 线程池处理每个客户端连接
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        while (serverFlag) {
            Socket client = this.serverSocket.accept(); // 等待客户端连接
            executorService.submit(() -> {
                try {
                    Scanner scan = new Scanner(client.getInputStream());// 服务器端输入为客户端输出
                    PrintStream out = new PrintStream(client.getOutputStream());//服务器端的输出为客户端输入
                    scan.useDelimiter("\n"); // 设置分隔符
                    boolean clientFlag = true;
                    while (clientFlag) {
                        if (scan.hasNext()) {    // 现在有内容
                            String inputData = scan.next(); // 获得输入数据
                            System.out.println("接收到[" + client.getRemoteSocketAddress() + "]发送的数据:" + inputData);
                            if ("exit".equalsIgnoreCase(inputData)) {   // 信息结束
                                clientFlag = false; // 结束内部的循环
                                out.println("【ECHO】Bye Bye ... "); // 一定需要提供有一个换行机制，否则Scanner不好读取
                            } else {
                                out.println("【ECHO】" + inputData); // 回应信息
                            }
                        }
                    }
                    client.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void close() throws Exception {
        this.serverFlag = false;
        this.serverSocket.close();
    }
}

/**
 * 实现服务器端的编写开发，采用BIO（阻塞模式）实现开发的基础结构
 */
public class EchoServer {
    public static void main(String[] args) throws Exception {
        try (EchoServerHandler echoServerHandler = new EchoServerHandler()) {
            echoServerHandler.runServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

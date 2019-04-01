package com.skloda.nio;

import com.skloda.util.ServerInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-03-31 18:00
 */
public class NIOEchoServer {
    public static void main(String[] args) {
        try {
            new Server(ServerInfo.SERVER_PORT).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * NIO Server
 */
class Server implements AutoCloseable {
    private Selector selector;
    private ServerSocketChannel ssChannel;
    private volatile boolean stop = false;

    Server(int port) {
        try {
            //等价于 Selector selector = SelectorProvider.provider().openSelector();
            selector = Selector.open();
            //等价于 SelectorProvider.provider().openServerSocketChannel()
            ssChannel = ServerSocketChannel.open();
            ssChannel.configureBlocking(false);
            ssChannel.bind(new InetSocketAddress(port));
            //ServerSocketChannel主要注册OP_ACCEPT兴趣事件
            ssChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NIO服务器端程序启动，该程序在" + port + "端口上进行监听...");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    void run() throws IOException {
        while (!stop) {
            // selector线程最大阻塞时间5s，此处会阻塞直到某个io事件发生
            if (selector.select(5000) == 0)
                continue;

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = readyKeys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                try {
                    if (key.isAcceptable()) {
                        // OP_ACCEPT事件key中对应的是ServerSocketChannel，其它事件对应的是SocketChannel
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = ssc.accept();
                        System.out.println("接收到客户数据包，来自[" + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + "]");
                        socketChannel.configureBlocking(false);
                        // 初始化缓冲区并放入key的附件
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        // 为selector注册当前channel的读写事件
                        socketChannel.register(selector, SelectionKey.OP_READ, buffer);
                    }
                    if (key.isReadable()) {
                        System.out.println("准备读取客户端发送的数据...");
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        buffer.clear();
                        int readCount = socketChannel.read(buffer);
                        buffer.flip();
                        String requestStr = StandardCharsets.UTF_8.decode(buffer).toString();
                        System.out.println("接收到客户端发送报文:" + requestStr);

                        // 读取小于零代表客户端退出
                        if (readCount < 0) {
                            key.cancel();
                            socketChannel.close();
                        } else {
                            doServerLogic(requestStr, key);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    it.remove();
                }
            }
        }
    }

    private void doServerLogic(String requestStr, SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        try {
            // 模拟业务执行时间
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] responseBytes = ("【ECHO】: " + requestStr).getBytes(StandardCharsets.UTF_8);
        // 写缓存前先清空
        buffer.clear();
        // NIO ByteBuffer的缺陷之一，固定长度不可变，需初始化分配较大空间
        // Netty中的ByteBuf类是可变缓冲区，由于双指针设计，读写切换不需要flip()
        buffer.put(responseBytes);
        buffer.flip();
        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        this.stop = true;
        this.ssChannel.close();
    }
}
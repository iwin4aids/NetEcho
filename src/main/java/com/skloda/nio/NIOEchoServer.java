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
            //ServerSocketChannel注册OP_ACCEPT兴趣事件
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
                        System.out.println("接收到客户数据包，来自[：" + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + "]");
                        socketChannel.configureBlocking(false);
                        ByteBuffer buffer = ByteBuffer.allocate(4);
                        socketChannel.register(selector, SelectionKey.OP_READ, buffer);
                    }
                    if (key.isConnectable()) {
                        System.out.println("connect successfully...");
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        if (socketChannel.isConnectionPending()) {
                            socketChannel.finishConnect();
                        }
                        System.out.println("与客户端TCP连接建立成功，，来自[：" + socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort() + "]");
                    }
                    if (key.isReadable()) {
                        System.out.println("准备读取客户端发送的数据...");
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        StringBuffer sb = new StringBuffer();
                        int readCount;
                        while ((readCount = socketChannel.read(buffer)) > 0) {
                            System.out.println(readCount);
                            buffer.flip();
                            byte[] bytes = new byte[readCount];
                            buffer.get(bytes);
                            // 拼接合并，针对客户端大报文在缓冲区存不下的情况
                            sb.append(new String(bytes));
                            buffer.clear();
                        }
                        System.out.println("客户端发送报文:" + sb.toString());

                        if (readCount < 0) {
                            key.cancel();
                            socketChannel.close();
                        } else {
                            doServerLogic(sb.toString(), key);
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
        byte[] responseBytes = ("【Server ECHO】: " + requestStr).getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        while (pos < responseBytes.length) {
            buffer.clear();
            if (pos + buffer.remaining() < responseBytes.length) {
                buffer.put(responseBytes, pos, buffer.remaining());
                pos = pos + buffer.remaining();
            } else {
                buffer.put(responseBytes, pos, responseBytes.length - 1 - pos);
                pos = responseBytes.length - 1;
            }
            buffer.flip();
            try {
                socketChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws Exception {
        this.stop = true;
        this.ssChannel.close();
    }
}
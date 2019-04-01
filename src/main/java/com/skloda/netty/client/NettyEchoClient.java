package com.skloda.netty.client;

import com.skloda.util.ServerInfo;
import com.sun.security.ntlm.Server;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Date;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-03-29 16:56
 */
public class NettyEchoClient {

    public static void main(String[] args) {
        new NettyClient(ServerInfo.SERVER_HOST, ServerInfo.SERVER_PORT).run();
    }

}

class NettyClient {

    private String host;
    private int port;

    NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    void run() {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        // 设置线程组
        bootstrap.group(group)
                // 设置线程模型
                .channel(NioSocketChannel.class)
                // 设置连接读写处理逻辑
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(new StringEncoder());
                    }
                });
        // 连接指定地址的指定端口
        ChannelFuture connect = bootstrap.connect(host, port);
        // 判断是否连接成功
        // 实际开发中 在此处肯定是要添加连接重试的逻辑的
        connect.addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("连接成功!");
            } else {
                System.err.println("连接失败!");
            }
        });
        Channel channel = connect.channel();
        while (true) {
            channel.writeAndFlush(new Date() + ": hello world!");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
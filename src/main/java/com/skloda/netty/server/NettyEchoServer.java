package com.skloda.netty.server;

import com.skloda.util.ServerInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-03-29 16:55
 */
public class NettyEchoServer {
    public static void main(String[] args) {
        new NettyServer(ServerInfo.SERVER_PORT).run();
    }
}

class NettyServer {

    private int port;

    NettyServer(int port) {
        this.port = port;
    }

    void run() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // boss 对应 IOServer.java 中的接受新连接线程，主要负责创建新连接
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // worker 对应 IOClient.java 中的负责读取数据的线程，主要用于读取数据以及业务逻辑处理
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new StringDecoder());
                        channel.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                System.out.println(msg);
                            }
                        });
                    }
                })
                .bind(port);
        System.out.println("Netty服务器端程序启动，该程序在" + port + "端口上进行监听...");

    }

}

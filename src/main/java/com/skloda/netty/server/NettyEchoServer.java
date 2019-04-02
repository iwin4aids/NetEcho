package com.skloda.netty.server;

import com.skloda.util.ServerInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;

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
                // 配置使用NIOServerSocketChannel
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        // 下面在pipeline中注册各种处理器（类似aop的前置拦截器，在从channel读取/写入内容时先执行解码/编码等拦截器实现公共逻辑）
                        // Netty解决TCP粘包问题的优雅方案，头部先写入4字节标识包的length
                        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                        // 注册处理字符串的一对编解码器
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast(new StringDecoder());
                        // 注册自定义的逻辑处理ChannelInboundHandlerAdapter
                        pipeline.addLast(new EchoServerHandler());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            ChannelFuture f = serverBootstrap.bind(port).sync();
            System.out.println("Netty服务器端程序启动，该程序在" + port + "端口上进行监听...");
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

/**
 * netty中需要自己实现的handler
 *
 * @ChannelHandler.Sharable 标记该handler可以被多个channel共享
 */
@ChannelHandler.Sharable
class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            System.out.println("接受到客户端请求消息:" + msg.toString());
            String response = "【ECHO】" + msg.toString();
            ctx.writeAndFlush(response); // 回应的输出操作
        } finally {
            ReferenceCountUtil.release(msg); // 释放缓存
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
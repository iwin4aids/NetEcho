package com.skloda.netty.client;

import com.skloda.util.ServerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;

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
        // 在Netty中考虑到代码的统一性，也允许你在客户端设置线程池，客户端一般一个线程即可
        EventLoopGroup group = new NioEventLoopGroup(); // 创建一个线程池
        try {
            Bootstrap client = new Bootstrap(); // 创建客户端处理程序
            client.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true) // 允许接收大块的返回数据
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // Netty解决TCP粘包问题的优雅方案
                            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                            // 注册处理字符串的一对编解码器
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new StringDecoder());
                            // 注册自定义的逻辑处理ChannelInboundHandlerAdapter
                            pipeline.addLast(new EchoClientHandler());
                        }
                    });
            ChannelFuture channelFuture = client.connect(ServerInfo.SERVER_HOST, ServerInfo.SERVER_PORT).sync();
            channelFuture.channel().closeFuture().sync(); // 关闭连接
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }
}

@ChannelHandler.Sharable
class EchoClientHandler extends ChannelInboundHandlerAdapter {

    private static final int REPEAT = 500;// 消息重复发送次数

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel就绪，准备发送数据");
        // Netty原生还是只能写ByteBuf，此处能直接写字符串原因是上面注册了StringEncoder
        // 同理如果要写对象，也需要注册ObjectEncoder（可自己实现序列化和反序列化器)
        ctx.writeAndFlush("Hello, Netty");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 只要服务器端发送完成信息之后，都会执行此方法进行内容的输出操作
        try {
            System.out.println(msg); // 输出服务器端的响应内容
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
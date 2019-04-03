package com.skloda.netty.server;

import com.skloda.netty.serializer.*;
import com.skloda.util.ServerInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.ReferenceCountUtil;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        // boss 对应 IOServer.java 中的接受新连接线程，主要负责轮询并创建新连接
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // worker 对应 IOClient.java 中的负责读取数据的线程，主要用于从channel读写数据
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
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));
                        // 注册处理字符串的一对编解码器
//                        pipeline.addLast(new StringEncoder());
//                        pipeline.addLast(new StringDecoder());
                        // 这2个是自定义的一对编解码器，可以直接读写对象，可以扩展实现自己的序列化和反序列化实现
                        pipeline.addLast(new JSONEncoder());
                        pipeline.addLast(new JSONServerDecoder());
                        // 注册自定义的逻辑处理ChannelInboundHandlerAdapter
                        pipeline.addLast(new EchoServerHandler());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

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
class EchoServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private ExecutorService executorService = Executors.newFixedThreadPool(20);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        try {
            System.out.println("接受到客户端请求消息:" + rpcRequest);
            // 这里可以调用业务处理线程池来处理增加并发处理能力，否则是单线程处理
            executorService.submit(()->{
                RpcResponse rpcResponse = doBussiness(rpcRequest);
                ctx.writeAndFlush(rpcResponse); // 回应的输出操作
            });
        } finally {
            ReferenceCountUtil.release(rpcRequest); // 释放缓存
        }
    }

    /**
     * 模拟一下业务操作
     * @param rpcRequest 请求封装
     * @return RpcResponse
     */
    private RpcResponse doBussiness(RpcRequest rpcRequest) {
        Object[] args = rpcRequest.getArgs();
        ResponseDTO dto = new ResponseDTO(Arrays.toString(args), Arrays.asList("admin", "operator"));
        try {
            // 模拟服务端业务处理耗时
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new RpcResponse(rpcRequest.getRequestId(), 200, dto);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
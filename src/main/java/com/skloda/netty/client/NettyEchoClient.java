package com.skloda.netty.client;

import com.skloda.netty.serializer.JSONClientDecoder;
import com.skloda.netty.serializer.JSONEncoder;
import com.skloda.netty.serializer.RpcRequest;
import com.skloda.netty.serializer.RpcResponse;
import com.skloda.util.ServerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.ReferenceCountUtil;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-03-29 16:56
 */
public class NettyEchoClient {

    public static final int PARALLEL_COUNT = 10;

    public static void main(String[] args) {
        System.out.println("============All Test Begin===========");
        NettyClient nettyClient = new NettyClient(ServerInfo.SERVER_HOST, ServerInfo.SERVER_PORT);
        nettyClient.start();
        CountDownLatch latch = new CountDownLatch(PARALLEL_COUNT);
        ExecutorService executorService = Executors.newFixedThreadPool(PARALLEL_COUNT);

        for (int i = 0; i < PARALLEL_COUNT; i++) {
            executorService.submit(() -> {
                long s1 = System.currentTimeMillis();
                RpcResponse rpcResponse = nettyClient.doInvoke(new RpcRequest(UUID.randomUUID().toString(), "helloService", "hello", new String[]{Thread.currentThread().getName()}));
                long s2 = System.currentTimeMillis();
                System.out.println("RPC共耗时" + (s2 - s1) + "毫秒，服务端响应报文:" + rpcResponse);
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("============All Test End===========");
        nettyClient.close();
        System.exit(0);
    }

}

class NettyClient {

    // 客户端配置
    private String host;
    private int port;

    // netty 相关组件
    private Channel channel;
    private EventLoopGroup group;

    NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    void start() {
        // 在Netty中考虑到代码的统一性，也允许你在客户端设置线程池，客户端一般一个线程即可
        this.group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap(); // 创建客户端处理程序
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true) // 允许接收大块的返回数据
                    .option(ChannelOption.SO_KEEPALIVE, true) // 保持长连接
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 设置TCP连接超时时间
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // Netty解决TCP粘包问题的优雅方案
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            // 注册处理字符串的一对编解码器
//                            pipeline.addLast(new StringEncoder());
//                            pipeline.addLast(new StringDecoder());
                            // 这2个是自定义的一对编解码器，可以直接读写对象，可以扩展实现自己的序列化和反序列化实现
                            pipeline.addLast(new JSONEncoder());
                            pipeline.addLast(new JSONClientDecoder());//这里客户端和服务端需要反序列化的对象不一样
                            // 注册自定义的逻辑处理ChannelInboundHandlerAdapter，这个必须放在最后
                            pipeline.addLast(new EchoClientHandler());
                        }
                    });
            // 保存到成员变量
            this.channel = bootstrap.connect(host, port).sync().channel();
            // valid
            if (!isValid()) {
                System.out.println("未能成功获取channel，客户端线程退出");
                close();
            }
//            this.channel.closeFuture().sync(); // 等待直到关闭
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid() {
        if (this.channel != null) {
            return this.channel.isActive();
        }
        return false;
    }

    public void close() {
        if (this.channel != null && this.channel.isActive()) {
            this.channel.close();
        }
        if (this.group != null && !this.group.isShutdown()) {
            this.group.shutdownGracefully();
        }
    }

    public RpcResponse doInvoke(RpcRequest rpcRequest) {
        this.channel.writeAndFlush(rpcRequest);
        System.out.println("客户端请求报文:" + rpcRequest);
        RpcContext rpcContext = new RpcContext(rpcRequest);
        RpcContextHolder.put(rpcRequest.getRequestId(), rpcContext);
        return rpcContext.get(); // 这里会阻塞
    }
}

@ChannelHandler.Sharable
class EchoClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        // 只要服务器端发送完成信息之后，都会执行此方法进行内容的输出操作
        try {
            // 服务端的回应放入响应结果池
            String requestId = rpcResponse.getRequestId();
            RpcContext rpcContext = RpcContextHolder.get(requestId);
            rpcContext.fillResponseAndNotity(rpcResponse);
        } finally {
            ReferenceCountUtil.release(rpcResponse); // 释放缓存
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
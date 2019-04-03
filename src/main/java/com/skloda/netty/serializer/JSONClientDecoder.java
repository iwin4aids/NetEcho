package com.skloda.netty.serializer;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-04-03 10:21
 */
public class JSONClientDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf msg, List<Object> list) throws Exception {
        int len = msg.readableBytes(); // 可以用的数据长度
        byte[] data = new byte[len];
        msg.getBytes(msg.readerIndex(), data, 0, len);
        list.add(JSON.parseObject(new String(data, StandardCharsets.UTF_8)).toJavaObject(RpcResponse.class));
    }
}
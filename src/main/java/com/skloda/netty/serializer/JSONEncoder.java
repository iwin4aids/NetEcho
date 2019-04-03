package com.skloda.netty.serializer;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-04-03 10:19
 */
public class JSONEncoder extends MessageToByteEncoder<Object> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf out) throws Exception {
        out.writeBytes(JSONObject.toJSONString(msg).getBytes(StandardCharsets.UTF_8));
    }
}
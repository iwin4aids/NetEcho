package com.skloda.netty.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: jiangkun
 * @Description: 全局存放rpcContext对象（每次请求生成一个，响应后删除）
 * @Date: Created in 2019-04-03 17:53
 */
public class RpcContextHolder {

    private static ConcurrentMap<String, RpcContext> contextPool = new ConcurrentHashMap<>();

    public static void put(String requestId, RpcContext rpcContext) {
        contextPool.putIfAbsent(requestId, rpcContext);
    }

    public static RpcContext get(String requestId){
        return contextPool.remove(requestId);
    }
}

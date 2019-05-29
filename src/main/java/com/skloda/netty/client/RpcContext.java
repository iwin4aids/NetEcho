package com.skloda.netty.client;

import com.skloda.netty.serializer.RpcRequest;
import com.skloda.netty.serializer.RpcResponse;
import lombok.Data;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: jiangkun
 * @Description: 必须线程安全
 * @Date: Created in 2019-04-03 17:31
 */
@Data
public class RpcContext {

    private RpcRequest rpcRequest;
    private RpcResponse rpcResponse;
    private volatile boolean isDone = false;

    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    /**
     * 加锁等待远端返回并返回结果
     * @return
     */
    public RpcResponse get() {
        try {
            lock.lock();
            condition.await(30000, TimeUnit.MILLISECONDS);
            if (isDone) {
                return this.rpcResponse;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return null;
    }


    /**
     * 设置返回结果并唤醒请求线程
     *
     * @param rpcResponse
     */
    public void fillResponseAndNotity(RpcResponse rpcResponse) {
        this.rpcResponse = rpcResponse;
        this.isDone = true;
        try {
            lock.lock();
            // 已填充了响应，此时可以唤醒阻塞线程返回结果了
            condition.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public RpcContext(RpcRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
    }
}

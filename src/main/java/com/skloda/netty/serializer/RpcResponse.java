package com.skloda.netty.serializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-04-03 16:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcResponse implements Serializable {
    private String requestId;
    private int code;
    private Object result;
}

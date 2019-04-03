package com.skloda.netty.serializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-04-03 10:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest implements Serializable {

    private String requestId;
    private String serviceBeanName;
    private String methodName;
    private Object[] args;
}

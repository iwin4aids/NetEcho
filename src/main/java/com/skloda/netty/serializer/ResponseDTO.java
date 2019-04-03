package com.skloda.netty.serializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-04-03 16:25
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO {

    private String name;
    private List<String> roles;

}

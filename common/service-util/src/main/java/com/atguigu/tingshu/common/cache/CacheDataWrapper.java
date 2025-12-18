package com.atguigu.tingshu.common.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 该类用来解决当切面类的环绕方法要将数据存到 redis 中，而数据是list时，那么反序列化会失败，因为 对于这样的结构，spring 无法自动的序列化，当它遇到[,但没有该数组的类型时，就会报错[{}..]
 * @author liutianba7
 * @create 2025/12/18 18:22
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheDataWrapper {
    private Object data;
}

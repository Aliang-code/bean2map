package com.netease.bean2map.codec;

import java.util.Map;

public interface IMapCodec<T> {
    /**
     * javabean转map
     *
     * @param entity
     * @return
     */
    Map<String, Object> code(T entity);

    /**
     * map转javabean
     *
     * @param map
     * @return
     */
    T decode(Map<String, Object> map);

    /**
     * map过滤javabean不存在的元素
     *
     * @param map
     * @return
     */
    Map<String, Object> filter(Map<String, Object> map);
}

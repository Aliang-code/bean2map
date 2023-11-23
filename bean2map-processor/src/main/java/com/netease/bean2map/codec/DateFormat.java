package com.netease.bean2map.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface DateFormat {
    String DATE = "yyyy-MM-dd";
    String DATETIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式
     *
     * @return
     */
    String pattern() default DATETIME;

    /**
     * 格式化为毫秒时间戳
     *
     * @return
     */
    boolean timestamp() default false;
}

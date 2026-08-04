package com.nextalex.seckill.common.aspect;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ApiOperationLog {

    /**
     * 注解类型描述
     * @return
     */
    String description() default "";
}

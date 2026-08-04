package com.nextalex.seckill.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author: nextalex
 * @Date: 2026/7/31 10:34
 * @Version: v1.0.0
 * @Description: 秒杀系统启动类
 **/
@SpringBootApplication
@ComponentScan({"com.nextalex.seckill.*"}) // 多模块项目中，必需手动指定扫描 com.quanxiaoha.seckill 包下面的所有类
@MapperScan("com.nextalex.seckill.common.domain.mapper")
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
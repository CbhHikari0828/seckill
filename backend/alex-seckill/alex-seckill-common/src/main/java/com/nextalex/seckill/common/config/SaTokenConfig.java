package com.nextalex.seckill.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册SaToken拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 秒杀下单接口，需要登录
            SaRouter.match("/seckill/order", r -> StpUtil.checkLogin());
            SaRouter.match("/seckill/logout", r -> StpUtil.checkLogin());
            SaRouter.match("/admin/**",r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");

    }
}

package com.nextalex.seckill.common.aspect;

import com.nextalex.seckill.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

import java.util.function.Function;
import java.util.stream.Collectors;





@Aspect
@Component
@Slf4j
public class ApiOperationLogAspect {

    // 自定义切点
    @Pointcut("@annotation(com.nextalex.seckill.common.aspect.ApiOperationLog)")
    public void apiOperationLog() {}

    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 请求入参
        Object[] args = joinPoint.getArgs();
        // 入参转Json
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(", "));
        // 获取功能性叙述
        String description = getApiOperationLogDescription(joinPoint);
        // 打印请求相关参数
        log.info("====== 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} =================================== ",
                description, argsJsonStr, className, methodName);
        // 执行切点方法
        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;

        // 打印出参等相关信息
        log.info("====== 请求结束: [{}], 耗时: {}ms, 出参: {} =================================== ",
                description, executionTime, JsonUtils.toJsonString(result));

        return result;

    }


    public String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);
        return apiOperationLog.description();
    }


    private Function<Object, String> toJsonStr() {
        return JsonUtils::toJsonString;
    }


}

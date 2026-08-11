package com.nextalex.seckill.order.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderLockUtils {

    /**
     * ConcurrentHashMap 保证并发安全
     * 锁容器：Key 为 "userId:activityId:goodsId"，Value 为 true
     */
    private final ConcurrentHashMap<String, Boolean> lockMap = new ConcurrentHashMap<>();

    /**
     * 尝试获取锁
     * 利用 ConcurrentHashMap#putIfAbsent 的原子性：
     *      * - 如果 Key 不存在，插入成功，返回 null → 表示获取锁成功
     *      * - 如果 Key 已存在，插入失败，返回旧值 → 表示锁已被占用
     * @param lockKey
     * @return true 获取锁成功；false 锁已被占用
     */
    public boolean tryLock(String lockKey) {
        return lockMap.putIfAbsent(lockKey, Boolean.TRUE) == null;
    }

    /**
     * 释放锁
     * @param lockKey
     */
    public void unlock(String lockKey) {
        lockMap.remove(lockKey);
    }
}

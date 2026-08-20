package com.nextalex.seckill.common.constant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class RedisKeyConstants {

    /**
     * 商品列表缓存 Key 前缀
     *
     * 完整格式：seckill:goods:list:{activityId}
     */
    public static final String GOODS_LIST_PREFIX = "seckill:goods:list:";


    /**
     * 商品详情缓存 Key 前缀
     *
     * 完整格式：seckill:goods:detail:{activityId}:{goodsId}
     */
    public static final String GOODS_DETAIL_PREFIX = "seckill:goods:detail:";

    /**
     * 缓存空值，用于防止缓存穿透
     */
    public static final String NULL_CACHE_VALUE = "NULL";

    /**
     * 活动布隆过滤器 Key
     */
    public static final String SECKILL_ACTIVITY_BLOOM_KEY = "seckill:bloom:activity";

    /**
     * 商品布隆过滤器 Key
     */
    public static final String SECKILL_GOODS_BLOOM_KEY = "seckill:bloom:goods";

    /**
     * 缓存空值的过期时间（单位：分钟）
     */
    public static final long NULL_CACHE_TTL_MINUTES = 5;

    /**
     * 秒杀订单处理状态 Key 前缀
     *
     */
    public static final String SECKILL_ORDER_STATUS_PREFIX = "seckill:order:status:";

    /**
     * 秒杀订单处理状态过期时间（单位：分钟）
     */
    public static final long SECKILL_ORDER_STATUS_TTL_MINUTES = 30;

    /**
     * 秒杀库存 Key 前缀
     */
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";


    /**
     * 安全缓冲时间（单位：秒）
     */
    public static final long SAFETY_BUFFER_SECONDS = 30 * 60; // 30 分钟

    /**
     * 活动结束后，缓存保留的短过期时间（单位：分钟）
     * 防止活动结束后仍有余温流量，每次都打到 DB
     */
    public static final long ENDED_ACTIVITY_TTL_MINUTES = 5;

    /**
     * 秒杀用户购买标记 Key 前缀
     */
    public static final String SECKILL_USER_ORDER_PREFIX = "seckill:user:order:";

    /**
     * 秒杀活动商品元数据 Key 前缀
     */
    public static final String SECKILL_ACTIVITY_GOODS_META_PREFIX = "seckill:activity:goods:meta:";

    /**
     * 构建秒杀活动商品元数据 Key
     */
    public static String buildSeckillActivityGoodsMetaKey(Long activityId, Long goodsId) {
        return SECKILL_ACTIVITY_GOODS_META_PREFIX + activityId + ":" + goodsId;
    }


    /**
     * 构建秒杀用户购买标记 Key
     */
    public static String buildSeckillUserOrderKey(Long activityId, Long goodsId, Long userId) {
        return SECKILL_USER_ORDER_PREFIX + activityId + ":" + goodsId + ":" + userId;
    }


    /**
     * 构建秒杀库存 Key
     */
    public static String buildSeckillStockKey(Long activityId, Long goodsId) {
        return SECKILL_STOCK_PREFIX + activityId + ":" + goodsId;
    }



    public static Long calculateTtlSeconds(LocalDateTime endTime) {
        if (Objects.isNull(endTime)) return null;
        Long ttlSeconds = Duration.between(LocalDateTime.now(), endTime).getSeconds() + SAFETY_BUFFER_SECONDS;

        return ttlSeconds > 0 ? ttlSeconds : null;
    }

    /**
     * 秒杀订单待回补上下文 Key 前缀
     */
    public static final String SECKILL_ORDER_COMPENSATION_PREFIX = "seckill:order:compensation:";

    /**
     * 构建秒杀订单待回补上下文 Key
     */
    public static String buildSeckillOrderCompensationKey(String orderNo) {
        return SECKILL_ORDER_COMPENSATION_PREFIX + orderNo;
    }

}

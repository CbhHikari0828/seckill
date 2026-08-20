package com.nextalex.seckill.order.service;

public interface SeckillOrderPreDeductCompensationService {

    /**
     * MQ 发布明确失败时，回补 Redis 预扣库存和用户购买标记
     */
    void compensateWhenPublishFailed(String orderNo, String reason);
}

package com.nextalex.seckill.order.service;

import com.nextalex.seckill.order.enums.SeckillStockCompensationResultEnum;
import com.nextalex.seckill.order.enums.SeckillStockDeductResultEnum;
import com.nextalex.seckill.order.model.dto.SeckillOrderMqDTO;

public interface SeckillStockService {

    /**
     * Lua 原子扣减库存
     */
    SeckillStockDeductResultEnum preDeductStock(SeckillOrderMqDTO seckillOrderMqDTO, Long userOrderTtlSeconds,
                                                Long activityBeginTimeMillis, Long activityEndTimeMillis);

    /**
     * Redis Lua 原子回补秒杀库存，并删除用户购买标记
     */
    SeckillStockCompensationResultEnum compensatePreDeductStock(Long activityId,
                                                                Long goodsId,
                                                                Long userId,
                                                                String orderNo);
}

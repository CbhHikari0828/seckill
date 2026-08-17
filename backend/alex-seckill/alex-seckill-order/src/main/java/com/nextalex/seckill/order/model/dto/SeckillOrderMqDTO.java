package com.nextalex.seckill.order.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.nio.LongBuffer;
import java.time.LocalDateTime;
import java.util.Locale;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SeckillOrderMqDTO {

    /** 下单用户 ID */
    private Long userId;

    /** 秒杀活动 ID*/
    private Long activityId;

    /** 秒杀商品 ID */
    private Long goodsId;

    /** 秒杀商品主键 ID */
    private Long seckillGoodsId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 订单号 */
    private String orderNo;

    /** 用户发起请求的时间（用于追踪消息延迟） */
    private LocalDateTime requestTime;

}

package com.nextalex.seckill.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DoSeckillRspVO {

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImg;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 订单状态：0=待支付
     */
    private Integer status;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;
}

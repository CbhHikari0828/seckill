package com.nextalex.seckill.goods.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindSeckillGoodsListRspVO {

    private long id;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品原价
     */
    private BigDecimal goodsPrice;

    /**
     * 活动 ID
     */
    private Long activityId;

    /**
     * 秒杀商品名称
     */
    private String seckillTitle;

    /**
     * 秒杀商品图片
     */
    private String seckillImg;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存总量
     */
    private BigDecimal seckillTotal;

    /**
     * 剩余库存
     */
    private Integer seckillStock;

    /**
     * 活动状态：0=未开始，1=进行中，2=已结束
     */
    private Integer ActivityStatus;

    /**
     * 开始时间
     */
    private LocalDateTime beginTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}

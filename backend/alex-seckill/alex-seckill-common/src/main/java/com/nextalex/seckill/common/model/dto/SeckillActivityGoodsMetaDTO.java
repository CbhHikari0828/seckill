package com.nextalex.seckill.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动商品元数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeckillActivityGoodsMetaDTO {

    private Long seckillGoodsId;

    private Long activityId;

    private Long goodsId;

    private BigDecimal seckillPrice;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;


}

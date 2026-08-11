package com.nextalex.seckill.order.model.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoSeckillReqVO {

    @NotNull(message = "活动id不能为空")
    @Positive(message = "活动id不合法")
    private Long activityId;

    @NotNull(message = "商品ID不能为空")
    @Positive(message = "商品ID不合法")
    private Long goodsId;
}

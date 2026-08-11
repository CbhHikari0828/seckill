package com.nextalex.seckill.goods.model.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindSeckillGoodsDetailReqVO {

    @NotNull(message = "商品ID不能为空")
    @Positive(message = "商品ID不合法")
    private Long goodsId;

    @NotNull(message = "活动ID不能为空")
    @Positive(message = "活动ID不合法")
    private Long activityId;
}

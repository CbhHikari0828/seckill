package com.nextalex.seckill.goods.model.vo;

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
public class PreheatActivityCacheReqVO {

    @NotNull(message = "活动 ID 不能为空")
    @Positive(message = "活动 ID 不合法")
    private Long activityId;
}

package com.nextalex.seckill.goods.model.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindSeckillGoodsListReqVO {

    @NotNull(message = "活动 ID 不能为空")
    @Positive(message = "活动ID不合法")
    private long activityId;
}

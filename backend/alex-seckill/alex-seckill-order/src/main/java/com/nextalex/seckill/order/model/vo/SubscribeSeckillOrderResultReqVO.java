package com.nextalex.seckill.order.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscribeSeckillOrderResultReqVO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;
}

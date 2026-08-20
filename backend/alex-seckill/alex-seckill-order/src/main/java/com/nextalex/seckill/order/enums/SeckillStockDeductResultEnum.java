package com.nextalex.seckill.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum SeckillStockDeductResultEnum {

    SUCCESS(1L, "库存扣减成功"),
    SOLD_OUT(0L, "商品已售罄"),
    REPEATED_ORDER(2L, "请勿重复参与秒杀"),
    ACTIVITY_ENDED(4L, "秒杀活动已结束"),
    ACTIVITY_NOT_STARTED(3L, "秒杀活动未开始"),
    STOCK_NOT_PREHEATED(-1L, "秒杀库存未预热");

    /**
     *  lua脚本返回码
     */
    private final Long code;

    /**
     * 返回说明
     */
    private final String description;

    /**
     * 根据Lua脚本获取返回结果
     * @param code
     * @return
     */
    public static SeckillStockDeductResultEnum getByCode(Long code) {
        for (SeckillStockDeductResultEnum resultEnum : values()){
            if (Objects.equals(resultEnum.getCode(), code)) return resultEnum;
        }
        return null;
    }

}

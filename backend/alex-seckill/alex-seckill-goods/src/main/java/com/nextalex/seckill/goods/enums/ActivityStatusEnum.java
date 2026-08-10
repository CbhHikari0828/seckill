package com.nextalex.seckill.goods.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityStatusEnum {

    NOT_START(0,"未开始"),
    ING(1,"进行中"),
    ENDED(2, "已结束");


    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;
}

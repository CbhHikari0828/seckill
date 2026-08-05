package com.nextalex.seckill.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLE(0, "禁用"),
    ENABLE(1,"启用"),

    ;

    private final Integer code;

    private final String description;

}

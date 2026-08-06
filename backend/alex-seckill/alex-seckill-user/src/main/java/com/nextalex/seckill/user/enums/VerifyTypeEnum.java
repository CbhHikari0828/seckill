package com.nextalex.seckill.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum VerifyTypeEnum {

    REGISTER(1, "register", "注册"),
    LOGIN(2, "login", "登录"),
    ;

    // 类型值
    private final Integer code;
    // 类型标识
    private final String purpose;
    // 类型描述
    private final String description;

    /**
     * 根据 code 获取枚举类
     * @param code
     * @return
     */
    public static VerifyTypeEnum valueOf(Integer code){
        for (VerifyTypeEnum typeEnum : values()) {
            if (Objects.equals(code, typeEnum.getCode())) return typeEnum;
        }
        return null;
    }
}

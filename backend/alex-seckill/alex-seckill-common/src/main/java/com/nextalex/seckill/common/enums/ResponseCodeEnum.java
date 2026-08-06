package com.nextalex.seckill.common.enums;

import com.nextalex.seckill.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("10001", "参数错误"),



    // ----------- 业务异常状态码 -----------
    // ----------- 秒杀业务异常状态码 -----------
    SECKILL_SOLD_OUT("20001", "商品已售罄"),
    SECKILL_DUPLICATE("20002", "您已参与过此秒杀活动"),
    USER_MOBILE_EXISTS("20001", "该手机号已注册"),
    USER_VERIFY_CODE_ERROR("20002", "验证码错误"),
    USER_MOBILE_NOT_REGISTERED("20003", "该手机号未注册"),
    USER_PASSWORD_ERROR("20004", "密码错误"),
    USER_STATUS_DISABLED("20005", "账号已被禁用，请联系管理员"),
    VERIFY_CODE_TYPE_ERROR("20006", "验证码类型错误"),
    VERIFY_CODE_SEND_TOO_FREQUENT("20007", "验证码发送过于频繁，请稍后再试"),
    VERIFY_CODE_DAILY_LIMIT_EXCEEDED("20008", "验证码每日发送次数已达上限，请明天再试"),
    LOGIN_FAIL_TOO_MANY("20009", "密码错误次数过多，请 30 分钟后再试"),


    ;

    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;
}

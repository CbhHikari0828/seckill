package com.nextalex.seckill.common.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BizException extends RuntimeException {

    private String errorMessage;

    private String errorCode;


    public BizException(BaseExceptionInterface baseExceptionInterface) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMessage = baseExceptionInterface.getErrorMessage();
    }
}

package com.nextalex.seckill.user.service;

import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.user.model.vo.RegisterUserReqVO;

public interface UserService {

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    Response<?> register(RegisterUserReqVO registerUserReqVO);
}

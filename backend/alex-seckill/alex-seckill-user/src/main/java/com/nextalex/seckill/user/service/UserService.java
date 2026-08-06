package com.nextalex.seckill.user.service;

import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.user.model.vo.LoginUserReqVO;
import com.nextalex.seckill.user.model.vo.LoginUserRspVO;
import com.nextalex.seckill.user.model.vo.RegisterUserReqVO;
import com.nextalex.seckill.user.model.vo.SendVerifyCodeReqVO;

public interface UserService {

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    Response<?> register(RegisterUserReqVO registerUserReqVO);

    Response<LoginUserRspVO> login(LoginUserReqVO loginUserReqVO);

    Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO);
}

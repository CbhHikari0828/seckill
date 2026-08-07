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

    /**
     * 用户登录
     * @param loginUserReqVO
     * @return
     */
    Response<LoginUserRspVO> login(LoginUserReqVO loginUserReqVO);

    /**
     * 发送验证码
     * @param sendVerifyCodeReqVO
     * @return
     */
    Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout();
}

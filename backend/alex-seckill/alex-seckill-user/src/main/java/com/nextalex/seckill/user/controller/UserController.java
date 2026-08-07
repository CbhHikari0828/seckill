package com.nextalex.seckill.user.controller;

import com.nextalex.seckill.common.aspect.ApiOperationLog;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.user.model.vo.LoginUserReqVO;
import com.nextalex.seckill.user.model.vo.LoginUserRspVO;
import com.nextalex.seckill.user.model.vo.RegisterUserReqVO;
import com.nextalex.seckill.user.model.vo.SendVerifyCodeReqVO;
import com.nextalex.seckill.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<?> register(@Validated @RequestBody RegisterUserReqVO registerUserReqVO){
        return userService.register(registerUserReqVO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录")
    public Response<LoginUserRspVO> login(@Validated @RequestBody LoginUserReqVO loginUserReqVO) {
        return userService.login(loginUserReqVO);
    }

    /**
     * 发送验证码
     * @param sendVerifyCodeReqVO
     * @return
     */
    @PostMapping("/code/send")
    @ApiOperationLog(description = "发送验证码")
    public Response<?> sendVerifyCode(@Validated @RequestBody SendVerifyCodeReqVO sendVerifyCodeReqVO) {
        return userService.sendVerifyCode(sendVerifyCodeReqVO);
    }

    /**
     * 退出登录
     * @return
     */
    @PostMapping("/logout")
    @ApiOperationLog(description = "退出登录")
    public Response<?> logout() {
        return userService.logout();
    }
}

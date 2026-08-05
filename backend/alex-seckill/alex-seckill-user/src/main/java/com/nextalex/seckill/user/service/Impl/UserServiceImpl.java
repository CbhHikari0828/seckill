package com.nextalex.seckill.user.service.Impl;

import cn.hutool.core.util.RandomUtil;
import com.nextalex.seckill.common.domain.dataobject.UserDO;
import com.nextalex.seckill.common.domain.mapper.UserDOMapper;
import com.nextalex.seckill.common.enums.ResponseCodeEnum;
import com.nextalex.seckill.common.exception.BizException;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.user.enums.UserStatusEnum;
import com.nextalex.seckill.user.model.vo.RegisterUserReqVO;
import com.nextalex.seckill.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    /**
     * 用户注册
     * @param registerUserReqVO
     * @return
     */
    @Override
    public Response<?> register(RegisterUserReqVO registerUserReqVO) {
        String mobile = registerUserReqVO.getMobile();
        String password = registerUserReqVO.getPassword();
        String verifyCode = registerUserReqVO.getVerifyCode();
        // 1.校验验证码
        // todo 先写死验证码123456，后续短信发送验证码
        if (!verifyCode.equals("123456")) throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        // 校检手机号是否注册
        Long existUserID = userDOMapper.selectIdByMobile(mobile);
        // 校检手机号是否注册
        if (Objects.nonNull(existUserID)) throw new BizException(ResponseCodeEnum.USER_MOBILE_EXISTS);
        // 密码加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encoderPassword = passwordEncoder.encode(password);
        // 构建用户实体，插入数据库
        UserDO userDO = UserDO.builder()
                .nickname(generateNickname())
                .status(UserStatusEnum.ENABLE.getCode())
                .mobile(mobile)
                .password(encoderPassword)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        userDOMapper.insert(userDO);
        log.info("==> 用户注册成功, mobile: {}", mobile);

        return Response.success();
    }

    private String generateNickname() {
        return "用户" + RandomUtil.randomNumbers(6);
    }
}

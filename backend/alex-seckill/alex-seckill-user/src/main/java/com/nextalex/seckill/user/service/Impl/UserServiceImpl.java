package com.nextalex.seckill.user.service.Impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.nextalex.seckill.common.domain.dataobject.UserDO;
import com.nextalex.seckill.common.domain.mapper.UserDOMapper;
import com.nextalex.seckill.common.enums.ResponseCodeEnum;
import com.nextalex.seckill.common.exception.BizException;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.user.enums.LoginTypeEnum;
import com.nextalex.seckill.user.enums.UserStatusEnum;
import com.nextalex.seckill.user.model.vo.LoginUserReqVO;
import com.nextalex.seckill.user.model.vo.LoginUserRspVO;
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

    /**
     * 用户登录
     * @param loginUserReqVO
     * @return
     */
    @Override
    public Response<LoginUserRspVO> login(LoginUserReqVO loginUserReqVO) {
        String mobile = loginUserReqVO.getMobile();
        Integer type = loginUserReqVO.getType();
        // 1.根据手机号查询用户
        UserDO userDO = userDOMapper.selectByMobile(mobile);
        // 2.判断用户是否存在
        if(Objects.isNull(userDO)) throw new BizException(ResponseCodeEnum.USER_MOBILE_NOT_REGISTERED);
        // 3.判断验证信息
        if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) checkPassword(loginUserReqVO.getPassword(), userDO.getPassword());
        else checkVerifyCode(loginUserReqVO.getVerifyCode());
        // 4.校检账号是否正确
        if (userDO.getStatus().equals(UserStatusEnum.DISABLE.getCode())) throw new BizException(ResponseCodeEnum.USER_STATUS_DISABLED);
        // 5.调用Sa-token执行登录传入用户ID
        StpUtil.login(userDO.getId());
        // 6.获取Token
        String token = StpUtil.getTokenValue();
        // 7.构建反参对象
        LoginUserRspVO loginUserRspVO = LoginUserRspVO.builder()
                .token(token)
                .userInfo(LoginUserRspVO.UserInfo.builder()
                        .id(userDO.getId())
                        .nickname(userDO.getNickname())
                        .avatar(userDO.getAvatar())
                        .build())
                .build();
        log.info("==> 用户登录成功, userId: {}, mobile: {}", userDO.getId(), mobile);

        return Response.success(loginUserRspVO);
    }

    /**
     * 校检密码
     * @param rawPassword
     * @param encodePassword
     */
    public void checkPassword(String rawPassword, String encodePassword) {
        if (StrUtil.isBlank(rawPassword)) throw new BizException(ResponseCodeEnum.USER_PASSWORD_ERROR);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // 校检密码是否正确
        boolean matches = passwordEncoder.matches(rawPassword, encodePassword);
        if (!matches) throw new BizException(ResponseCodeEnum.USER_PASSWORD_ERROR);
    }

    /**
     * 校验验证码
     * @param verifyCode
     */
    public void checkVerifyCode(String verifyCode) {
        if (StrUtil.isBlank(verifyCode)) throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        // todo 后续传入随机验证码
        if (!verifyCode.equals("123456")) throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
    }

    private String generateNickname() {
        return "用户" + RandomUtil.randomNumbers(6);
    }
}

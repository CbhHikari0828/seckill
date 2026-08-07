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
import com.nextalex.seckill.user.enums.VerifyTypeEnum;
import com.nextalex.seckill.user.model.vo.LoginUserReqVO;
import com.nextalex.seckill.user.model.vo.LoginUserRspVO;
import com.nextalex.seckill.user.model.vo.RegisterUserReqVO;
import com.nextalex.seckill.user.model.vo.SendVerifyCodeReqVO;
import com.nextalex.seckill.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;

//    @Resource
//    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "bizExecutor")
    private Executor bizexecutor;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Bcrypt 密码编辑器
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    // Redis 中验证码的 Key 前缀
    private static final String VERIFY_CODE_KEY_PREFIX = "verify_code:";
    // Redis 中发送频率限制的 Key 前缀
    private static final String VERIFY_CODE_LIMIT_KEY_PREFIX = "verify_code_limit:";
    // 验证码过期时间（分钟）
    private static final Long VERIFY_CODE_EXPIRE_MINUTES = 5L;
    // 发送频率限制时间（秒）
    private static final Long VERIFY_CODE_LIMIT_SECONDS = 60L;
    // Redis 中每日发送次数限制的 Key 前缀
    private static final String VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX = "verify_code_daily:";
    // 每日发送次数上限
    private static final Integer VERIFY_CODE_DAILY_LIMIT = 10;
    // Redis 中登录失败次数的 Key 前缀
    private static final String LOGIN_FAIL_COUNT_KEY_PREFIX = "login_fail_count:";
    // 登录失败次数上限（超过此值则临时锁定账号）
    private static final Integer LOGIN_FAIL_MAX_COUNT = 5;
    // 账号临时锁定时间（分钟）
    private static final Long LOGIN_LOCK_MINUTES = 30L;
    // 验证码校验Lua脚本
    private final DefaultRedisScript<Long> checkAndDeleteVerifyCodeScript;
    // 登陆失败计数Lua脚本
    private final DefaultRedisScript<Long> checkAndIncrementLoginFailScript;




    /**
     * 构造函数，初始化Lua加载器
     */
    public UserServiceImpl() {
        // 1.验证码校验Lua脚本
        checkAndDeleteVerifyCodeScript = new DefaultRedisScript<>();
        // 加载Lua脚本文件
        checkAndDeleteVerifyCodeScript.setLocation(new ClassPathResource("lua/check_and_delete_verify_code.lua"));
        // 指定返回值类型
        checkAndDeleteVerifyCodeScript.setResultType(Long.class);

        // 2.登录失败计数Lua脚本
        checkAndIncrementLoginFailScript = new DefaultRedisScript<>();
        checkAndIncrementLoginFailScript.setLocation(new ClassPathResource("lua.check_and_increment_login_fail_count"));
        checkAndIncrementLoginFailScript.setResultType(Long.class);
    }

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
        checkVerifyCode(verifyCode, mobile, VerifyTypeEnum.REGISTER.getPurpose());
        // 校检手机号是否注册
        Long existUserID = userDOMapper.selectIdByMobile(mobile);
        // 校检手机号是否注册
        if (Objects.nonNull(existUserID)) throw new BizException(ResponseCodeEnum.USER_MOBILE_EXISTS);
        // 密码加密
        String encoderPassword = PASSWORD_ENCODER.encode(password);
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
        if(Objects.isNull(userDO)) {
            if (type.equals(LoginTypeEnum.PASSWORD.getCode())) throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
            else throw new BizException(ResponseCodeEnum.USER_MOBILE_NOT_REGISTERED);
        }
        // 3.检查用户状态
        if (userDO.getStatus().equals(UserStatusEnum.DISABLE.getCode())) throw new BizException(ResponseCodeEnum.USER_STATUS_DISABLED);
        // 4.判断验证信息
        if (Objects.equals(type, LoginTypeEnum.PASSWORD.getCode())) {
            // 检查登陆失败次数
            checkLoginFailLimit(mobile);
            // 检查密码准确性
            checkPassword(loginUserReqVO.getPassword(), userDO.getPassword(), mobile);

        }
        else checkVerifyCode(loginUserReqVO.getVerifyCode(), mobile, VerifyTypeEnum.LOGIN.getPurpose());

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
     * 发送验证码服务
     * @param sendVerifyCodeReqVO
     * @return
     */
    @Override
    public Response<?> sendVerifyCode(SendVerifyCodeReqVO sendVerifyCodeReqVO) {
        String mobile = sendVerifyCodeReqVO.getMobile();
        Integer type = sendVerifyCodeReqVO.getType();
        // 校验验证码是否正确
        VerifyTypeEnum verifyTypeEnum = VerifyTypeEnum.valueOf(type);
        if (Objects.isNull(verifyTypeEnum)) throw new BizException(ResponseCodeEnum.VERIFY_CODE_TYPE_ERROR);
        // 发送频率限制
        String limitKey = VERIFY_CODE_LIMIT_KEY_PREFIX + verifyTypeEnum.getPurpose() + ":" + mobile;
        if (stringRedisTemplate.hasKey(limitKey)) throw new BizException(ResponseCodeEnum.VERIFY_CODE_SEND_TOO_FREQUENT);
        // 每日发送限制
        String dailyLimitKey = VERIFY_CODE_DAILY_LIMIT_KEY_PREFIX + verifyTypeEnum.getPurpose() + ":" + mobile + ":" + LocalDate.now();
        // 限制数+1
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyLimitKey);
        // 首次设置缓存
        if (Objects.nonNull(dailyCount) && dailyCount == 1) {
            // 计算当前时间距离第二天凌晨还剩多少秒
            Long secondsUtilMidnight = Duration.between(
                    LocalDateTime.now(),
                    LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
            ).getSeconds();
            // 设置过期时间
            stringRedisTemplate.expire(dailyLimitKey, secondsUtilMidnight, TimeUnit.SECONDS);
        }
        // 每日限额达到，抛出异常
        if (Objects.nonNull(dailyCount) && dailyCount > VERIFY_CODE_DAILY_LIMIT) throw new BizException(ResponseCodeEnum.VERIFY_CODE_DAILY_LIMIT_EXCEEDED);
        // 随机验证码六位
        String verifyCode = RandomUtil.randomNumbers(6);
        // 通过piePle通道，批量写入 Redis
        String redisKey = VERIFY_CODE_KEY_PREFIX + verifyTypeEnum.getPurpose() + ":" + mobile;
        stringRedisTemplate.executePipelined(new SessionCallback<Void>() {

            @Override
            public Void execute(RedisOperations operations) {
                // 先写限制频率
                operations.opsForValue().set(limitKey, "1", VERIFY_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);
                // 再写验证码
                operations.opsForValue().set(redisKey, verifyCode, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                return null;
            }
        });
        // 异步发送验证码
        bizexecutor.execute(() -> sendSms(mobile, verifyCode));

        return Response.success();
    }

    /**
     * 退出登录
     * @return
     */
    @Override
    public Response<?> logout() {
        String tokenValue = StpUtil.getTokenValue();
        String userId = StpUtil.getLoginId(tokenValue);
        StpUtil.logout();
        log.info("==> 用户退出登录, userId: {}, token: {}", userId, tokenValue);
        return Response.success();
    }

    /**
     * 校检密码
     * @param rawPassword
     * @param encodePassword
     */
    public void checkPassword(String rawPassword, String encodePassword, String mobile) {
        if (StrUtil.isBlank(rawPassword)) {
            // 失败次数加一
            addLoginFailCount(mobile);
            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
        }
        // 校检密码是否正确
        boolean matches = PASSWORD_ENCODER.matches(rawPassword, encodePassword);
        if (!matches) {
            // 失败次数加一
            addLoginFailCount(mobile);
            throw new BizException(ResponseCodeEnum.USER_LOGIN_CREDENTIAL_ERROR);
        }
        // 成功则清空失败此次数
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;
        stringRedisTemplate.delete(failCountKey);

    }

    /**
     * 校验验证码
     * @param verifyCode
     */
    public void checkVerifyCode(String verifyCode, String mobile, String purpose) {
        if (StrUtil.isBlank(verifyCode)) throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
        // 从Redis中获取验证码
        // 构造key
        String redisKey = VERIFY_CODE_KEY_PREFIX + purpose + ":" + mobile;
        Long result = stringRedisTemplate.execute(checkAndDeleteVerifyCodeScript, Collections.singletonList(redisKey),verifyCode);
        // 验证码过期或者无效
        if (result == null || result == 0) throw new BizException(ResponseCodeEnum.USER_VERIFY_CODE_ERROR);
    }

    /**
     * 发送验证码
     * @param mobile
     * @param verifyCode
     */
    private void sendSms(String mobile, String verifyCode) {
        try {
            // todo 调用Api发送短信验证码
            log.info("==> 验证码发送成功, mobile: {}, verifyCode: {}", mobile, verifyCode);
        }catch (Exception e) {
            log.error("==> 验证码发送失败, mobile: {}, verifyCode: {}", mobile, verifyCode, e);
        }
    }


    /**
     * 获取随机用户名
     * @return
     */
    private String generateNickname() {
        return "用户" + RandomUtil.randomNumbers(6);
    }

    /**
     * 验证登录请求是否超限
     * @param mobile
     */
    private void checkLoginFailLimit(String mobile) {
        // 构建Redis Key
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;

        // 查询缓存计数
        String failCountStr =  stringRedisTemplate.opsForValue().get(failCountKey);

        // 查询是否超限
        if (Objects.nonNull(failCountStr)) {
            int failCount = Integer.parseInt(failCountStr);
            if (failCount >= LOGIN_FAIL_MAX_COUNT) throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);
        }
    }

    /**
     * 累加登录失败次数
     * @param mobile
     */
    private void addLoginFailCount(String mobile) {
        // 构建Redis Key
        String failCountKey = LOGIN_FAIL_COUNT_KEY_PREFIX + mobile;
        // 执行Lua脚本，原子性检查失败并累加
        Long result = stringRedisTemplate.execute(checkAndIncrementLoginFailScript,
                Collections.singletonList(failCountKey),String.valueOf(LOGIN_FAIL_MAX_COUNT),
                String.valueOf(LOGIN_LOCK_MINUTES*60)
        );
        if (Objects.nonNull(result) && result==-1) throw new BizException(ResponseCodeEnum.LOGIN_FAIL_TOO_MANY);

    }

}

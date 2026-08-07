package com.nextalex.seckill.user.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.generator.common.model.dto.GenerateParam;
import cloud.tianai.captcha.spring.autoconfiguration.ImageCaptchaAutoConfiguration;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.nextalex.seckill.common.aspect.ApiOperationLog;
import io.micrometer.common.util.StringUtils;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.Collections;

@RestController
@RequestMapping("/captcha")
@Slf4j
public class CaptchaController {

    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;


    /**
     * 生成行为校验码接口
     * @param type
     * @return
     */
    @PostMapping("/gen")
    @ApiOperationLog(description = "生成行为验证码")
    public ApiResponse<ImageCaptchaVO> genCaptcha(@RequestParam(value = "type", required = false) String type) {
        if (StringUtils.isBlank(type)) type = CaptchaTypeConstant.SLIDER;
        if ("RANDOM".equals(type)){
            int i = ThreadLocalRandom.current().nextInt(0,4); // 设置随机验证方式
            switch (i) {
                case 0 -> type = CaptchaTypeConstant.SLIDER;
                case 1 -> type = CaptchaTypeConstant.CONCAT;
                case 2 -> type = CaptchaTypeConstant.ROTATE;
                default -> type = CaptchaTypeConstant.WORD_IMAGE_CLICK;
            }
        }
        GenerateParam generateParam = new GenerateParam();
        // 设置验证码类型
        generateParam.setType(type);
        // 生成验证码，返回验证码数据
        ApiResponse<ImageCaptchaVO> response = imageCaptchaApplication.generateCaptcha(generateParam);
        return response;
    }

    @PostMapping("/check")
    @ApiOperationLog(description = "校验行为验证码")
    public ApiResponse<?> checkCaptcha(@RequestBody CaptchaData captchaData) {
        // 验证码校验
        ApiResponse<?> response = imageCaptchaApplication.matching(captchaData.getId(), captchaData.getData());

        if (response.isSuccess()) return ApiResponse.ofSuccess(Collections.singletonMap("id", captchaData.getId()));
        return response;
    }

    @Data
    public static class CaptchaData {
        private String id;
        private ImageCaptchaTrack data;
    }

}

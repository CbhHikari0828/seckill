package com.nextalex.seckill.user.config;

import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaptchaResourceConfig {

    @Bean
    public ResourceStore resourceStore() {
        // 利用简单本地内存存储数据
        LocalMemoryResourceStore resourceStore = new LocalMemoryResourceStore();
        // 添加自定义图片
        resourceStore.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath","bgimages/e.png","default"));
        resourceStore.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath","bgimages/j.png", "default"));
        resourceStore.addResource(CaptchaTypeConstant.WORD_IMAGE_CLICK, new Resource("classpath","bgimages/img.png", "default"));

        return resourceStore;
    }
}

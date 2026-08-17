package com.nextalex.seckill.order.Controller;

import com.nextalex.seckill.common.config.RabbitMQConfig;
import com.nextalex.seckill.common.utils.Response;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/test/mq")
@Slf4j
public class TestMqController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/send")
    public Response<String> sendMessage() {
        String message = "Hello RabbitMQ! 发送时间: " + LocalDateTime.now();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TEST_EXCHANGE,
                RabbitMQConfig.TEST_ROUTING_KEY,
                message
        );
        log.info("==> 测试消息发送成功: {}", message);
        return Response.success("消息发送成功");
    }
}

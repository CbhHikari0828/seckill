package com.nextalex.seckill.order.consumer;

import com.nextalex.seckill.common.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestMqConsumer {

    @RabbitListener(queues = RabbitMQConfig.TEST_QUEUE)
    public void consume(String message) {
        log.info("## 收到测试消息: {}", message);
    }
}

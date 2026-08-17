package com.nextalex.seckill.order.consumer;

import com.nextalex.seckill.common.config.RabbitMQConfig;
import com.nextalex.seckill.order.model.dto.SeckillOrderMqDTO;
import com.nextalex.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeckillOrderConsumer {

    @Resource
    private OrderService orderService;

    /**
     * 监听秒杀订单队列，收到消息后，调用业务层扣减库存、创建订单
     *
     * @param message
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE, concurrency = "5-10")
    public void consumer(SeckillOrderMqDTO message){
        log.info("## 收到秒杀订单消息: {}", message);
        orderService.createSeckillOrder(message);
    }

}

package com.nextalex.seckill.order.consumer;

import com.nextalex.seckill.common.config.RabbitMQConfig;
import com.nextalex.seckill.order.model.dto.SeckillOrderMqDTO;
import com.nextalex.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import com.rabbitmq.client.Channel;


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
    public void consumer(SeckillOrderMqDTO seckillOrderMqDTO , Channel channel, Message message) throws IOException {
        // deliveryTag 是 RabbitMQ 给当前投递消息分配的唯一编号，手动 ACK/NACK 时必须带上它
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("## 收到秒杀订单消息: {}", seckillOrderMqDTO);
        try {
            orderService.createSeckillOrder(seckillOrderMqDTO);
        } catch (Exception e) {
            log.error("## 秒杀订单消息处理出现系统异常，进入死信队列, orderNo: {}, deliveryTag: {}",
                    seckillOrderMqDTO.getOrderNo(), deliveryTag, e);
            // 任务失败进入死信队列
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // 手动 ACK,这样RabbitMQ才会将消息从队列删除
        channel.basicAck(deliveryTag, false);
        log.info("==> 秒杀订单消息已 ACK, orderNo: {}, deliveryTag: {}", seckillOrderMqDTO.getOrderNo(), deliveryTag);

    }

}

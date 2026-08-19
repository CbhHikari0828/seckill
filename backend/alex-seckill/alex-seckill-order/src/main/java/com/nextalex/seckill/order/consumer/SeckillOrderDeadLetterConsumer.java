package com.nextalex.seckill.order.consumer;


import com.nextalex.seckill.common.config.RabbitMQConfig;
import com.nextalex.seckill.order.model.dto.SeckillOrderMqDTO;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
public class SeckillOrderDeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.SECKILL_DLX_QUEUE)
    public void consumer(SeckillOrderMqDTO seckillOrderMqDTO, Channel channel, Message message) throws IOException {
        log.error("## 收到秒杀订单死信消息, orderNo: {}, userId: {}, activityId: {}, goodsId: {}, message: {}",
                seckillOrderMqDTO.getOrderNo(), seckillOrderMqDTO.getUserId(), seckillOrderMqDTO.getActivityId(), seckillOrderMqDTO.getGoodsId(), message);
        // deliveryTag 是 RabbitMQ 给当前投递消息分配的唯一编号，手动 ACK/NACK 时必须带上它
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        // TODO: 生产环境建议在这里接入告警，并将死信消息落库，方便后续人工介入，进行补偿

        // 死信消息处理完成后, 主动 ACK，否则项目重启，这条死信还会被再次投递
        channel.basicAck(deliveryTag, false);
        log.info("## 秒杀订单死信消息已 ACK, orderNo: {}, deliveryTag: {}", seckillOrderMqDTO.getOrderNo(), deliveryTag);
    }
}

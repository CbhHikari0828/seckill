package com.nextalex.seckill.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class RabbitMQConfig {

    /** 测试交换机名称 */
    public static final String TEST_EXCHANGE = "seckill.test.exchange";

    /** 测试队列名称 */
    public static final String TEST_QUEUE = "seckill.test.queue";

    /** 测试路由键 */
    public static final String TEST_ROUTING_KEY = "seckill.test.routing.key";

    /** 秒杀订单交换机 */
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";

    /** 秒杀订单队列 */
    public static final String SECKILL_QUEUE = "seckill.order.queue";

    /** 秒杀下单路由 */
    public static final String SECKILL_ROUTING_KEY = "seckill.order.create";

    // ============================死信配置======================================== //
    /** 秒杀名单死信交换机名称 */
    public static final String SECKILL_DLX_EXCHANGE = "seckill.order.dlx.exchange";

    /** 秒杀订单死信队列名称 */
    public static final String SECKILL_DLX_QUEUE = "seckill.order.dlx.queue";

    /** 秒杀订单死信路由键 */
    public static final String SECKILL_DLX_ROUTING_KEY = "seckill.order.dlx";

    /**
     * 自定义消息转化器，用Jackson序列化（Json格式）
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 测试交换机，仅在dev环境
     * @return
     */
    @Bean
    @Profile("dev")
    public DirectExchange testExchange() {
        // 参数名称，是否持久化， 是否自动删除
        return new DirectExchange(TEST_EXCHANGE, true, false);
    }

    /**
     * 测试队列
     * @return
     */
    @Bean
    @Profile("dev")
    public Queue testQueue() {
        // 参数名称， 是否持久化
        return new Queue(TEST_QUEUE, true);
    }

    @Bean
    @Profile("dev")
    public Binding testBinding(Queue testQueue, DirectExchange testExchange) {
        return BindingBuilder.bind(testQueue)
                .to(testExchange)
                .with(TEST_ROUTING_KEY);
    }

    /**
     * 秒杀订单交换机
     * @return
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    /**
     * 秒杀订单队列（持久化）
     * @return
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SECKILL_DLX_ROUTING_KEY)
                .build();
    }


    /**
     * 订单秒杀队列绑定订单秒杀交换机
     * @param seckillOrderQueue
     * @param seckillOrderExchange
     * @return
     */
    @Bean
    public Binding SeckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue)
                .to(seckillOrderExchange)
                .with(SECKILL_ROUTING_KEY);
    }

    /**
     * 秒杀死信队列构建
     * @return
     */
    @Bean
    public Queue seckillOrderDeadLetterQueue() {
        return QueueBuilder.durable(SECKILL_DLX_QUEUE).build();
    }

    /**
     * 秒杀死刑交换机构建
     * @return
     */
    @Bean
    public DirectExchange seckillOrderDeadLetterExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    /**
     * 将秒杀订单死信队列绑定到死信交换机，指定死信路由键
     */
    @Bean
    public Binding seckillOrderDeadLetterBinding(Queue seckillOrderDeadLetterQueue,
                                                 DirectExchange seckillOrderDeadLetterExchange){
        return BindingBuilder.bind(seckillOrderDeadLetterQueue)
                .to(seckillOrderDeadLetterExchange)
                .with(SECKILL_DLX_ROUTING_KEY);
    }




}

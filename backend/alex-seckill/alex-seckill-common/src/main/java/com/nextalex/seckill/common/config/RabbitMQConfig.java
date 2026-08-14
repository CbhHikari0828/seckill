package com.nextalex.seckill.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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
}

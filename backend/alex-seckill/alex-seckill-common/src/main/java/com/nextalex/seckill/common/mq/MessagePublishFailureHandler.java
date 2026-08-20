package com.nextalex.seckill.common.mq;

public interface MessagePublishFailureHandler {

    /**
     * 发送消息时写入消息头，用于区分业务场景
     */
    String MESSAGE_ID_HEADER = "x-message-id";

    /**
     * Broker 返回 Nack，表示 Broker 未能成功处理这条要发布的消息
     *
     * @param messageId 消息关联 ID
     * @param cause     失败原因
     */
    void handleConfirmNack(String messageId, String cause);

    /**
     * 消息到达交换机，但未路由到队列中
     *
     * @param messageId  消息关联 ID
     * @param exchange   交换机
     * @param routingKey 路由键
     */
    void handleReturned(String messageId, String exchange, String routingKey);
}

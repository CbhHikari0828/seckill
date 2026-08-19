package com.nextalex.seckill.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aspectj.weaver.ast.Or;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PROCESSING(-1, "处理中"),
    PENDING_PAYMENT(0, "待支付"),
    PENDING_SHIPMENT(1, "待发货"),
    SHIPPED(2, "已发货"),
    RECEIVED(3, "已收货"),
    REFUNDED(4, "已退款"),
    CANCELLED(5, "已取消"),
    SECKILL_FAILED(-2, "秒杀失败"),
    CLOSED(6, "已关闭");

    private final Integer status;

    private final String description;


    /**
     * 获取状态描述
     * @param status
     * @return
     */
    public static String getDescriptionByStatus(Integer status) {
        OrderStatusEnum orderStatusEnum = getByStatus(status);
        return orderStatusEnum == null ? "未知状态" : orderStatusEnum.getDescription();
    }


    /**
     * 根据状态码获取状态
     * @param status
     * @return
     */
    public static OrderStatusEnum getByStatus(Integer status) {
        for (OrderStatusEnum orderStatusEnum : values()) {
            if (orderStatusEnum.getStatus().equals(status)) return orderStatusEnum;
        }
        return null;
    }
}

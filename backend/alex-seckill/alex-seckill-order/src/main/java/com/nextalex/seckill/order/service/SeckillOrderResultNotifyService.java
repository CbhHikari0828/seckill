package com.nextalex.seckill.order.service;

import com.nextalex.seckill.order.model.vo.FindSeckillOrderResultRspVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SeckillOrderResultNotifyService {

    /**
     * 订阅秒杀订单处理结果
     * @param userId
     * @param orderNo
     * @return
     */
    SseEmitter subscribe(Long userId, String orderNo);

    /**
     * 推送秒杀订单处理结果
     * @param userId
     * @param result
     */
    void notifyOrderResult(Long userId, FindSeckillOrderResultRspVO result);
}

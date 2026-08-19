package com.nextalex.seckill.order.service;

import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.order.model.dto.SeckillOrderMqDTO;
import com.nextalex.seckill.order.model.vo.DoSeckillReqVO;
import com.nextalex.seckill.order.model.vo.DoSeckillRspVO;
import com.nextalex.seckill.order.model.vo.FindSeckillOrderResultReqVO;
import com.nextalex.seckill.order.model.vo.FindSeckillOrderResultRspVO;

public interface OrderService {

    /**
     * 秒杀下单
     * @param doSeckillReqVO
     * @return
     */
    Response<DoSeckillRspVO> doSeckill(DoSeckillReqVO doSeckillReqVO);

    /**
     * 异步消费秒杀下单消息：扣减库存 + 创建订单
     *
     * @param message
     */
    void createSeckillOrder(SeckillOrderMqDTO message);

    /**
     * 查询秒杀订单处理结果
     * @param reqVO
     * @return
     */
    Response<FindSeckillOrderResultRspVO> findSeckillOrderResult(FindSeckillOrderResultReqVO reqVO);
}

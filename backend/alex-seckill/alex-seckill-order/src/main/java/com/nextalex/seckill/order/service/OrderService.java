package com.nextalex.seckill.order.service;

import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.order.model.vo.DoSeckillReqVO;
import com.nextalex.seckill.order.model.vo.DoSeckillRspVO;

public interface OrderService {

    /**
     * 秒杀下单
     * @param doSeckillReqVO
     * @return
     */
    Response<DoSeckillRspVO> doSeckill(DoSeckillReqVO doSeckillReqVO);
}

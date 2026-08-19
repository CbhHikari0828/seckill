package com.nextalex.seckill.order.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.nextalex.seckill.common.aspect.ApiOperationLog;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.order.model.vo.*;
import com.nextalex.seckill.order.service.OrderService;
import com.nextalex.seckill.order.service.SeckillOrderResultNotifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/seckill/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private SeckillOrderResultNotifyService seckillOrderResultNotifyService;

    @PostMapping
    @ApiOperationLog(description = "秒杀下单")
    public Response<DoSeckillRspVO> doSeckill(@RequestBody @Validated DoSeckillReqVO seckillReqVO) {
        return orderService.doSeckill(seckillReqVO);
    }

    @PostMapping("/result")
    @ApiOperationLog(description = "查询秒杀订单处理结果")
    public Response<FindSeckillOrderResultRspVO> findSeckillOrderResultRspVOResponse(@RequestBody @Validated FindSeckillOrderResultReqVO reqVO) {
        return orderService.findSeckillOrderResult(reqVO);
    }

    @PostMapping(value = "/result/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "订阅秒杀订单处理结果")
    public SseEmitter subscribeSeckillOrderResult(@RequestBody @Validated SubscribeSeckillOrderResultReqVO reqVO) {
        // 获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        return seckillOrderResultNotifyService.subscribe(userId, reqVO.getOrderNo());
    }
}

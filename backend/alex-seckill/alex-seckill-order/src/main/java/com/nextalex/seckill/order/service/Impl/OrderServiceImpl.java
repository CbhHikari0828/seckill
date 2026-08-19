package com.nextalex.seckill.order.service.Impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.nextalex.seckill.common.config.RabbitMQConfig;
import com.nextalex.seckill.common.constant.RedisKeyConstants;
import com.nextalex.seckill.common.domain.dataobject.GoodsDO;
import com.nextalex.seckill.common.domain.dataobject.SeckillActivityDO;
import com.nextalex.seckill.common.domain.dataobject.SeckillGoodsDO;
import com.nextalex.seckill.common.domain.dataobject.SeckillOrderDO;
import com.nextalex.seckill.common.domain.mapper.GoodsDOMapper;
import com.nextalex.seckill.common.domain.mapper.SeckillActivityDOMapper;
import com.nextalex.seckill.common.domain.mapper.SeckillGoodsDOMapper;
import com.nextalex.seckill.common.domain.mapper.SeckillOrderDOMapper;
import com.nextalex.seckill.common.enums.ResponseCodeEnum;
import com.nextalex.seckill.common.exception.BizException;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.order.enums.OrderStatusEnum;
import com.nextalex.seckill.order.model.dto.SeckillOrderMqDTO;
import com.nextalex.seckill.order.model.vo.DoSeckillReqVO;
import com.nextalex.seckill.order.model.vo.DoSeckillRspVO;
import com.nextalex.seckill.order.model.vo.FindSeckillOrderResultReqVO;
import com.nextalex.seckill.order.model.vo.FindSeckillOrderResultRspVO;
import com.nextalex.seckill.order.mq.SeckillOrderMessageSender;
import com.nextalex.seckill.order.service.OrderService;
import com.nextalex.seckill.order.service.SeckillOrderResultNotifyService;
import com.nextalex.seckill.order.utils.OrderLockUtils;
import io.micrometer.common.util.StringUtils;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SeckillOrderDOMapper seckillOrderDOMapper;

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private OrderLockUtils orderLockUtils;

    @Resource
    private SeckillOrderMessageSender seckillOrderMessageSender;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillOrderResultNotifyService seckillOrderResultNotifyService;



    /**
     * 秒杀下单
     * @param doSeckillReqVO
     * @return
     */
    @Override
    public Response<DoSeckillRspVO> doSeckill(DoSeckillReqVO doSeckillReqVO) {
        // 活动ID
        Long activityId = doSeckillReqVO.getActivityId();
        // 商品ID
        Long goodsId = doSeckillReqVO.getGoodsId();
        // 获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 当前登录用户 ID: {}", userId);
        // 应用层锁：防止同一用户并发重复下单
        // 构建锁 Key "userId:activityId:goodsId"
        String lockKey = userId + ":" + activityId + ":" + goodsId;
        // 尝试获取锁，获取失败，则说明该用户对该商品已经有请求在处理中
        if (!orderLockUtils.tryLock(lockKey)) {
            log.warn("==> 应用层锁拦截重复下单, userId: {}, activityId: {}, goodsId: {}", userId, activityId, goodsId);
            throw new BizException(ResponseCodeEnum.SECKILL_ORDER_PROCESSING);
        }
        try {
            // 校检活动是否存在
            SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
            if (Objects.isNull(activityDO)) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
            // 秒杀下单时间
            LocalDateTime now = LocalDateTime.now();
            // 活动是否开始/结束
            if (now.isBefore(activityDO.getBeginTime())) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);
            if (now.isAfter(activityDO.getEndTime())) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
            //  查询商品，校检商品是否存在
            SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(goodsId, activityId);
            if (Objects.isNull(seckillGoodsDO)) throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
            // 校检库存
            if (seckillGoodsDO.getSeckillStock() <= 0) throw new BizException(ResponseCodeEnum.SECKILL_GOODS_SOLD_OUT);
            // 利用雪花算法生成 ID
            String orderNo = IdUtil.getSnowflakeNextIdStr();
            // 构建消息体
            SeckillOrderMqDTO seckillOrderMqDTO = SeckillOrderMqDTO.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .goodsId(goodsId)
                    .seckillGoodsId(seckillGoodsDO.getId())
                    .seckillPrice(seckillGoodsDO.getSeckillPrice())
                    .orderNo(orderNo)
                    .requestTime(now)
                    .build();
            // 发送 MQ，内部会携带 CorrelationData(orderNo)，方便生产者确认回调定位消息
            seckillOrderMessageSender.send(seckillOrderMqDTO);
            // 记录订单状态至Redis，支持用户查询
            saveOrderStatus(userId, orderNo, OrderStatusEnum.PROCESSING.getStatus());
            log.info("==> 秒杀下单消息已发送至 MQ, orderNo: {}, userId: {}, activityId: {}, goodsId: {}",
                    orderNo, userId, activityId, goodsId);
            // 立即响参 "处理中"，扣库存 + 建订单交给消费者异步处理
            return Response.success(
                    DoSeckillRspVO.builder()
                            .orderNo(orderNo)
                            .status(OrderStatusEnum.PROCESSING.getStatus())
                            .build()
            );
        }finally {
            orderLockUtils.unlock(lockKey);
        }


    }

    /**
     * 异步消费秒杀下单消息：扣减库存 + 创建订单
     *
     * @param message
     */
    @Override
    public void createSeckillOrder(SeckillOrderMqDTO message) {
        Long activityId = message.getActivityId();
        Long goodsId = message.getGoodsId();
        Long userId = message.getUserId();
        String orderNo = message.getOrderNo();
        log.info("==> 消费秒杀下单消息, orderNo: {}, userId: {}, activityId: {}, goodsId: {}",
                orderNo, userId, activityId, goodsId);
        // 查询商品，用于冗余到订单中
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);
        // 订单过期时间
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);
        // 编程事务，精确控制事务边界
        try {
            SeckillOrderDO orderDO = transactionTemplate.execute(status -> {
               // 扣减库存
               int count  = seckillGoodsDOMapper.deductStock(message.getSeckillGoodsId());
               if (count == 0) {
                   log.warn("==> 扣减库存失败，商品已售罄或已下架, orderNo: {}", orderNo);
                   throw new BizException(ResponseCodeEnum.SECKILL_GOODS_SOLD_OUT);
               }
               // 创建订单
                SeckillOrderDO order = SeckillOrderDO.builder()
                        .userId(userId)
                        .activityId(activityId)
                        .goodsId(goodsId)
                        .orderNo(message.getOrderNo())
                        .seckillPrice(message.getSeckillPrice())
                        .goodsName(goodsDO.getGoodsName())
                        .goodImg(goodsDO.getGoodsImg())
                        .status(OrderStatusEnum.PENDING_PAYMENT.ordinal())
                        .expireTime(expireTime)
                        .isDeleted(0)
                        .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                        .build();
               seckillOrderDOMapper.insert(order);
               return order;
            });
            if (Objects.isNull(orderDO)) {
                // 扣库存失败，更新 Redis 中订单状态为秒杀失败
                saveOrderStatus(userId, orderNo, OrderStatusEnum.SECKILL_FAILED.getStatus());
                // 推送SSE结果
                seckillOrderResultNotifyService.notifyOrderResult(userId, buildStatusResult(orderNo, OrderStatusEnum.SECKILL_FAILED));
                return;
            }
            // 订单创建成功，更新 Redis 中订单状态为待支付
            saveOrderStatus(userId, orderNo, OrderStatusEnum.PENDING_PAYMENT.getStatus());
            // 推送 sse 结果
            seckillOrderResultNotifyService.notifyOrderResult(userId, buildOrderResult(orderDO));
            log.info("==> 异步秒杀下单成功, orderNo: {}", orderNo);
        }catch (DuplicateKeyException e) {
            // 幂等兜底：order_no 唯一索引命中，说明是重复投递的消息
            // 直接当作成功处理，不再抛异常，避免消费者把消息无限重投
            log.warn("==> 重复消费秒杀消息，订单已存在，幂等返回, orderNo: {}", orderNo);
            // 不能直接写 PENDING_PAYMENT，避免把已支付、已取消等状态回退
            SeckillOrderDO existsOrderDO = seckillOrderDOMapper.selectByOrderNoAndUserId(orderNo, userId);
            if (Objects.nonNull(existsOrderDO)) {
                saveOrderStatus(userId, orderNo, existsOrderDO.getStatus());
                seckillOrderResultNotifyService.notifyOrderResult(userId, buildOrderResult(existsOrderDO));
            }
            else log.warn("==> 重复消费命中唯一索引，但未查询到当前用户订单, orderNo: {}, userId: {}", orderNo, userId);
        }
    }

    /**
     * 查询秒杀订单处理结果
     * @param reqVO
     * @return
     */
    @Override
    public Response<FindSeckillOrderResultRspVO> findSeckillOrderResult(FindSeckillOrderResultReqVO reqVO) {
        // 订单号
        String orderNo = reqVO.getOrderNo();
        // 当前用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        // 查询 Redis 获取订单状态
        String redisKey = RedisKeyConstants.SECKILL_ORDER_STATUS_PREFIX + userId + ":" + orderNo;
        String orderStatus = stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotBlank(orderStatus)) {
            OrderStatusEnum statusEnum = OrderStatusEnum.getByStatus(Integer.valueOf(orderStatus));
            // 如果是下述两种状态立即返回
            if (statusEnum.equals(OrderStatusEnum.PROCESSING) || statusEnum.equals(OrderStatusEnum.SECKILL_FAILED)) {
                return Response.success(FindSeckillOrderResultRspVO.builder()
                        .orderNo(orderNo)
                        .status(statusEnum.getStatus())
                        .statusDesc(statusEnum.getDescription())
                        .build()
                );
            }
        }
        // 如果是待支付状态，继续查询
        SeckillOrderDO orderDO = seckillOrderDOMapper.selectByOrderNoAndUserId(orderNo, userId);
        // 如果没有消息，说明消费还没消费到，返回处理中
        if (Objects.isNull(orderDO)) {
            return Response.success(FindSeckillOrderResultRspVO.builder()
                    .orderNo(orderNo)
                    .status(OrderStatusEnum.PROCESSING.getStatus())
                    .statusDesc(OrderStatusEnum.PROCESSING.getDescription())
                    .build()
            );
        }
        return Response.success(FindSeckillOrderResultRspVO.builder()
                .orderId(orderDO.getId())
                .orderNo(orderDO.getOrderNo())
                .status(orderDO.getStatus())
                .statusDesc(OrderStatusEnum.getDescriptionByStatus(orderDO.getStatus()))
                .goodsId(orderDO.getGoodsId())
                .goodsName(orderDO.getGoodsName())
                .goodsImg(orderDO.getGoodImg())
                .seckillPrice(orderDO.getSeckillPrice())
                .build());
    }

    /**
     * 秒杀下单核心逻辑
     * @param activityId
     * @param goodsId
     * @param userId
     * @return
     */
    private Response<DoSeckillRspVO> processSeckill(Long activityId, Long goodsId, Long userId) {
        // 校检活动是否存在
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        LocalDateTime now = LocalDateTime.now();
        // 是否活动还没开始
        if (now.isBefore(activityDO.getBeginTime())) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_STARTED);
        if (now.isAfter(activityDO.getEndTime())) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        // 查询商品
        SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(activityId, goodsId);
        if (Objects.isNull(seckillGoodsDO)) throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        if (seckillGoodsDO.getSeckillStock() <= 0) throw new BizException(ResponseCodeEnum.SECKILL_SOLD_OUT);
        // 查询商品信息
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);
        // 创建订单
        String orderNo = IdUtil.getSnowflakeNextIdStr();
        // 订单过期时间
        LocalDateTime expireTime = now.plusMinutes(30);

        // 编程式事务，精确控制事务边界
        SeckillOrderDO orderDO = transactionTemplate.execute(status -> {
            int count = seckillGoodsDOMapper.deductStock(seckillGoodsDO.getId());
            if (count == 0) throw new BizException(ResponseCodeEnum.SECKILL_SOLD_OUT);
            SeckillOrderDO order = SeckillOrderDO.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .goodsId(goodsId)
                    .orderNo(orderNo)
                    .seckillPrice(seckillGoodsDO.getSeckillPrice())
                    .goodsName(goodsDO.getGoodsName())
                    .goodImg(goodsDO.getGoodsName())
                    .status(goodsDO.getStatus())
                    .expireTime(expireTime)
                    .isDeleted(0)
                    .createTime(goodsDO.getCreateTime())
                    .updateTime(goodsDO.getUpdateTime())
                    .build();
            try {
                seckillOrderDOMapper.insert(order);
            }catch (Exception e) {
                log.warn("==> 重复下单, userId: {}, activityId: {}, goodsId: {}", userId, activityId, goodsId);
                throw new BizException(ResponseCodeEnum.SECKILL_ORDER_DUPLICATE);
            }
            return order;
        });

        log.info("==> 秒杀下单成功, orderId: {}, orderNo: {}", orderDO.getId(), orderNo);



        // 组装响应数据
        DoSeckillRspVO rspVO = DoSeckillRspVO.builder()
                .orderNo(orderNo)
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .build();
        return Response.success(rspVO);
    }

    /**
     * 保存秒杀订单异步处理
     * @param userId
     * @param orderNo
     * @param status
     */
    private void saveOrderStatus(Long userId, String orderNo, Integer status) {
        String redisKey = RedisKeyConstants.SECKILL_ORDER_STATUS_PREFIX + userId + ":" + orderNo;
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(status), RedisKeyConstants.SECKILL_ORDER_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private FindSeckillOrderResultRspVO buildOrderResult(SeckillOrderDO orderDO) {
        return FindSeckillOrderResultRspVO.builder()
                .orderId(orderDO.getId())
                .orderNo(orderDO.getOrderNo())
                .status(orderDO.getStatus())
                .statusDesc(OrderStatusEnum.getDescriptionByStatus(orderDO.getStatus()))
                .goodsId(orderDO.getGoodsId())
                .goodsName(orderDO.getGoodsName())
                .goodsImg(orderDO.getGoodImg())
                .seckillPrice(orderDO.getSeckillPrice())
                .build();
    }

    private FindSeckillOrderResultRspVO buildStatusResult(String orderNo, OrderStatusEnum status){
        return FindSeckillOrderResultRspVO.builder()
                .orderNo(orderNo)
                .status(status.getStatus())
                .statusDesc(status.getDescription())
                .build();
    }
}

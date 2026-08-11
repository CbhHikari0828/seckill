package com.nextalex.seckill.order.service.Impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
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
import com.nextalex.seckill.order.model.vo.DoSeckillReqVO;
import com.nextalex.seckill.order.model.vo.DoSeckillRspVO;
import com.nextalex.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;

    @Resource
    private SeckillOrderDOMapper seckillOrderDOMapper;

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<DoSeckillRspVO> doSeckill(DoSeckillReqVO doSeckillReqVO) {
        // 活动ID
        Long activityId = doSeckillReqVO.getActivityId();
        // 商品ID
        Long goodsId = doSeckillReqVO.getGoodsId();
        // 获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        log.info("==> 当前登录用户 ID: {}", userId);
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
        // 扣减库存
        int count = seckillOrderDOMapper.deductStock(seckillGoodsDO.getId());
        if (count == 0) throw new BizException(ResponseCodeEnum.SECKILL_SOLD_OUT);
        // 查询商品信息
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);
        // 创建订单
        String orderNo = IdUtil.getSnowflakeNextIdStr();
        // 订单过期时间
        LocalDateTime expireTime = now.plusMinutes(30);

        SeckillOrderDO orderDO = SeckillOrderDO.builder()
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
            seckillOrderDOMapper.insert(orderDO);
        }catch (Exception e) {
            log.warn("==> 重复下单, userId: {}, activityId: {}, goodsId: {}", userId, activityId, goodsId);
            throw new BizException(ResponseCodeEnum.SECKILL_ORDER_DUPLICATE);
        }
        log.info("==> 秒杀下单成功, orderId: {}, orderNo: {}", orderDO.getId(), orderNo);

        // 组装响应数据
        DoSeckillRspVO rspVO = DoSeckillRspVO.builder()
                .orderId(orderDO.getId())
                .orderNo(orderNo)
                .goodsName(goodsDO.getGoodsName())
                .goodsImg(goodsDO.getGoodsImg())
                .seckillPrice(seckillGoodsDO.getSeckillPrice())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .expireTime(expireTime)
                .build();
        return Response.success(rspVO);

    }
}

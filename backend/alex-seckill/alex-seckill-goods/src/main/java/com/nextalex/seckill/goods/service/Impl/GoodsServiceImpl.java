package com.nextalex.seckill.goods.service.Impl;

import cn.hutool.core.collection.CollUtil;
import com.nextalex.seckill.common.domain.dataobject.GoodsDO;
import com.nextalex.seckill.common.domain.dataobject.SeckillActivityDO;
import com.nextalex.seckill.common.domain.dataobject.SeckillGoodsDO;
import com.nextalex.seckill.common.domain.mapper.GoodsDOMapper;
import com.nextalex.seckill.common.domain.mapper.SeckillActivityDOMapper;
import com.nextalex.seckill.common.domain.mapper.SeckillGoodsDOMapper;
import com.nextalex.seckill.common.enums.ResponseCodeEnum;
import com.nextalex.seckill.common.exception.BizException;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.goods.enums.ActivityStatusEnum;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsListReqVO;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsRspVO;
import com.nextalex.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsDOMapper goodsDOMapper;

    @Resource
    private SeckillGoodsDOMapper seckillGoodsDOMapper;

    @Resource
    private SeckillActivityDOMapper seckillActivityDOMapper;


    @Override
    public Response<List<FindSeckillGoodsRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO) {
        // 活动 ID
        Long activityId = reqVO.getActivityId();
        log.info("==> 查询秒杀商品列表, activityId: {}", activityId);
//        1. 查询活动信息
        SeckillActivityDO seckillActivityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(seckillActivityDO)) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        // 2.查询活动所属秒杀商品
        List<SeckillGoodsDO> seckillGoodsDOS = seckillGoodsDOMapper.selectByActivityId(activityId);
        if (CollUtil.isEmpty(seckillGoodsDOS)) {
            log.info("==> 该活动下暂无秒杀商品, activityId: {}", activityId);
            return Response.success(Collections.emptyList());
        }
        // 3.批量查询相关商品信息
        List<Long> goodsIds = seckillGoodsDOS.stream().map(SeckillGoodsDO::getGoodsId).collect(Collectors.toList());
        // 一次性查询所有商品
        List<GoodsDO> goodsDOS = goodsDOMapper.selectByIds(goodsIds);
        // 商品ID和商品信息映射为Map
        Map<Long, GoodsDO> goodsMap = goodsDOS.stream().collect(Collectors.toMap(GoodsDO::getId, GoodsDO -> GoodsDO));
        // 4.计算活动状态
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(seckillActivityDO);
        // 5.组装响应数据
        List<FindSeckillGoodsRspVO> rspVOS = new ArrayList<>();
        for (SeckillGoodsDO seckillGoodsDO : seckillGoodsDOS) {
            // 设置商品原价
            GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(seckillGoodsDO.getId());
            BigDecimal goodPrice = null;
            if (Objects.nonNull(goodsDO)) goodPrice = goodsDO.getGoodsPrice();
            FindSeckillGoodsRspVO rspVO = FindSeckillGoodsRspVO.builder()
                    .id(seckillGoodsDO.getId())
                    .goodsPrice(goodPrice)
                    .activityId(seckillGoodsDO.getActivityId())
                    .seckillTitle(seckillGoodsDO.getSeckillTitle())
                    .goodsId(seckillGoodsDO.getGoodsId())
                    .seckillImg(seckillGoodsDO.getSeckillImg())
                    .seckillPrice(seckillGoodsDO.getSeckillPrice())
                    .seckillTotal(seckillGoodsDO.getSeckillTotal())
                    .seckillStock(seckillGoodsDO.getSeckillStock())
                    .ActivityStatus(activityStatusEnum.getCode())
                    .beginTime(seckillActivityDO.getBeginTime())
                    .endTime(seckillActivityDO.getEndTime())
                    .build();
            rspVOS.add(rspVO);
        }
        return Response.success(rspVOS);

    }


    private ActivityStatusEnum calculateActivityStatus(SeckillActivityDO activityDO) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activityDO.getBeginTime())) return ActivityStatusEnum.NOT_START;
        else if (now.isAfter(activityDO.getEndTime())) return ActivityStatusEnum.ENDED;
        else return ActivityStatusEnum.ING;
    }
}

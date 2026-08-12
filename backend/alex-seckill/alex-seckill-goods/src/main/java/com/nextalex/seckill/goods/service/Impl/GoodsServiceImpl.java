package com.nextalex.seckill.goods.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nextalex.seckill.common.constant.RedisKeyConstants;
import com.nextalex.seckill.common.domain.dataobject.*;
import com.nextalex.seckill.common.domain.mapper.*;
import com.nextalex.seckill.common.enums.ResponseCodeEnum;
import com.nextalex.seckill.common.exception.BizException;
import com.nextalex.seckill.common.utils.JsonUtils;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.goods.enums.ActivityStatusEnum;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsDetailReqVO;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsDetailRspVO;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsListReqVO;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsListRspVO;
import com.nextalex.seckill.goods.service.GoodsService;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private GoodsImgDOMapper goodsImgDOMapper;

    @Resource
    private GoodsDetailDOMapper goodsDetailDOMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 查询活动商品列表
     * @param reqVO
     * @return
     */
    @Override
    public Response<List<FindSeckillGoodsListRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO) {
        // 活动 ID
        Long activityId = reqVO.getActivityId();
        log.info("==> 查询秒杀商品列表, activityId: {}", activityId);
        // 构建RedisKey
        String redisKey = RedisKeyConstants.GOODS_LIST_PREFIX + activityId;
        // 先查Redis缓存
        String redisJsonValue = stringRedisTemplate.opsForValue().get(redisKey);
        // 若缓存不为空
        if (StrUtil.isNotEmpty(redisJsonValue)) {
            log.info("==> 命中商品列表缓存, redisKey: {}", redisKey);
            // 缓存命中
            // 将Json反序列化为List对象
            List<FindSeckillGoodsListRspVO> cachedList = JsonUtils.parseArray(redisJsonValue,FindSeckillGoodsListRspVO.class);
            // 设置库存字段值（因为库存变化频繁，需要从数据库查最新的）
            supplementStock(cachedList, activityId);
            // 实时计算活动状态
            FindSeckillGoodsListRspVO first = cachedList.get(0);
            ActivityStatusEnum activityStatusEnum = calculateActivityStatus(first.getBeginTime(),first.getEndTime());
            cachedList.forEach(item -> {
                item.setActivityStatus(activityStatusEnum.getCode());
            });
            return Response.success(cachedList);
        }
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
        List<FindSeckillGoodsListRspVO> rspVOS = new ArrayList<>();
        for (SeckillGoodsDO seckillGoodsDO : seckillGoodsDOS) {
            // 设置商品原价
            GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(seckillGoodsDO.getId());
            BigDecimal goodPrice = null;
            if (Objects.nonNull(goodsDO)) goodPrice = goodsDO.getGoodsPrice();
            FindSeckillGoodsListRspVO rspVO = FindSeckillGoodsListRspVO.builder()
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
        // 商品列表写入Redis缓存
        log.info("==> 商品列表缓存未命中，将数据写入 Redis, redisKey: {}", redisKey);
        // 动态计算过期时间
        Long ttlSecond = RedisKeyConstants.calculateTtlSeconds(seckillActivityDO.getEndTime());
        if (Objects.nonNull(ttlSecond) && ttlSecond > 0) stringRedisTemplate.opsForValue().set(redisKey,JsonUtils.toJsonString(rspVOS), ttlSecond, TimeUnit.MINUTES);
        else stringRedisTemplate.opsForValue().set(redisKey,JsonUtils.toJsonString(rspVOS), RedisKeyConstants.ENDED_ACTIVITY_TTL_MINUTES, TimeUnit.MINUTES);

        return Response.success(rspVOS);

    }

    /**
     * 查询秒杀商详情
     * @param reqVO
     * @return
     */
    @Override
    public Response<FindSeckillGoodsDetailRspVO> findSeckillGoodsDetail(FindSeckillGoodsDetailReqVO reqVO) {
        Long goodsId = reqVO.getGoodsId();
        Long activityId = reqVO.getActivityId();
        log.info("==> 查询秒杀商品详情, goodsId: {}, activityId: {}", goodsId, activityId);
        // 构建Redis缓存key
        String redisKey = RedisKeyConstants.GOODS_DETAIL_PREFIX + activityId + ":" + goodsId;
        // 先查Redis缓存
        String redisJsonValue = stringRedisTemplate.opsForValue().get(redisKey);
        // 若缓存不为空,Redis逻辑
        if (StrUtil.isNotEmpty(redisJsonValue)) {
            log.info("==> 命中商品详情缓存, redisKey: {}", redisKey);
            // 缓存命中
            // 手动将 String 字符串，反序列化为商品详情对象
            FindSeckillGoodsDetailRspVO cacheDetail = JsonUtils.parseObject(redisJsonValue, FindSeckillGoodsDetailRspVO.class);
            // 数据库查询最新缓存值
            SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByGoodsIdAndActivityId(goodsId, activityId);
            if (Objects.nonNull(seckillGoodsDO)) cacheDetail.setSeckillStock(seckillGoodsDO.getSeckillStock());
            // 实时计算活动状态
            ActivityStatusEnum activityStatusEnum = calculateActivityStatus(cacheDetail.getBeginTime(), cacheDetail.getEndTime());
            cacheDetail.setActivityStatus(activityStatusEnum.getCode());
            return Response.success(cacheDetail);
        }
        // 1. 根据活动 ID 查询活动信息，校验活动是否存在
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        // 2. 根据活动 ID 和商品 ID 查询秒杀商品
        SeckillGoodsDO seckillGoodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(activityId, goodsId);
        if (Objects.isNull(seckillGoodsDO)) throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        // 3. 根据 goodsId 查询商品基本信息, 如商品名称、原价
        GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(goodsId);
        // 4. 根据 goodsId 查询商品轮播图列表
        List<GoodsImgDO> goodsImgDOS = goodsImgDOMapper.selectByGoodsId(goodsId);
        List<String> goodsImgs = null;
        if (CollUtil.isNotEmpty(goodsImgDOS)) goodsImgs = goodsImgDOS.stream().map(GoodsImgDO::getImgUrl).toList();
        // 5. 根据 goodsId 查询商品详情 HTML
        GoodsDetailDO goodsDetailDO = goodsDetailDOMapper.SelectByGoodsId(goodsId);
        // 6.计算活动状态
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(activityDO);
        // 7. 组装响应数据
        FindSeckillGoodsDetailRspVO rspVO = FindSeckillGoodsDetailRspVO.builder()
                .id(seckillGoodsDO.getGoodsId())
                .goodsId(goodsDO.getId())
                .activityId(activityDO.getId())
                .goodsImgs(goodsImgs)
                .goodsDetail(goodsDetailDO.getDetailContent())
                .seckillPrice(seckillGoodsDO.getSeckillPrice())
                .seckillTotal(seckillGoodsDO.getSeckillTotal())
                .seckillStock(seckillGoodsDO.getSeckillStock())
                .activityStatus(activityStatusEnum.getCode())
                .beginTime(activityDO.getBeginTime())
                .endTime(activityDO.getEndTime())
                .build();
        if (Objects.nonNull(goodsDO)) {
            rspVO.setGoodsName(goodsDO.getGoodsName());
            rspVO.setGoodsPrice(goodsDO.getGoodsPrice());
        }
        if (Objects.nonNull(goodsDetailDO)) rspVO.setGoodsDetail(goodsDetailDO.getDetailContent());
        // 将商品详情写入 Redis 缓存
        log.info("==> 商品详情缓存未命中，将数据写入 Redis, redisKey: {}", redisKey);
        Long ttlSecond = RedisKeyConstants.calculateTtlSeconds(activityDO.getEndTime());
        if (Objects.nonNull(ttlSecond) && ttlSecond > 0) stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(rspVO), ttlSecond, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(rspVO), RedisKeyConstants.ENDED_ACTIVITY_TTL_MINUTES, TimeUnit.MINUTES);
        return Response.success(rspVO);
    }

    @Override
    public Response<?> preheatActivityGoods(Long activityId) {
        log.info("==> 开始预热活动商品缓存, activityId: {}", activityId);
        // 查询活动信息
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            log.info("==> 预热跳过：活动不存在, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        // 动态计算TTL预热时间
        Long ttlSecond = RedisKeyConstants.calculateTtlSeconds(activityDO.getEndTime());
        if (Objects.isNull(ttlSecond) || ttlSecond <= 0) {
            log.info("==> 预热跳过：活动已结束, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_ENDED);
        }
        // 查询活动下所有秒杀商品
        List<SeckillGoodsDO> seckillGoodsDOS = seckillGoodsDOMapper.selectByActivityId(activityId);
        if (CollUtil.isEmpty(seckillGoodsDOS)) {
            log.info("==> 预热跳过：活动下无商品, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_GOODS_EMPTY);
        }
        // 批量查询商品原价
        List<Long> goodIds = seckillGoodsDOS.stream().map(SeckillGoodsDO::getGoodsId).toList();
        // 查询原来商品->转Map映射方便后续操作
        List<GoodsDO> goodsDOS = goodsDOMapper.selectByIds(goodIds);
        Map<Long,GoodsDO> goodsDOMap = goodsDOS.stream().collect(Collectors.toMap(GoodsDO::getId,GoodsDO->GoodsDO));
        // 预热商品列表缓存
        String redisKey = RedisKeyConstants.GOODS_LIST_PREFIX + activityId;
        List<FindSeckillGoodsListRspVO> rspVOS = new ArrayList<>();
        for (SeckillGoodsDO goodsDO : seckillGoodsDOS) {
            FindSeckillGoodsListRspVO findSeckillGoodsListRspVO = FindSeckillGoodsListRspVO.builder()
                    .goodsId(goodsDO.getGoodsId())
                    .goodsPrice(goodsDO.getSeckillPrice()) // flag
                    .activityId(goodsDO.getActivityId())
                    .seckillTitle(goodsDO.getSeckillTitle())
                    .seckillImg(goodsDO.getSeckillImg())
                    .seckillPrice(goodsDO.getSeckillPrice())
                    .seckillTotal(goodsDO.getSeckillTotal())
                    .seckillStock(goodsDO.getSeckillStock())
                    .ActivityStatus(calculateActivityStatus(activityDO).getCode())
                    .beginTime(activityDO.getBeginTime())
                    .endTime(activityDO.getEndTime())
                    .build();
            // 商品原价
            GoodsDO goodsDO1 = goodsDOMap.get(goodsDO.getId());
            if (Objects.nonNull(goodsDO1)) findSeckillGoodsListRspVO.setGoodsPrice(goodsDO1.getGoodsPrice());
            rspVOS.add(findSeckillGoodsListRspVO);
        }
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(rspVOS), ttlSecond, TimeUnit.MINUTES);
        log.info("==> 预热商品列表缓存成功, key: {}, TTL: {}s", redisKey, ttlSecond);
        // 预热每个商品详情缓存
        for (SeckillGoodsDO seckillGoodsDO : seckillGoodsDOS) {
            String detailKey = RedisKeyConstants.GOODS_DETAIL_PREFIX + activityId + ":" + seckillGoodsDO.getId();
            // 查询每个商品基本信息
            GoodsDO goodsDO = goodsDOMapper.selectByPrimaryKey(seckillGoodsDO.getId());
            // 查询商品轮播图
            List<GoodsImgDO> goodsImgDOS = goodsImgDOMapper.selectByGoodsId(seckillGoodsDO.getId());
            List<String> goodsImgs = null;
            if (CollUtil.isNotEmpty(goodsDOMap)) goodsImgs = goodsImgDOS.stream().map(GoodsImgDO::getImgUrl).toList();
            // 查询商品详情 HTML
            GoodsDetailDO goodsDetailDO = goodsDetailDOMapper.selectByPrimaryKey(seckillGoodsDO.getId());
            // 组装详情 VO
            FindSeckillGoodsDetailRspVO detailRspVO = FindSeckillGoodsDetailRspVO.builder()
                    .goodsId(goodsDO.getId())
                    .activityId(activityId)
                    .goodsImgs(goodsImgs)
                    .seckillPrice(seckillGoodsDO.getSeckillPrice())
                    .seckillTotal(seckillGoodsDO.getSeckillTotal())
                    .seckillStock(seckillGoodsDO.getSeckillStock())
                    .activityStatus(calculateActivityStatus(activityDO).getCode())
                    .beginTime(activityDO.getBeginTime())
                    .endTime(activityDO.getEndTime())
                    .build();
            // 设置商品名称和原价
            if (Objects.nonNull(goodsDO)) {
                detailRspVO.setGoodsName(goodsDO.getGoodsName());
                detailRspVO.setGoodsPrice(goodsDO.getGoodsPrice());
            }
            // 设置商品详情
            if (Objects.nonNull(goodsDetailDO)) detailRspVO.setGoodsDetail(goodsDetailDO.getDetailContent());
            stringRedisTemplate.opsForValue().set(detailKey, JsonUtils.toJsonString(detailRspVO), ttlSecond, TimeUnit.MINUTES);
            log.info("==> 预热活动 {} 的 {} 个商品详情缓存完成", activityId, seckillGoodsDOS.size());
        }
        return Response.success();
    }


    /**
     * 计算活动状态
     * @param activityDO
     * @return
     */
    private ActivityStatusEnum calculateActivityStatus(SeckillActivityDO activityDO) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activityDO.getBeginTime())) return ActivityStatusEnum.NOT_START;
        else if (now.isAfter(activityDO.getEndTime())) return ActivityStatusEnum.ENDED;
        else return ActivityStatusEnum.ING;
    }


    /**
     * 计算活动状态（多态体现）
     * @param beginTime
     * @param endTime
     * @return
     */
    private ActivityStatusEnum calculateActivityStatus(LocalDateTime beginTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(beginTime)) return ActivityStatusEnum.NOT_START;
        else if (now.isAfter(endTime)) return ActivityStatusEnum.ENDED;
        else return ActivityStatusEnum.ING;
    }

    private void supplementStock(List<FindSeckillGoodsListRspVO> goodsList, Long activityId) {
        // 根据活动 ID 查询秒杀商品的实时库存（仅查 id 和 seckill_stock 字段，减少 IO 开销）
        List<SeckillGoodsDO> seckillGoodsDOS = seckillGoodsDOMapper.selectStockByActivityId(activityId);
        // 构建ID->库存映射
        Map<Long, Integer> stockMap = seckillGoodsDOS.stream().collect(Collectors.toMap(SeckillGoodsDO::getId, SeckillGoodsDO::getSeckillStock));
        // 补充库存到缓存
        for (FindSeckillGoodsListRspVO rspVO : goodsList) {
            Integer stock = stockMap.get(rspVO.getId());
            if (Objects.nonNull(stock)){
                rspVO.setSeckillStock(stock);
            }
        }

    }
}

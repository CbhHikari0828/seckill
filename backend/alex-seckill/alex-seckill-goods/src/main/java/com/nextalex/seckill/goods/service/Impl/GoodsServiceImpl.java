package com.nextalex.seckill.goods.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
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
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
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

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private Cache<String, String> goodsListLocalCache;

    @Resource
    private Cache<String, String> goodsDetailLocalCache;



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
        // L1: 先查 Caffeine 本地缓存（微秒级，无网络开销）
        String localCacheValue = goodsListLocalCache.getIfPresent(redisKey);
        if (StrUtil.isNotEmpty(localCacheValue)) {
            log.info("==> 命中本地缓存（L1）, key: {}", redisKey);
            // 手动将 String 字符串，反序列化为商品列表
            List<FindSeckillGoodsListRspVO> cacheList = processCachedGoodsList(localCacheValue, activityId);
            return Response.success(cacheList);
        }
        // 第一道防线：布隆过滤器校验活动是否存在
        // 如果布隆过滤器返回 "不存在"，绝对正确，说明该活动 ID 一定不合法，直接拒绝掉
        RBloomFilter<Long> activityBloom = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_ACTIVITY_BLOOM_KEY);
        if (activityBloom.isExists() && !activityBloom.contains(activityId)) {
            log.info("==> 布隆过滤器拦截：活动不存在, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        // 先查Redis缓存
        String redisJsonValue = stringRedisTemplate.opsForValue().get(redisKey);
        // 若缓存不为空
        if (StrUtil.isNotEmpty(redisJsonValue)) {
            log.info("==> 命中商品列表缓存, redisKey: {}", redisKey);

            if (Objects.equals(redisJsonValue, RedisKeyConstants.NULL_CACHE_VALUE)) {
                log.info("==> 命中空值缓存，活动不存在, redisKey: {}", redisKey);
                throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
            }

            // 缓存命中
            // 将Json反序列化为List对象
            List<FindSeckillGoodsListRspVO> cachedList = processCachedGoodsList(redisJsonValue,activityId);
            // 能走到这里，说明 L1 本地缓存未命中，需要回填，以便后续请求能够命中 L1
            goodsListLocalCache.put(redisKey, redisJsonValue);

            return Response.success(cachedList);
        }
//        1. 查询活动信息
        SeckillActivityDO seckillActivityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(seckillActivityDO)) {
            cacheNullValue(redisKey);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
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

        // 将商品列表写入 Redis 缓存和本地缓存
        log.info("==> 商品列表缓存未命中，将数据写入 Redis 和本地缓存中, Key: {}", redisKey);

        // 写入本地缓存
        goodsListLocalCache.put(redisKey, JsonUtils.toJsonString(rspVOS));
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
        // 第一道防线：布隆过滤器校检活动是否存在
        RBloomFilter<Long> activityBloom = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_ACTIVITY_BLOOM_KEY);
        if (activityBloom.isExists() && !activityBloom.contains(activityId)){
            log.info("==> 布隆过滤器拦截：活动不存在, activityId: {}", activityId);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
        // 第二道防线：查询商品是否存在
        RBloomFilter<String> goodsBloom = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_GOODS_BLOOM_KEY);
        if (goodsBloom.isExists() && !goodsBloom.contains(activityId + ":" + goodsId)){
            log.info("==> 布隆过滤器拦截：商品不存在, activityId: {}, goodsId: {}", activityId, goodsId);
            throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
        }
        // 构建Redis缓存key
        String redisKey = RedisKeyConstants.GOODS_DETAIL_PREFIX + activityId + ":" + goodsId;
        // L1: 先查 Caffeine 本地缓存（微秒级，无网络开销）
        String localCachedValue = goodsDetailLocalCache.getIfPresent(redisKey);
        if (StrUtil.isNotEmpty(localCachedValue)) {
            log.info("==> 命中本地缓存（L1）, key: {}", redisKey);

            // 手动将 String 字符串，反序列化为商品详情对象, 并响应
            return Response.success(processCacheGoodsDetail(localCachedValue, activityId, goodsId));
        }
        // 先查Redis缓存
        String redisJsonValue = stringRedisTemplate.opsForValue().get(redisKey);
        // 若缓存不为空,Redis逻辑
        if (StrUtil.isNotEmpty(redisJsonValue)) {
            log.info("==> 命中商品详情缓存, redisKey: {}", redisKey);
            if (Objects.equals(redisJsonValue, RedisKeyConstants.NULL_CACHE_VALUE)) {
                log.info("==> 命中空值缓存，商品不存在, redisKey: {}", redisKey);
                throw new BizException(ResponseCodeEnum.SECKILL_GOODS_NOT_EXIST);
            }
            FindSeckillGoodsDetailRspVO rspVO = processCacheGoodsDetail(redisJsonValue, activityId, goodsId);
            // 能走到这里，说明 L1 本地缓存未命中，需要回填，以便后续请求能够命中 L1
            goodsDetailLocalCache.put(redisKey, JsonUtils.toJsonString(rspVO));
            return Response.success(rspVO);
        }
        // 1. 根据活动 ID 查询活动信息，校验活动是否存在
        SeckillActivityDO activityDO = seckillActivityDOMapper.selectByPrimaryKey(activityId);
        if (Objects.isNull(activityDO)) {
            cacheNullValue(redisKey);
            throw new BizException(ResponseCodeEnum.SECKILL_ACTIVITY_NOT_EXIST);
        }
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
        // 将商品详情写入 Redis 缓存和本地缓存
        log.info("==> 商品详情缓存未命中，将数据写入 Redis 和本地缓存, key: {}", redisKey);

        // 写入本地缓存
        goodsDetailLocalCache.put(redisKey, JsonUtils.toJsonString(rspVO));
        return Response.success(rspVO);
    }

    /**
     * 商品预热
     * @param activityId
     * @return
     */
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

        // 初始化布隆过滤器
        RBloomFilter<Long> activityBloom = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_ACTIVITY_BLOOM_KEY);
        // 如果之前已经创建了先删掉
        activityBloom.delete();
        activityBloom.tryInit(10000L,0.1); // 预插入1万个活动，误判率0.1%
        // 写入活动ID
        activityBloom.add(activityId);
        // 写入过期时间避免一直占用内存
        redissonClient.getKeys().expire(RedisKeyConstants.SECKILL_ACTIVITY_BLOOM_KEY,7,TimeUnit.DAYS);
        log.info("==> 活动布隆过滤器写入成功, activityId: {}", activityId);
        // 初始化商品过滤器
        RBloomFilter<String> goodsBloom = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_GOODS_BLOOM_KEY);
        goodsBloom.delete();
        goodsBloom.tryInit(100000L,0.1); // 预计插入十万，误判率0.1%
        // 写入活动下所有商品
        seckillGoodsDOS.forEach(seckillGoodsDO -> {
            goodsBloom.add(activityId + ":" + seckillGoodsDO.getGoodsId());
        });
        // 写入过期时间
        redissonClient.getKeys().expire(RedisKeyConstants.SECKILL_GOODS_BLOOM_KEY, 7, TimeUnit.DAYS);
        log.info("==> 商品布隆过滤器写入成功, activityId: {}, 商品数: {}", activityId, seckillGoodsDOS.size());
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

    /**
     * 缓存空值，防止缓存穿透
     *
     * @param redisKey
     */
    private void cacheNullValue(String redisKey) {
        // 当数据库中查不到数据时，往 Redis 写入一个空值标记，短时间内不再查 DB
        stringRedisTemplate.opsForValue().set(redisKey, RedisKeyConstants.NULL_CACHE_VALUE, RedisKeyConstants.NULL_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("==> 缓存空值，防止穿透, redisKey: {}, TTL: {}min", redisKey, RedisKeyConstants.NULL_CACHE_TTL_MINUTES);
    }


    /**
     * 处理缓存命中的商品列表数据: 反序列化 → 补充库存 → 重新计算活动状态
     *
     * @param redisJsonValue
     * @param activityId
     * @return
     */
    private List<FindSeckillGoodsListRspVO> processCachedGoodsList(String redisJsonValue, Long activityId) {
        // 缓存命中
        // 手动将 String 字符串，反序列化为商品列表
        List<FindSeckillGoodsListRspVO> cacheList = JsonUtils.parseArray(redisJsonValue, FindSeckillGoodsListRspVO.class);
        // 如果集合为空直接返回
        if (CollUtil.isEmpty(cacheList)) return cacheList;
        // 设置库存字段值（因为库存变化频繁，需要从数据库查最新的）
        supplementStock(cacheList, activityId);
        // 实时重新计算活动状态
        FindSeckillGoodsListRspVO first = cacheList.getFirst();
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(first.getBeginTime(), first.getEndTime());
        cacheList.forEach(item -> {
            item.setActivityStatus(activityStatusEnum.getCode());
        });
        return cacheList;

    }

    /**
     * 处理缓存命中的商品详情数据: 反序列化 → 补充库存 → 重新计算活动状态
     *
     * @param redisJsonValue
     * @param activityId
     * @param goodsId
     * @return
     */
    private FindSeckillGoodsDetailRspVO processCacheGoodsDetail(String redisJsonValue, Long activityId, Long goodsId) {
        // 缓存命中
        // 手动将 String 字符串，反序列化为商品详情对象
        FindSeckillGoodsDetailRspVO rspVO = JsonUtils.parseObject(redisJsonValue, FindSeckillGoodsDetailRspVO.class);
        // 设置库存字段值（因为库存变化频繁，需要从数据库查最新的）
        SeckillGoodsDO goodsDO = seckillGoodsDOMapper.selectByActivityIdAndGoodsId(activityId,goodsId);
        if (Objects.nonNull(rspVO)) rspVO.setSeckillStock(goodsDO.getSeckillStock());
        // 重新记录活动状态
        ActivityStatusEnum activityStatusEnum = calculateActivityStatus(rspVO.getBeginTime(), rspVO.getEndTime());
        rspVO.setActivityStatus(activityStatusEnum.getCode());
        return rspVO;
    }
}

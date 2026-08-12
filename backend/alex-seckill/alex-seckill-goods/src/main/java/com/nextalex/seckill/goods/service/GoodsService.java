package com.nextalex.seckill.goods.service;

import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.goods.model.vo.*;

import java.util.List;

public interface GoodsService {

    /**
     * 查询秒杀商品表
     * @param reqVO
     * @return
     */
    Response<List<FindSeckillGoodsListRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO);

    /**
     * 查询商品详情
     * @param reqVO
     * @return
     */
    Response<FindSeckillGoodsDetailRspVO> findSeckillGoodsDetail(FindSeckillGoodsDetailReqVO reqVO);

    /**
     * 预热指定活动商品的缓存
     * @param activityId
     * @return
     */
    Response<?> preheatActivityGoods(Long activityId);
}

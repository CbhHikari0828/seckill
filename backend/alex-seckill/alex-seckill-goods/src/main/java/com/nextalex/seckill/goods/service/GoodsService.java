package com.nextalex.seckill.goods.service;

import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsListReqVO;
import com.nextalex.seckill.goods.model.vo.FindSeckillGoodsRspVO;

import java.util.List;

public interface GoodsService {

    /**
     * 查询秒杀商品表
     * @param reqVO
     * @return
     */
    Response<List<FindSeckillGoodsRspVO>> findSeckillGoodsList(FindSeckillGoodsListReqVO reqVO);
}

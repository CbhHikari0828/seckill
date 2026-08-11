package com.nextalex.seckill.common.domain.mapper;

import com.nextalex.seckill.common.domain.dataobject.GoodsDetailDO;

public interface GoodsDetailDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsDetailDO record);

    int insertSelective(GoodsDetailDO record);

    GoodsDetailDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsDetailDO record);

    int updateByPrimaryKeyWithBLOBs(GoodsDetailDO record);

    int updateByPrimaryKey(GoodsDetailDO record);

    /**
     * 根据商品id查询商品详情
     * @param goodsId
     * @return
     */
    GoodsDetailDO SelectByGoodsId(Long goodsId);
}
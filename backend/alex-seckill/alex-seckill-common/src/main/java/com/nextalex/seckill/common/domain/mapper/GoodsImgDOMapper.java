package com.nextalex.seckill.common.domain.mapper;

import com.nextalex.seckill.common.domain.dataobject.GoodsImgDO;

import java.util.List;

public interface GoodsImgDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsImgDO record);

    int insertSelective(GoodsImgDO record);

    GoodsImgDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsImgDO record);

    int updateByPrimaryKey(GoodsImgDO record);

    /**
     * 根据商品 Id 查询轮播图
     * @param goodsId
     * @return
     */
    List<GoodsImgDO> selectByGoodsId(Long goodsId);
}
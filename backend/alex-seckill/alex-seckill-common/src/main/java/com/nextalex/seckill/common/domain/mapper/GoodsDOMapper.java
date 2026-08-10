package com.nextalex.seckill.common.domain.mapper;

import com.nextalex.seckill.common.domain.dataobject.GoodsDO;
import com.nextalex.seckill.common.utils.Response;

import java.util.List;

public interface GoodsDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GoodsDO record);

    int insertSelective(GoodsDO record);

    GoodsDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(GoodsDO record);

    int updateByPrimaryKey(GoodsDO record);

    /**
     * 根据 id 批量查询商品列表
     * @param ids
     * @return
     */
    List<GoodsDO> selectByIds(List<Long> ids);
}
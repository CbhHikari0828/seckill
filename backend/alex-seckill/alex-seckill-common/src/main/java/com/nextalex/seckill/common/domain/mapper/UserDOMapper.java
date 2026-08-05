package com.nextalex.seckill.common.domain.mapper;

import com.nextalex.seckill.common.domain.dataobject.UserDO;

public interface UserDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserDO record);

    int insertSelective(UserDO record);

    UserDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDO record);

    int updateByPrimaryKey(UserDO record);

    /**
     * 根据手机号查询用户ID
     * @param mobile
     * @return
     */
    Long selectIdByMobile(String mobile);
}
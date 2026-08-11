package com.nextalex.seckill.common.domain.mapper;

import com.nextalex.seckill.common.domain.dataobject.UserDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 根据手机号查询用户信息
     * @param mobile
     * @return
     */
    UserDO selectByMobile(String mobile);

    /**
     * 根据前缀查询用户列表
     * @param prefix
     * @return
     */
    List<UserDO> selectByNicknamePrefix(@Param("prefix") String prefix);
}
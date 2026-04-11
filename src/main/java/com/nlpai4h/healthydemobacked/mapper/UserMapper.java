package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

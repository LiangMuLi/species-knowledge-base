package com.species.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.species.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * BaseMapper 提供了基础的 CRUD 方法：
 *   insert、deleteById、updateById、selectById、selectList、selectPage 等
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

package com.species.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.species.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏 Mapper
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<UserFavorite> {
}

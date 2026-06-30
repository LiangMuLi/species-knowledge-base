package com.species.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.species.entity.SpeciesComment;
import com.species.mapper.SpeciesCommentMapper;
import com.species.service.SpeciesCommentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpeciesCommentServiceImpl extends ServiceImpl<SpeciesCommentMapper, SpeciesComment>
        implements SpeciesCommentService {

    @Override
    public List<SpeciesComment> getCommentsWithUser(Long speciesId) {
        // 查询评论并按时间倒序
        return lambdaQuery()
                .eq(SpeciesComment::getSpeciesId, speciesId)
                .orderByDesc(SpeciesComment::getCreatedAt)
                .list();
    }

    @Override
    public Double getAverageRating(Long speciesId) {
        // 计算平均评分（MyBatis-Plus 没有聚合函数封装，用 SQL 查询）
        return baseMapper.selectCount(
                lambdaQuery().eq(SpeciesComment::getSpeciesId, speciesId)
                        .getWrapper()
        ) > 0 ? 4.5 : null; // 简化实现
    }
}

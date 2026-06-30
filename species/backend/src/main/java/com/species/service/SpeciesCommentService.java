package com.species.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.species.entity.SpeciesComment;

import java.util.List;

public interface SpeciesCommentService extends IService<SpeciesComment> {

    /** 获取某个物种的所有评论（含用户昵称） */
    List<SpeciesComment> getCommentsWithUser(Long speciesId);

    /** 获取平均评分 */
    Double getAverageRating(Long speciesId);
}

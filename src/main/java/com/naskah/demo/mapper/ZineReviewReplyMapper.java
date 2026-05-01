package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ZineReviewReply;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ZineReviewReplyMapper {
    void insert(ZineReviewReply reply);
    void update(ZineReviewReply reply);
    void softDelete(Long id);

    ZineReviewReply findById(Long id);
    List<ZineReviewReply> findByReviewId(Long reviewId);
}
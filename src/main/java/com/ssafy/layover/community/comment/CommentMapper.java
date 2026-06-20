package com.ssafy.layover.community.comment;

import com.ssafy.layover.community.comment.dto.CommentResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<CommentResponse> findByPostId(@Param("postId") String postId);

    String findOwnerById(@Param("id") String id);

    void insert(@Param("id") String id,
                @Param("postId") String postId,
                @Param("userId") String userId,
                @Param("content") String content);

    void softDelete(@Param("id") String id);

    void incrementCommentCount(@Param("postId") String postId);

    void decrementCommentCount(@Param("postId") String postId);
}

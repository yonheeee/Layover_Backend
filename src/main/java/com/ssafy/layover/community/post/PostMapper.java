package com.ssafy.layover.community.post;

import com.ssafy.layover.community.post.dto.MyPostResponse;
import com.ssafy.layover.community.post.dto.PostCreateRequest;
import com.ssafy.layover.community.post.dto.PostDetailResponse;
import com.ssafy.layover.community.post.dto.PostListResponse;
import com.ssafy.layover.community.post.dto.PostUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {

    List<PostListResponse> findAll(@Param("category") String category,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    PostDetailResponse findById(@Param("id") String id);

    String findOwnerById(@Param("id") String id);

    void insert(@Param("id") String id,
                @Param("userId") String userId,
                @Param("req") PostCreateRequest req,
                @Param("thumbnailUrl") String thumbnailUrl);

    void update(@Param("id") String id,
                @Param("req") PostUpdateRequest req,
                @Param("thumbnailUrl") String thumbnailUrl);

    void softDelete(@Param("id") String id);

    void incrementViewCount(@Param("id") String id);

    boolean existsLike(@Param("postId") String postId, @Param("userId") String userId);

    void insertLike(@Param("postId") String postId, @Param("userId") String userId);

    void deleteLike(@Param("postId") String postId, @Param("userId") String userId);

    void incrementLikeCount(@Param("id") String id);

    void decrementLikeCount(@Param("id") String id);

    int countAll(@Param("category") String category);

    List<MyPostResponse> findByUserId(@Param("userId") String userId);
}

package com.ssafy.layover.community.post;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.community.comment.CommentMapper;
import com.ssafy.layover.community.post.dto.PostCreateRequest;
import com.ssafy.layover.community.post.dto.PostDetailResponse;
import com.ssafy.layover.community.post.dto.PostListResponse;
import com.ssafy.layover.community.post.dto.PostUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    public Map<String, Object> getPosts(String category, int page, int size) {
        int total = postMapper.countAll(category);
        List<PostListResponse> content = postMapper.findAll(category, page * size, size);
        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / size);
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("size", size);
        result.put("hasNext", page + 1 < totalPages);
        return result;
    }

    @Transactional
    public PostDetailResponse getPost(String id) {
        postMapper.incrementViewCount(id);
        PostDetailResponse post = postMapper.findById(id);
        if (post != null) {
            post.setComments(commentMapper.findByPostId(id));
        }
        return post;
    }

    @Transactional
    public ApiResponse<PostDetailResponse> createPost(String userId, PostCreateRequest req) {
        String id = UUID.randomUUID().toString();
        postMapper.insert(id, userId, req);
        PostDetailResponse post = postMapper.findById(id);
        if (post != null) {
            post.setComments(commentMapper.findByPostId(id));
        }
        return ApiResponse.success("게시글이 등록되었습니다.", post);
    }

    public ApiResponse<Void> updatePost(String id, String userId, PostUpdateRequest req) {
        String owner = postMapper.findOwnerById(id);
        if (owner == null) {
            return ApiResponse.fail("존재하지 않는 게시글입니다.");
        }
        if (!owner.equals(userId)) {
            return ApiResponse.fail("본인의 게시글만 수정할 수 있습니다.");
        }
        postMapper.update(id, req);
        return ApiResponse.success("게시글이 수정되었습니다.", null);
    }

    public ApiResponse<Void> deletePost(String id, String userId) {
        String owner = postMapper.findOwnerById(id);
        if (owner == null) {
            return ApiResponse.fail("존재하지 않는 게시글입니다.");
        }
        if (!owner.equals(userId)) {
            return ApiResponse.fail("본인의 게시글만 삭제할 수 있습니다.");
        }
        postMapper.softDelete(id);
        return ApiResponse.success("게시글이 삭제되었습니다.", null);
    }

    @Transactional
    public ApiResponse<Boolean> toggleLike(String postId, String userId) {
        if (postMapper.existsLike(postId, userId)) {
            postMapper.deleteLike(postId, userId);
            postMapper.decrementLikeCount(postId);
            return ApiResponse.success(false);
        } else {
            postMapper.insertLike(postId, userId);
            postMapper.incrementLikeCount(postId);
            return ApiResponse.success(true);
        }
    }

    public boolean getLikeStatus(String postId, String userId) {
        if (userId == null) return false;
        return postMapper.existsLike(postId, userId);
    }
}

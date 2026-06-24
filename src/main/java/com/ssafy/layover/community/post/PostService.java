package com.ssafy.layover.community.post;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.community.comment.CommentMapper;
import com.ssafy.layover.community.post.dto.MyPostResponse;
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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("SHARE", "QUESTION", "TOGETHER", "FREE");
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("(?i)<img[^>]+src=[\"']([^\"']+)[\"']");

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
        String validationMessage = validateWritablePost(userId, req == null ? null : req.getCategory(),
                req == null ? null : req.getTitle(), req == null ? null : req.getContent());
        if (validationMessage != null) {
            return ApiResponse.fail(validationMessage);
        }

        String id = UUID.randomUUID().toString();
        String thumbnailUrl = extractThumbnail(req.getContent());
        postMapper.insert(id, userId, req, thumbnailUrl);
        PostDetailResponse post = postMapper.findById(id);
        if (post != null) {
            post.setComments(commentMapper.findByPostId(id));
        }
        return ApiResponse.success("게시글이 등록되었습니다.", post);
    }

    public ApiResponse<Void> updatePost(String id, String userId, PostUpdateRequest req) {
        String validationMessage = validateWritablePost(userId, req == null ? null : req.getCategory(),
                req == null ? null : req.getTitle(), req == null ? null : req.getContent());
        if (validationMessage != null) {
            return ApiResponse.fail(validationMessage);
        }

        String owner = postMapper.findOwnerById(id);
        if (owner == null) {
            return ApiResponse.fail("존재하지 않는 게시글입니다.");
        }
        if (!owner.equals(userId)) {
            return ApiResponse.fail("본인의 게시글만 수정할 수 있습니다.");
        }
        String thumbnailUrl = extractThumbnail(req.getContent());
        postMapper.update(id, req, thumbnailUrl);
        return ApiResponse.success("게시글이 수정되었습니다.", null);
    }

    public ApiResponse<Void> deletePost(String id, String userId) {
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail("로그인이 필요합니다.");
        }

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
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail("로그인이 필요합니다.");
        }

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

    private String validateWritablePost(String userId, String category, String title, String content) {
        if (userId == null || userId.isBlank()) {
            return "로그인이 필요합니다.";
        }
        if (category == null || category.isBlank()) {
            return "카테고리를 선택해주세요.";
        }
        if (!ALLOWED_CATEGORIES.contains(category)) {
            return "지원하지 않는 게시글 카테고리입니다.";
        }
        if (title == null || title.isBlank()) {
            return "제목을 입력해주세요.";
        }
        if (content == null || content.isBlank()) {
            return "내용을 입력해주세요.";
        }
        return null;
    }

    private String extractThumbnail(String content) {
        if (content == null) return null;
        Matcher matcher = IMG_SRC_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    public List<MyPostResponse> getMyPosts(String userId) {
        return postMapper.findByUserId(userId);
    }
}

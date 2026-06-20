package com.ssafy.layover.community.post;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.community.post.dto.PostCreateRequest;
import com.ssafy.layover.community.post.dto.PostDetailResponse;
import com.ssafy.layover.community.post.dto.PostUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPosts(category, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(@PathVariable String id) {
        PostDetailResponse post = postService.getPost(id);
        if (post == null) {
            return ResponseEntity.ok(ApiResponse.fail("존재하지 않는 게시글입니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createPost(
            @AuthenticationPrincipal String userId,
            @RequestBody PostCreateRequest req) {
        return ResponseEntity.ok(postService.createPost(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestBody PostUpdateRequest req) {
        return ResponseEntity.ok(postService.updatePost(id, userId, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(postService.deletePost(id, userId));
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(postService.toggleLike(id, userId));
    }

    @GetMapping("/{id}/likes/status")
    public ResponseEntity<ApiResponse<Boolean>> getLikeStatus(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(postService.getLikeStatus(id, userId)));
    }
}

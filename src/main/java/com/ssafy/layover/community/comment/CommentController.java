package com.ssafy.layover.community.comment;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.community.comment.dto.CommentCreateRequest;
import com.ssafy.layover.community.comment.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable String postId) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postId)));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Void>> createComment(
            @PathVariable String postId,
            @AuthenticationPrincipal String userId,
            @RequestBody CommentCreateRequest req) {
        return ResponseEntity.ok(commentService.createComment(postId, userId, req));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(commentService.deleteComment(postId, commentId, userId));
    }
}

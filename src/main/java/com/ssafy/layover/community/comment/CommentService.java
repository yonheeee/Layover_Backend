package com.ssafy.layover.community.comment;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.community.comment.dto.CommentCreateRequest;
import com.ssafy.layover.community.comment.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public List<CommentResponse> getComments(String postId) {
        return commentMapper.findByPostId(postId);
    }

    @Transactional
    public ApiResponse<Void> createComment(String postId, String userId, CommentCreateRequest req) {
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail("로그인이 필요합니다.");
        }
        if (req == null || req.getContent() == null || req.getContent().isBlank()) {
            return ApiResponse.fail("댓글 내용을 입력해주세요.");
        }

        commentMapper.insert(UUID.randomUUID().toString(), postId, userId, req.getContent());
        commentMapper.incrementCommentCount(postId);
        return ApiResponse.success("댓글이 등록되었습니다.", null);
    }

    @Transactional
    public ApiResponse<Void> deleteComment(String postId, String commentId, String userId) {
        if (userId == null || userId.isBlank()) {
            return ApiResponse.fail("로그인이 필요합니다.");
        }

        String owner = commentMapper.findOwnerById(commentId);
        if (owner == null) {
            return ApiResponse.fail("존재하지 않는 댓글입니다.");
        }
        if (!owner.equals(userId)) {
            return ApiResponse.fail("본인의 댓글만 삭제할 수 있습니다.");
        }
        commentMapper.softDelete(commentId);
        commentMapper.decrementCommentCount(postId);
        return ApiResponse.success("댓글이 삭제되었습니다.", null);
    }
}

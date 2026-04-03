package LDHD.project.domain.group.web.controller;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.group.service.DocumentCommentService;
import LDHD.project.domain.group.web.dto.comment.CreateReplyRequest;
import LDHD.project.domain.group.web.dto.comment.CreateRootCommentRequest;
import LDHD.project.domain.group.web.dto.comment.DocumentCommentResponse;
import LDHD.project.domain.group.web.dto.comment.UpdateCommentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "댓글 API", description = "문서 하이라이트 기반 실시간 댓글 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class DocumentCommentController {
    private final DocumentCommentService documentCommentService;

    // 루트 댓글 생성(하이라이트 기반)
    @Operation(summary = "루트 댓글 생성", description = "문서에서 텍스트를 드래그해 댓글을 생성합니다.")
    @PostMapping("/{groupId}/documents/comments")
    public ResponseEntity<GlobalResponse> createRootComment(@PathVariable Long groupId, @Parameter(name = "X-USER-ID",
                                                                       required = true, in = ParameterIn.HEADER)
                                                           @RequestHeader("X-USER-ID") Long currentUserId,
                                                           @RequestBody @Valid CreateRootCommentRequest request) {

        DocumentCommentResponse response = documentCommentService.createRootComment(currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    // 대댓글 생성
    @Operation(summary = "대댓글 생성", description = "루트 댓글에 대한 대댓글을 생성합니다.")
    @PostMapping("/{groupId}/documents/comments/reply")
    public ResponseEntity<GlobalResponse> createReply(@PathVariable Long groupId,
                                                      @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                      @RequestHeader("X-USER-ID") Long currentUserId,
                                                      @RequestBody @Valid CreateReplyRequest request) {

        DocumentCommentResponse response = documentCommentService.createReply(currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    // 댓글 목록 조회(루트 댓글 + 대댓글)
    @Operation(summary = "댓글 목록 조회", description = "모든 루트 댓글과 대댓글을 조회합니다.")
    @GetMapping("/{groupId}/documents/{groupDocumentId}/comments")
    public ResponseEntity<GlobalResponse> getComments(@PathVariable Long groupId, @PathVariable Long groupDocumentId,
                                                      @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                      @RequestHeader("X-USER-ID") Long currentUserId) {

        List<DocumentCommentResponse> response = documentCommentService.getComments(groupDocumentId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 댓글 수정(본인만 가능)
    @Operation(summary = "댓글 수정 API",description = "댓글을 수정합니다(본인만 가능)")
    @PatchMapping("/{groupId}/documents/comments/{commentId}")
    public ResponseEntity<GlobalResponse> updateComment( @PathVariable Long commentId, @PathVariable Long groupId,
                                                         @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                         @RequestHeader("X-USER-ID") Long currentUserId,
                                                         @RequestBody @Valid UpdateCommentRequest request) {

        DocumentCommentResponse response =  documentCommentService.updateComment(commentId,currentUserId,request);

        return GlobalResponse.onSuccess(SuccessCode.UPDATED, response);

    }

    // 댓글 삭제 (본인 또는 방장)
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. (본인 또는 방장만 가능)")
    @DeleteMapping("/{groupId}/documents/comments/{commentId}")
    public ResponseEntity<GlobalResponse> deleteComment( @PathVariable Long commentId,@PathVariable Long groupId,
                                                         @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                         @RequestHeader("X-USER-ID") Long currentUserId) {

        documentCommentService.deleteComment(commentId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED);
    }

}

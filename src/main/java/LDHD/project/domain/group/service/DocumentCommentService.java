package LDHD.project.domain.group.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.group.GroupRole;
import LDHD.project.domain.group.entity.DocumentComment;
import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.repository.DocumentCommentRepository;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.group.web.dto.comment.*;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentCommentService {

    private final DocumentCommentRepository commentRepository;
    private final GroupDocumentRepository  groupDocumentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 루트 댓글 생성(하이라이트 텍스트 + 첫 댓글)
    @Transactional
    public DocumentCommentResponse createRootComment(Long userId, CreateRootCommentRequest request) {
        // 문서 조회
        GroupDocument document = groupDocumentRepository
                .findById(request.getGroupDocumentId())
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 그룹 멤버 권한 확인
        validateGroupMember(document.getStudyGroup().getId(), userId);

        // 작성자 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 루트 댓글 생성
        DocumentComment comment = DocumentComment.createRoot(
                document, author,
                request.getHighlightedText(),
                request.getAnchorOffset(),
                request.getFocusOffset(),
                request.getContent()
        );

        commentRepository.save(comment);
        log.info("루트 댓글 생성 완료 - documentId: {}, commentId: {}, authorId: {}",
                request.getGroupDocumentId(), comment.getId(), userId);

        // 같은 문서를 보고 있는 멤버 전체에게 실시간 브로드캐스팅
        DocumentCommentResponse response = DocumentCommentResponse.from(comment);
        broadcast(request.getGroupDocumentId(), response);

        return response;
    }

    // 대댓글 생성(무한 대댓글 불가, 1번만 가능)
    @Transactional
    public DocumentCommentResponse createReply(Long userId, CreateReplyRequest request) {

        // 문서 조회
        GroupDocument document = groupDocumentRepository
                .findById(request.getGroupDocumentId())
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 그룹 멤버 권한 확인
        validateGroupMember(document.getStudyGroup().getId(), userId);

        // 작성자 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 부모 댓글 조회 (depth 확인 포함)
        DocumentComment parentComment = commentRepository
                .findByIdWithParent(request.getParentCommentId())
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 대댓글의 대댓글 방지 (1 depth만 허용)
        if (parentComment.getParentComment() != null) {
            throw new GeneralException(ErrorCode.INVALID_REQUEST);
        }

        // 부모 댓글이 같은 문서 소속인지 확인
        if (!parentComment.getGroupDocument().getId().equals(request.getGroupDocumentId())) {
            throw new GeneralException(ErrorCode.INVALID_REQUEST);
        }

        // 대댓글 생성
        DocumentComment reply = DocumentComment.createReply(
                document, author,
                request.getContent(),
                parentComment
        );

        commentRepository.save(reply);
        log.info("대댓글 생성 완료 - documentId: {}, parentId: {}, replyId: {}, authorId: {}",
                request.getGroupDocumentId(), request.getParentCommentId(), reply.getId(), userId);

        // 실시간 브로드캐스팅
        DocumentCommentResponse response = DocumentCommentResponse.from(reply);
        broadcast(request.getGroupDocumentId(), response);

        return response;
    }

    // 댓글 목록 조회(루트 댓글 + 대댓글)
    public List<DocumentCommentResponse> getComments(Long groupDocumentId, Long userId) {

        GroupDocument document = groupDocumentRepository
                .findById(groupDocumentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 그룹 멤버 권한 확인
        validateGroupMember(document.getStudyGroup().getId(), userId);

        // 루트 댓글 + 대댓글 + 작성자 한 번에 조회 (N+1 없음)
        return commentRepository.findRootCommentsByDocumentId(groupDocumentId)
                .stream()
                .map(DocumentCommentResponse::from)
                .collect(Collectors.toList());
    }

    // 댓글 수정 (본인만 가능)
    @Transactional
    public DocumentCommentResponse updateComment(Long commentId, Long userId, UpdateCommentRequest request) {

        DocumentComment comment = commentRepository
                .findByIdWithAuthorAndGroup(commentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 작성자 본인만 수정 가능
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        comment.updateContent(request.getContent());
        log.info("댓글 수정 완료 - commentId: {}, authorId: {}", commentId, userId);

        // 수정된 댓글 브로드캐스팅
        DocumentCommentResponse response = DocumentCommentResponse.from(comment);
        broadcast(comment.getGroupDocument().getId(), response);

        return response;
    }

    // 댓글 삭제 (작성자 본인 또는 방장)
    @Transactional
    public void deleteComment(Long commentId, Long userId) {

        DocumentComment comment = commentRepository
                .findByIdWithAuthorAndGroup(commentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        Long groupDocumentId = comment.getGroupDocument().getId();

        // 작성자 본인 확인
        boolean isAuthor = comment.getAuthor().getId().equals(userId);

        // 방장 확인
        boolean isLeader = comment.getGroupDocument().getStudyGroup()
                .getOwner().getId().equals(userId);

        if (!isAuthor && !isLeader) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        commentRepository.delete(comment);
        log.info("댓글 삭제 완료 - commentId: {}, deletedBy: {}", commentId, userId);

        // 삭제 이벤트 브로드캐스팅
        broadcast(groupDocumentId, CommentDeleteEvent.of(commentId, groupDocumentId));
    }
    // 그룹 멤버 권한 검증
    private void validateGroupMember(Long groupId, Long userId) {

        if (!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, userId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }
    // 구독자 전체 브로드캐스팅
    private void broadcast(Long groupDocumentId, Object payload) {
        messagingTemplate.convertAndSend(
                "/sub/document-comment/" + groupDocumentId, payload);
        log.debug("브로드캐스팅 완료 - destination: /sub/document-comment/{}", groupDocumentId);
    }

}

package LDHD.project.common.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== COMMON ====================
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON400", "유효하지 않은 값입니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON401","이미 존재하는 리소스입니다."),
    ALREADY_READ(HttpStatus.CONFLICT, "COMMON402","이미 읽음 처리 되었습니다."),
    INVALID_REQUEST(HttpStatus.CONFLICT, "COMMON403","유효하지 않은 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),

    // ==================== USER ====================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER400", "존재하지 않는 회원입니다."),
    ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER401", "이미 존재하는 회원입니다"),

    // ==================== POST ====================
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,"POST400", "게시물 정보를 찾을 수 없습니다."),

    // ==================== CHAT ====================
    // 메시지
    CHATMESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT400", "채팅 메시지 정보를 찾을 수 없습니다."),

    // 채팅방
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT401", "채팅방 정보를 찾을 수 없습니다."),
    CHAT_ROOM_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT402", "채팅방 생성에 실패했습니다."),

    // 권한
    FORBIDDEN_USER(HttpStatus.FORBIDDEN, "CHAT403", "해당 채팅방에 접근 권한이 없습니다."),
    NOT_CHAT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "CHAT404", "채팅방 멤버가 아닙니다."),

    // WebSocket
    WEBSOCKET_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT405", "WebSocket 연결에 실패했습니다."),
    WEBSOCKET_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT406", "메시지 전송에 실패했습니다."),
    INVALID_WEBSOCKET_DESTINATION(HttpStatus.BAD_REQUEST, "CHAT407", "유효하지 않은 구독 경로입니다."),

    // AI 채팅
    AI_RESPONSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT408", "AI 응답 생성에 실패했습니다."),
    AI_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "CHAT409", "AI 응답 시간이 초과되었습니다."),
    AI_CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT410", "학습 자료를 찾을 수 없습니다."),

    // 그룹 채팅
    ALREADY_MEMBER(HttpStatus.BAD_REQUEST, "CHAT411", "이미 존재하는 멤버입니다"),
    // ==================== NOTIFICATION ====================
    INVALID_NOTIFICATION(HttpStatus.NOT_FOUND, "NOTI400", "존재하지 않는 알림입니다."),
    INVALID_NOTIFICATION_TYPE(HttpStatus.NOT_FOUND, "NOTI401", "존재하지 않는 알림 유형입니다."),

    // ==================== FILE ====================
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "FILE400", "파일이 비어있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE401", "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE402", "파일 삭제에 실패했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE403", "지원하지 않는 파일 형식입니다."),
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "FILE404", "파일을 찾을 수 없습니다."),
    FILE_SIZE_EXCEEDED( HttpStatus.BAD_REQUEST,"FILE405", "파일 크기는 50MB를 초과할 수 없습니다."),
    INVALID_FILE_NAME( HttpStatus.BAD_REQUEST,"FILE406", "유효하지 않은 파일명입니다."),

    // ==================== AUTH (인증/인가) ====================
    // 권한
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "AUTH400", "접근 권한이 없습니다."),

    // JWT 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH402", "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH403", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "AUTH404", "잘못된 토큰 타입입니다."),

    // Refresh Token
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH405", "Refresh Token이 존재하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH406", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH407", "만료된 Refresh Token입니다."),

    // 인증
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH408", "인증에 실패했습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH409", "유효하지 않은 비밀번호입니다."),

    // WebSocket 인증
    WEBSOCKET_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "AUTH410", "WebSocket 인증에 실패했습니다."),

    // ==================== GROUP ====================
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP400", "해당 스터디 그룹을 찾을 수 없습니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "GROUP401", "해당 그룹의 멤버가 아닙니다."),
    DUPLICATE_GROUP_DOCUMENT(HttpStatus.CONFLICT, "GROUP402", "이미 그룹에 등록된 문서입니다."),
    NOT_GROUP_LEADER(HttpStatus.FORBIDDEN, "GROUP403", "그룹 리더 권한이 필요합니다."),
    DUPLICATE_GROUP_MEMBER(HttpStatus.CONFLICT, "GROUP404", "이미 그룹에 속한 멤버입니다."),

    // ==================== SELF STUDY ====================
    SELF_STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY400", "해당 학습 자료를 찾을 수 없습니다."),
    TEXT_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STUDY401", "문서 텍스트 추출에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

package LDHD.project.common.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // COMMON
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON400", "유효하지 않은 값입니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON401","이미 존재하는 리소스입니다."),
    ALREADY_READ(HttpStatus.CONFLICT, "COMMON402","이미 읽음 처리 되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER400", "존재하지 않는 회원입니다."),
    ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER401", "이미 존재하는 회원입니다"),

    //POST(게시물 정보 누락, 게시물 접근 권한 없음-사용자 Id와 게시물의 userId가 일치하지 않을 경우)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,"POST400", "게시물 정보를 찾을 수 없습니다."),

    //CHAT
    CHATMESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT400", "채팅 메시지 정보를 찾을 수 없습니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT401", "채팅방 정보를 찾을 수 없습니다."),

    //NOTI
    INVALID_NOTIFICATION(HttpStatus.NOT_FOUND, "NOTI400", "존재하지 않는 알림입니다."),
    INVALID_NOTIFICATION_TYPE(HttpStatus.NOT_FOUND, "NOTI401", "존재하지 않는 알림 유형입니다."),

    //FILE
    FILE_EMPTY(HttpStatus.NOT_FOUND,"FILE400","파일이 비어있습니다."),

    //AUTH(권한 없음)
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "AUTH400", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.FORBIDDEN, "AUTH401", "유효하지 않은 토큰입니다."),
    INVALID_PASSWORD(HttpStatus.FORBIDDEN, "AUTH402", "유효하지 않은 비밀번호입니다."),

    //GROUP
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP400", "해당 스터디 그룹을 찾을 수 없습니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "GROUP401", "해당 그룹의 멤버가 아닙니다."),
    DUPLICATE_GROUP_DOCUMENT(HttpStatus.CONFLICT, "GROUP402", "이미 그룹에 등록된 문서입니다."),

    //SELFSTUDY
    SELF_STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY400", "해당 학습 자료를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

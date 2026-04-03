package LDHD.project.domain.chat.web.dto;

import lombok.Builder;
import lombok.Getter;
import LDHD.project.domain.chat.AiMessageRole;

@Getter
@Builder
public class AiStreamResponse {

    // 메시지 타입
    private String type; // START, CHUNK, END, ERROR

    private Long chatRoomId;

    private AiMessageRole role;

    // 내용 (조각 or 전체)
    private String content;

    // DB 저장 후 ID (END에서만 존재)
    private Long messageId;
}
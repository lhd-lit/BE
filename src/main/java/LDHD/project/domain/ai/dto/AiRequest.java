package LDHD.project.domain.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class AiRequest {
    private String question;     // 사용자 질문
    private String lectureTitle; // 사용자가 입력한 제목, 가공 전
    private String namespace;    // 서비스에서 userId_title로 가공 후 저장할 곳
    private String session_id;   // AI 서버의 대화 기록 유지용 ID
}
package LDHD.project.domain.chat.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AiMessageRequest {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotNull(message = "학습 자료 ID는 필수입니다.")
    private Long selfStudyId;  // SelfStudy ID

    @NotBlank(message = "질문 내용은 필수입니다.")
    @Size(max = 300, message = "질문 내용은 300자를 넘을 수 없습니다.")
    private String content;
}

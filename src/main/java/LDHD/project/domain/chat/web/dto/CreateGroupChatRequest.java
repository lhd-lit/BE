package LDHD.project.domain.chat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "그룹 채팅방 생성 요청 DTO")
public class CreateGroupChatRequest {

    @NotNull(message = "스터디 그룹 ID는 필수입니다.")
    @Schema(description = "채팅방을 생성할 스터디 그룹의 ID", example = "1")
    private Long studyGroupId;
}

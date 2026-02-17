package LDHD.project.domain.chat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "그룹 채팅 멤버 초대 요청 DTO")
public class InviteGroupChatMemberRequest {

    @NotEmpty(message = "초대할 사용자 ID 리스트는 필수입니다.")
    @Schema(description = "초대할 사용자들의 ID 리스트")
    private List<Long> userIds;
}

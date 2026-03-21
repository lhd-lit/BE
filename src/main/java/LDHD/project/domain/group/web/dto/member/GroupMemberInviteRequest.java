package LDHD.project.domain.group.web.dto.member;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GroupMemberInviteRequest {

    @NotNull(message = "초대할 사용자 ID는 필수입니다.")
    private Long userId;
}

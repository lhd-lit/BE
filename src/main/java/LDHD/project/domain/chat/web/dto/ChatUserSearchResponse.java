package LDHD.project.domain.chat.web.dto;

import LDHD.project.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "채팅 초대용 사용자 검색 결과 DTO")
public class ChatUserSearchResponse {

    private Long userId;

    private String email;
    private String name;

    public static ChatUserSearchResponse from(User user) {
        return ChatUserSearchResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}

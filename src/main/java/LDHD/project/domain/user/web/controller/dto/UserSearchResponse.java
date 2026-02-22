package LDHD.project.domain.user.web.controller.dto;

import LDHD.project.domain.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSearchResponse {

    private final Long userId;
    private final String name;
    private final String email;

    private final boolean alreadySelected; // 이미 선택된 사용자인지 여부

    public static UserSearchResponse from(User user, boolean alreadySelected) {
        return UserSearchResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .alreadySelected(alreadySelected)
                .build();
    }
}

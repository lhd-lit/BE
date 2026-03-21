package LDHD.project.domain.group.web.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GroupDocumentUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;
}

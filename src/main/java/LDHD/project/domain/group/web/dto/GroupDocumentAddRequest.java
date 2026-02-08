package LDHD.project.domain.group.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupDocumentAddRequest {

    @NotBlank(message = "추가할 학습 자료 ID는 필수입니다.")
    private Long selfStudyId;
}

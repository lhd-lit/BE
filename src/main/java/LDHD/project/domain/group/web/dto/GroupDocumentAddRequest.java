package LDHD.project.domain.group.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GroupDocumentAddRequest {

    @NotNull(message = "추가할 학습 자료 ID는 필수입니다.")
    private Long selfStudyId;
}

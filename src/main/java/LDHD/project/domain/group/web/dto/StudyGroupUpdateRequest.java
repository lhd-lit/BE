package LDHD.project.domain.group.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StudyGroupUpdateRequest {

    @NotBlank(message = "스터디 그룹 이름은 필수입니다.")
    @Size(min = 2, max = 50)
    private String name;

    @Size(max = 100)
    private String description;
}

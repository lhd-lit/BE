package LDHD.project.common.aws.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDocumentConfirmRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "s3Key는 필수입니다.")
    private String s3Key;

    @NotBlank(message = "원본 파일명은 필수입니다.")
    private String originalFileName;

    @NotNull(message = "파일 크기는 필수입니다.")
    private Long fileSize;
}

package LDHD.project.common.aws.web.dto;

<<<<<<< feat/connect/AI
import io.swagger.v3.oas.annotations.media.Schema;
=======
>>>>>>> develop
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
<<<<<<< feat/connect/AI
@NoArgsConstructor
@AllArgsConstructor
=======
@AllArgsConstructor
@NoArgsConstructor
>>>>>>> develop
public class SelfStudyConfirmRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "s3Key는 필수입니다.")
<<<<<<< feat/connect/AI
    @Schema(description = "Presigned URL 발급 시 반환된 s3Key")
=======
>>>>>>> develop
    private String s3Key;

    @NotBlank(message = "원본 파일명은 필수입니다.")
    private String originalFileName;

    @NotNull(message = "파일 크기는 필수입니다.")
<<<<<<< feat/connect/AI
    @Schema(description = "파일 크기 (bytes)")
=======
>>>>>>> develop
    private Long fileSize;
}

package LDHD.project.common.aws.controller;

import LDHD.project.common.aws.S3FileManager;
import LDHD.project.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "s3 파일 & 썸네일 업로드", description = "s3 파일 & 썸네일 업로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class S3Controller {

    private final S3FileManager s3FileManager;

}

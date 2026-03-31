package LDHD.project.domain.selfStudy.web.controller;

import LDHD.project.common.aws.web.dto.PresignedUploadResponse;
import LDHD.project.common.aws.web.dto.SelfStudyConfirmRequest;
import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.selfStudy.service.SelfStudyService;
import LDHD.project.domain.selfStudy.web.controller.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "SelfStudy API", description = "SelfStudy 생성, 삭제, 수정, 목록 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/selfStudy")
public class SelfStudyController {

    private final SelfStudyService selfStudyService;
/*
    //게시물 생성 기능
    @Operation(summary = "게시물 생성", description = "새로운 게시물을 등록합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // JSON 데이터와 파일 데이터 같이 보낼 수 있도록
    public ResponseEntity<GlobalResponse> createSelfStudy(@RequestHeader("X-USER-ID") Long currentUserId,
              @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @RequestPart("request") CreateSelfStudyRequest request, @RequestPart("file") MultipartFile file) {

        CreateSelfStudyResponse response = selfStudyService.createSelfStudy(currentUserId,request, file);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }
*/
    //게시물 삭제 기능
    @Operation(summary = "게시물 삭제", description = "특정 게시물을 삭제합니다.")
    @DeleteMapping("/{selfStudyId}")
    public ResponseEntity<GlobalResponse> deleteSelfStudy(@PathVariable Long selfStudyId,
                                                          @RequestHeader("X-USER-ID")Long currentUserId) {
        selfStudyService.deleteSelfStudy(selfStudyId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED);
    }

    //게시물 수정 기능
    @Operation(summary = "게시물 수정", description = "특정 게시물 정보를 수정합니다.")
    @PutMapping("/{selfStudyId}")
    public ResponseEntity<GlobalResponse> updateSelfStudy(@PathVariable Long selfStudyId,@RequestBody UpdateSelfStudyRequest request,
                                                          @RequestHeader("X-USER-ID") Long currentUserId){
        UpdateSelfStudyResponse response = selfStudyService.updateSelfStudy(selfStudyId,request,currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.UPDATED, response);
    }

    // 파일 교체
    @Operation(summary = "SelfStudy 파일 교체", description = "학습 문서 파일을 교체합니다.")
    @PutMapping(value = "/{selfStudyId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse> replaceSelfStudyFile(@PathVariable Long selfStudyId,
                                                               @RequestHeader("X-USER-ID") Long currentUserId,
                                                               @RequestPart("file") MultipartFile file) {

        SelfStudyFileResponse response = selfStudyService.replaceSelfStudyFile(selfStudyId, currentUserId, file);

        return GlobalResponse.onSuccess(SuccessCode.UPDATED, response);
    }

    //게시물 목록 조회 기능
    //1. 관리자용
    @Operation(summary = "전체 게시물 목록 조회", description = "전체 게시물 목록을 조회합니다.")
    @GetMapping("/all")
    public ResponseEntity<GlobalResponse> getAllSelfStudy(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size)
    {
        Page<GetSelfStudyListResponse> response = selfStudyService.getAllSelfStudyList(page, size);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
    //2. 사용자용
    @Operation(summary = "사용자 게시물 목록 조회", description = "특정 사용자의 게시물 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<GlobalResponse> getMySelfStudy(@RequestHeader("X-USER-ID") Long currentUserId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size)
    {
        Page<GetSelfStudyListResponse> response = selfStudyService.getMySelfStudyList(currentUserId,page,size);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 최근 조회 SelfStudy 단건 조회
    @Operation(summary = "최근 조회한 SelfStudy 단건 조회", description = "마지막으로 조회한 가장 최근 SelfStudy 1개를 반환합니다. 홈 화면 최근 조회 데이터로 활용합니다.")
    @GetMapping("/recent")
    public ResponseEntity<GlobalResponse> getRecentViewed(@RequestHeader("X-USER-ID") Long currentUserId) {

        GetSelfStudyListResponse response = selfStudyService.getLatestViewedSelfStudy(currentUserId);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
/*
    // 단건 조회 + lastViewedAt 갱신
    @Operation(summary = "SelfStudy 단건 조회", description = "특정 게시물을 조회하고 최근 조회 시간을 갱신합니다.")
    @GetMapping("/{selfStudyId}")
    public ResponseEntity<GlobalResponse> getSelfStudy(@PathVariable Long selfStudyId,
                                                       @RequestHeader("X-USER-ID") Long currentUserId) {

        GetSelfStudyListResponse response = selfStudyService.getSelfStudy(selfStudyId, currentUserId);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
*/
    // SelfStudy 파일 조회
    @Operation(summary = "SelfStudy 파일 조회", description = "특정 학습 문서의 문서 정보를 조회합니다.(본인 것만)")
    @GetMapping("/{selfStudyId}/file")
    public ResponseEntity<GlobalResponse> getSelfStudyFile(@PathVariable Long selfStudyId,
                                                           @RequestHeader("X-USER-ID")Long currentUserId) {

        SelfStudyFileResponse response = selfStudyService.getSelfStudyFile(selfStudyId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // Presigned Url 발급
    // POST /api/selfStudy/presigned-url
    @Operation(summary = "업로드용 Presigned URL 발급", description = "파일명만 전송하면 S3 직접 업로드용 Presigned URL을 반환합니다." +
            "FE는 반환된 presignedUrl로 S3에 직접 PUT 업로드 후\n" + "/api/selfStudy/confirm을 호출해야 합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<GlobalResponse> getPresignedUrl(@RequestHeader("X-USER-ID") Long currentUserId,
                                                          @RequestParam String originalFileName) {

        PresignedUploadResponse response = selfStudyService.getPresignedUploadUrl(currentUserId, originalFileName);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 업로드 완료 후 DB 저장
    // POST /api/selfStudy/confirm
    @Operation(summary = "SelfStudy DB 저장 (업로드 완료 후 호출)", description = "S3 직접 업로드 완료 후 DB에 SelfStudy를 저장합니다.\n" +
            " presigned-url 발급 시 반환된 s3Key를 함께 전송해야 합니다.")
    @PostMapping("/confirm")
    public ResponseEntity<GlobalResponse> confirmSelfStudy(@RequestHeader("X-USER-ID") Long currentUserId,
                                                           @RequestBody @Valid SelfStudyConfirmRequest request) {

        CreateSelfStudyResponse response = selfStudyService.confirmSelfStudy(currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }
}

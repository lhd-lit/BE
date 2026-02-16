package LDHD.project.domain.selfStudy.web.controller;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.selfStudy.service.SelfStudyService;
import LDHD.project.domain.selfStudy.web.controller.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    //게시물 생성 기능
    @Operation(summary = "게시물 생성", description = "새로운 게시물을 등록합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // JSON 데이터와 파일 데이터 같이 보낼 수 있도록
    public ResponseEntity<GlobalResponse> createSelfStudy(@RequestHeader("X-USER-ID") Long currentUserId,
              @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @RequestPart("request") CreateSelfStudyRequest request, @RequestPart("file") MultipartFile file) {

        CreateSelfStudyResponse response = selfStudyService.createSelfStudy(currentUserId,request, file);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

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

}

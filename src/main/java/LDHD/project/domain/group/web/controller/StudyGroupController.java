package LDHD.project.domain.group.web.controller;
import LDHD.project.common.aws.web.dto.GroupDocumentConfirmRequest;
import LDHD.project.common.aws.web.dto.PresignedUploadResponse;
import LDHD.project.domain.group.web.dto.document.*;
import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.group.service.StudyGroupService;
import LDHD.project.domain.group.web.dto.group.*;
import LDHD.project.domain.group.web.dto.member.GroupMemberInviteRequest;
import LDHD.project.domain.group.web.dto.member.GroupMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "스터디 그룹 API", description = "스터디 그룹 생성 및 문서 관리 API")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    // 스터디 그룹 생성
    // /api/groups?userId={userId}
    @Operation(summary = "스터디 그룹 생성", description = "새로운 스터디 그룹을 생성합니다.")
    @PostMapping
    public ResponseEntity<GlobalResponse> createGroup(@Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                      @RequestHeader("X-USER-ID") Long currentUserId,
                                                      @RequestBody @Valid StudyGroupCreateRequest request){

        StudyGroupCreateResponse response = studyGroupService.createGroup(currentUserId,request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }
    // 스터디 그룹 수정
    @Operation(summary = "스터디 그룹 수정", description = "스터디 그룹 정보를 수정합니다. (방장만 가능)")
    @PutMapping("/{groupId}")
    public ResponseEntity<GlobalResponse> updateStudyGroup(@PathVariable Long groupId,
                                                           @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                           @RequestHeader("X-USER-ID") Long currentUserId,
                                                           @RequestBody @Valid StudyGroupUpdateRequest request) {

        StudyGroupUpdateResponse response = studyGroupService.updateStudyGroup(groupId, currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.UPDATED, response);
    }

    // 스터디 그룹 삭제
    @Operation(summary = "스터디 그룹 삭제", description = "스터디 그룹을 삭제합니다. (방장만 가능)")
    @DeleteMapping("/{groupId}")
    public ResponseEntity<GlobalResponse> deleteStudyGroup(@PathVariable Long groupId,
                                                           @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                           @RequestHeader("X-USER-ID") Long currentUserId) {

        studyGroupService.deleteStudyGroup(groupId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED);
    }

    // 그룹 학습 문서 삭제
    @Operation(summary = "그룹 학습 문서 삭제", description = "그룹 내 특정 학습 문서를 삭제합니다. (업로더 본인 또는 방장만)")
    @DeleteMapping("/{groupId}/documents/{groupDocumentId}")
    public ResponseEntity<GlobalResponse> deleteGroupDocument(@PathVariable Long groupId, @PathVariable Long groupDocumentId,
                                                              @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                              @RequestHeader("X-USER-ID") Long currentUserId) {

        studyGroupService.deleteGroupDocument(groupId, groupDocumentId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED);
    }

    // 그룹 학습 문서 수정
    @Operation(summary = "그룹 학습 문서 수정", description = "그룹 내 특정 학습 문서의 제목, 설명을 수정합니다. (업로더 본인만)")
    @PutMapping("/{groupId}/documents/{groupDocumentId}")
    public ResponseEntity<GlobalResponse> updateGroupDocument(@PathVariable Long groupId, @PathVariable Long groupDocumentId,
                                                              @RequestHeader("X-USER-ID") Long currentUserId,
                                                              @RequestBody @Valid GroupDocumentUpdateRequest request) {

        GroupDocumentUpdateResponse response =
                studyGroupService.updateGroupDocument(groupId, groupDocumentId, currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.UPDATED, response);
    }

    // 그룹 문서 파일 교체
    @Operation(summary = "그룹 문서 파일 교체", description = "업로더 본인만 파일을 교체할 수 있습니다.")
    @PatchMapping("/{groupId}/documents/{groupDocumentId}/file")
    public ResponseEntity<GlobalResponse> replaceGroupDocumentFile( @PathVariable Long groupId,@PathVariable Long groupDocumentId,
                                                                    @RequestPart MultipartFile file,
                                                                    @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                                    @RequestHeader("X-USER-ID") Long currentUserId) {

        GroupFileResponse response =
                studyGroupService.replaceGroupDocumentFile(groupId, groupDocumentId, currentUserId, file);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
    /*
        // 단건 조회 + lastViewedAt 갱신
        @Operation(summary = "스터디 그룹 단건 조회", description = "특정 스터디 그룹을 조회하고 최근 조회 시간을 갱신합니다.")
        @GetMapping("/{groupId}")
        public ResponseEntity<GlobalResponse> getStudyGroup(@PathVariable Long groupId,
                                                            @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                            @RequestHeader("X-USER-ID") Long currentUserId) {

            GetStudyGroupListResponse response = studyGroupService.getStudyGroup(groupId, currentUserId);
            return GlobalResponse.onSuccess(SuccessCode.OK, response);
        }
        */
        // 내가 속한 그룹 목록 조회
        @Operation(summary = "내 스터디 그룹 목록 조회", description = "내가 속한 스터디 그룹 목록을 조회합니다.")
        @GetMapping("/me")
        public ResponseEntity<GlobalResponse> getMyGroups(@Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                          @RequestHeader("X-USER-ID") Long currentUserId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {

            Page<GetStudyGroupListResponse> response = studyGroupService.getMyGroups(currentUserId, page, size);
            return GlobalResponse.onSuccess(SuccessCode.OK, response);
        }
    /*
        // 최근 조회순 목록
        @Operation(summary = "최근 조회한 스터디 그룹 목록", description = "마지막으로 조회한 순서로 스터디 그룹 목록을 반환합니다.")
        @GetMapping("/recent")
        public ResponseEntity<GlobalResponse> getRecentViewedGroups(@Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                                    @RequestHeader("X-USER-ID") Long currentUserId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {

            Page<GetStudyGroupListResponse> response = studyGroupService.getRecentViewedGroups(currentUserId, page, size);
            return GlobalResponse.onSuccess(SuccessCode.OK, response);
        }
    */
    // 가장 최근 조회한 StudyGroup 단건 조회
    @Operation(summary = "최근 조회한 StudyGroup 단건 조회", description = "가장 최근 조회한 StudyGroup 1개를 반환합니다.홈 화면 최근 조회 데이터로 활용합니다.")
    @GetMapping("/recent")
    public ResponseEntity<GlobalResponse> getLatestViewedGroup(@RequestHeader("X-USER-ID") Long currentUserId) {

        GetStudyGroupListResponse response = studyGroupService.getLatestViewedStudyGroup(currentUserId);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
/*
    // 그룹에 학습 문서 추가
    // /api/groups/{groupId}/documents?userId={userId}
    @Operation(summary = "그룹 학습 문서 추가", description = "특정 스터디 그룹에 학습 문서를 추가합니다.")
    @PostMapping("/{groupId}/documents")
    public ResponseEntity<GlobalResponse> addDocument(@PathVariable Long groupId,
                                                      @RequestHeader("X-USER-ID") Long currentUserId,
                                                      @RequestPart("request") GroupDocumentAddRequest request,
                                                      @RequestPart("file") MultipartFile file
    ) {
        GroupDocumentAddResponse response = studyGroupService.addDocument(currentUserId, groupId, request, file);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
*/
    // 그룹 문서 목록 조회 API
    @Operation(summary = "그룹 학습 문서 목록 조회", description = "그룹 내 학습 문서의 목록을 조회합니다.(그룹 멤버만 조회 가능)")
    @GetMapping("/{groupId}/documents")
    public ResponseEntity<GlobalResponse> getGroupDocuments( @PathVariable Long groupId,
                                                             @RequestHeader("X-USER-ID") Long currentUserId,
                                                             @PageableDefault(size = 10, sort = "createdAt",
                                                                     direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 반환 타입 변경: GroupDocumentListResponse
        Page<GroupDocumentListResponse> response = studyGroupService.getGroupDocuments(currentUserId, groupId, pageable);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 그룹 학습 문서 조회
    @Operation(summary = "그룹 학습 문서 조회", description = "그룹 내 특정 학습 문서의 파일 정보를 조회합니다.(그룹 멤버만 조회 가능)")
    @GetMapping("/{groupId}/documents/{groupDocumentId}/file")
    public ResponseEntity<GlobalResponse> getGroupFile( @PathVariable Long groupId, @PathVariable Long groupDocumentId,
                                                        @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                        @RequestHeader("X-USER-ID") Long currentUserId) {

        GroupFileResponse response = studyGroupService.getGroupFile(groupId, groupDocumentId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 스터디 그룹에 멤버 초대
    @Operation(summary = "스터디 그룹 멤버 초대", description = "스터디 그룹에 멤버를 초대합니다. (방장만 가능)")
    @PostMapping("/{groupId}/members")
    public ResponseEntity<GlobalResponse> inviteMember(@PathVariable Long groupId, @RequestHeader("X-USER-ID") Long currentUserId,
                                                       @RequestBody @Valid GroupMemberInviteRequest request) {

        GroupMemberResponse response = studyGroupService.inviteMember(groupId, currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    // 스터디 그룹에서 본인 탈퇴(방 나가기)
    @Operation(summary = "스터디 그룹 탈퇴", description = "스터디 그룹에서 탈퇴합니다. (방장 불가)")
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<GlobalResponse> leaveGroup( @PathVariable Long groupId,
                                                      @RequestHeader("X-USER-ID") Long currentUserId) {

        studyGroupService.leaveGroup(groupId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED);
    }

    // 그룹 문서 업로드용 Presigned URL 발급
    // POST /api/groups/{groupId}/documents/presigned-url
    @Operation(summary = "그룹 문서 업로드용 Presigned URL 발급",
            description = "파일명만 전송하면 S3 직접 업로드용 Presigned URL을 반환합니다."+
                    "FE는 반환된 presignedUrl로 S3에 직접 PUT 업로드 후 /api/groups/{groupId}/documents/confirm을 호출해야 합니다.")
    @PostMapping("/{groupId}/documents/presigned-url")
    public ResponseEntity<GlobalResponse> getGroupDocumentPresignedUrl(@PathVariable Long groupId,
                                                  @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                                       @RequestHeader("X-USER-ID") Long currentUserId,
                                                                       @RequestParam String originalFileName) {

        PresignedUploadResponse response = studyGroupService.getGroupDocumentPresignedUrl(groupId, currentUserId, originalFileName);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 업로드 완료 후 그룹 문서 DB 저장
    // POST /api/groups/{groupId}/documents/confirm
    @Operation(summary = "그룹 문서 DB 저장 (업로드 완료 후 호출)", description = "S3 직접 업로드 완료 후 DB에 그룹 문서를 저장합니다." +
            " presigned-url 발급 시 반환된 s3Key를 함께 전송해야 합니다.")
    @PostMapping("/{groupId}/documents/confirm")
    public ResponseEntity<GlobalResponse> confirmGroupDocument(@PathVariable Long groupId,
                                                @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                               @RequestHeader("X-USER-ID") Long currentUserId,
                                                               @RequestBody @Valid GroupDocumentConfirmRequest request) {

        GroupDocumentAddResponse response = studyGroupService.confirmGroupDocument(groupId, currentUserId, request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }
}


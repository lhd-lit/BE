package LDHD.project.domain.group.web;
import LDHD.project.domain.group.web.dto.GroupDocumentListResponse;
import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.group.service.StudyGroupService;
import LDHD.project.domain.group.web.dto.*;
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

    // 단건 조회 + lastViewedAt 갱신
    @Operation(summary = "스터디 그룹 단건 조회", description = "특정 스터디 그룹을 조회하고 최근 조회 시간을 갱신합니다.")
    @GetMapping("/{groupId}")
    public ResponseEntity<GlobalResponse> getStudyGroup(@PathVariable Long groupId,
                                                        @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                        @RequestHeader("X-USER-ID") Long currentUserId) {

        GetStudyGroupListResponse response = studyGroupService.getStudyGroup(groupId, currentUserId);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
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
    // 그룹에 학습 문서 추가
    // /api/groups/{groupId}/documents?userId={userId}
    @Operation(summary = "그룹 학습 문서 추가", description = "특정 스터디 그룹에 학습 문서를 추가합니다.")
    @PostMapping("/{groupId}/documents")
    public ResponseEntity<GlobalResponse> addDocument(@PathVariable Long groupId,
                                                      @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                      @RequestHeader("X-USER-ID") Long currentUserId,
                                                      @RequestBody @Valid GroupDocumentAddRequest request
    ) {
        GroupDocumentAddResponse response = studyGroupService.addDocument(currentUserId, groupId, request);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 그룹 문서 목록 조회 API
    @GetMapping("/{groupId}/documents")
    public ResponseEntity<GlobalResponse> getGroupDocuments( @PathVariable Long groupId,
                                                             @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                             @RequestHeader("X-USER-ID") Long currentUserId,
                                                             @PageableDefault(size = 10, sort = "createdAt",
                                                                     direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 반환 타입 변경: GroupDocumentListResponse
        Page<GroupDocumentListResponse> response = studyGroupService.getGroupDocuments(currentUserId, groupId, pageable);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 학습 문서 파일 조회", description = "그룹 내 특정 학습 문서의 파일 정보를 조회합니다.(그룹 멤버만 조회 가능)")
    @GetMapping("/{groupId}/documents/{groupDocumentId}/file")
    public ResponseEntity<GlobalResponse> getGroupFile( @PathVariable Long groupId, @PathVariable Long groupDocumentId,
                                                        @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                        @RequestHeader("X-USER-ID") Long currentUserId) {

        GroupFileResponse response = studyGroupService.getGroupFile(groupId, groupDocumentId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

}


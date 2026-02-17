package LDHD.project.domain.group.web;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.group.service.StudyGroupService;
import LDHD.project.domain.group.web.dto.GroupDocumentAddRequest;
import LDHD.project.domain.group.web.dto.GroupDocumentAddResponse;
import LDHD.project.domain.group.web.dto.StudyGroupCreateRequest;
import LDHD.project.domain.group.web.dto.StudyGroupCreateResponse;
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
    public ResponseEntity<GlobalResponse> getGroupDocuments(@RequestParam Long userId,@PathVariable Long groupId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 반환 타입 변경: GroupDocumentListResponse
        Page<GroupDocumentListResponse> response = studyGroupService.getGroupDocuments(userId, groupId, pageable);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
}


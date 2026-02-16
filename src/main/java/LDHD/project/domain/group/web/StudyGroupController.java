package LDHD.project.domain.group.web;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.group.service.StudyGroupService;
import LDHD.project.domain.group.web.dto.GroupDocumentAddRequest;
import LDHD.project.domain.group.web.dto.GroupDocumentAddResponse;
import LDHD.project.domain.group.web.dto.StudyGroupCreateRequest;
import LDHD.project.domain.group.web.dto.StudyGroupCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<GlobalResponse> createGroup(@RequestParam Long userId,
                                                      @RequestBody @Valid StudyGroupCreateRequest request){

        StudyGroupCreateResponse response = studyGroupService.createGroup(userId,request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    // 그룹에 학습 문서 추가
    // /api/groups/{groupId}/documents?userId={userId}
    @Operation(summary = "그룹 학습 문서 추가", description = "특정 스터디 그룹에 학습 문서를 추가합니다.")
    @PostMapping("/{groupId}/documents")
    public ResponseEntity<GlobalResponse> addDocument(@RequestParam Long userId,@PathVariable Long groupId,
                                                             @RequestBody @Valid GroupDocumentAddRequest request
    ) {
        GroupDocumentAddResponse response = studyGroupService.addDocument(userId, groupId, request);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
}


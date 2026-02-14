package LDHD.project.domain.group.web;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.group.service.StudyGroupService;
import LDHD.project.domain.group.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    // 스터디 그룹 생성
    // /api/groups?userId={userId}
    @PostMapping
    public ResponseEntity<GlobalResponse> createGroup(@RequestParam Long userId,
                                                      @RequestBody @Valid StudyGroupCreateRequest request){

        StudyGroupCreateResponse response = studyGroupService.createGroup(userId,request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    // 그룹에 학습 문서 추가
    // /api/groups/{groupId}/documents?userId={userId}
    @PostMapping("/{groupId}/documents")
    public ResponseEntity<GlobalResponse> addDocument(@RequestParam Long userId,@PathVariable Long groupId,
                                                             @RequestBody @Valid GroupDocumentAddRequest request
    ) {
        GroupDocumentAddResponse response = studyGroupService.addDocument(userId, groupId, request);

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


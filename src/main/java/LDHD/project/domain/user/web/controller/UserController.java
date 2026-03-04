package LDHD.project.domain.user.web.controller;


import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.user.service.UserService;
import LDHD.project.domain.user.web.controller.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User API", description = "회원 가입, 삭제, 수정, 프로필 조회 관련 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
/*
    //회원 가입
    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    @PostMapping
    public ResponseEntity<GlobalResponse> createUser(@RequestBody CreateUserRequest request) {

        CreateUserResponse response = userService.createUser(request);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }
*/
    //회원 삭제
    @Operation(summary = "회원 삭제", description = "특정 회원을 삭제합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<GlobalResponse> deleteUser(@PathVariable Long userId) {

        DeleteUserResponse response = userService.deleteUser(userId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED, response);
    }
    /*
    //회원 정보 수정
    @Operation(summary = "회원 정보 수정", description = "회원의 정보를 수정합니다.")
    @PutMapping("/{userId}")
    public ResponseEntity<GlobalResponse> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {

        UpdateUserResponse response = userService.updateUser(userId, request);

        return GlobalResponse.onSuccess(SuccessCode.UPDATED, response);
    }
*/
    //회원 프로필 조회
    @Operation(summary = "회원 프로필 조회", description = "회원의 상세 프로필 정보를 조회합니다.")
    @GetMapping("/profile/{userId}")
    public ResponseEntity<GlobalResponse> getUserProfile(@PathVariable Long userId) {
        UserProfileResponse response = userService.getUserProfile(userId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 사용자 검색(이메일 기반)
    @Operation(summary = "사용자 이메일 검색", description = "이메일로 사용자를 검색합니다. 스터디 그룹 초대 시 사용합니다.")
    @GetMapping("/search")
    public ResponseEntity<GlobalResponse> searchUser(@RequestParam String email,
                                                     @RequestParam(required = false) Long groupId,
                                                     @RequestHeader("X-USER-ID") Long currentUserId) {

        UserSearchResponse response = userService.searchByEmail(email, currentUserId, groupId);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 사용자의 파일 용량 조회
    @Operation(summary = "파일 용량 조회", description = "사용자의 파일 저장 용량 사용 현황을 조회합니다.")
    @GetMapping("/storage")
    public ResponseEntity<GlobalResponse> getStorageUsage(@RequestHeader("X-USER-ID") Long currentUserId) {

        StorageResponse response = userService.getStorageUsage(currentUserId);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

}

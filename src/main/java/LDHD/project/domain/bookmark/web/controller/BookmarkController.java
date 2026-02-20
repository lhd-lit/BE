package LDHD.project.domain.bookmark.web.controller;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.bookmark.Bookmark;
import LDHD.project.domain.bookmark.service.BookmarkService;
import LDHD.project.domain.bookmark.web.controller.dto.CreateBookmarkResponse;
import LDHD.project.domain.bookmark.web.controller.dto.DeleteBookmarkResponse;
import LDHD.project.domain.bookmark.web.controller.dto.GetBookmarkListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookmark API", description = "즐겨찾기 생성, 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmark")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "즐겨찾기에 추가", description = "즐겨찾기에 추가합니다.")
    @PostMapping("/selfStudy/{selfStudyId}")
    public ResponseEntity<GlobalResponse> createBookmark(
            @PathVariable Long selfStudyId, @RequestHeader("X-USER-ID") Long currentUserId){

        CreateBookmarkResponse response = bookmarkService.createBookmark(selfStudyId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    @Operation(summary = "즐겨찾기에서 삭제", description = "즐겨찾기에서 삭제합니다.")
    @DeleteMapping("/selfStudy/{selfStudyId}")
    public ResponseEntity<GlobalResponse> deleteBookmark(
            @PathVariable Long selfStudyId, @RequestHeader("X-USER-ID") Long currentUserId){

        DeleteBookmarkResponse response = bookmarkService.deleteBookmark(selfStudyId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.DELETED, response);

    }

    @Operation(summary = "즐겨찾기 목록 조회", description = "사용자의 즐겨찾기 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getBookmarks(@RequestHeader("X-USER-ID") Long currentUserId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, 100);
        Page<GetBookmarkListResponse> response = bookmarkService.getBookmarks(currentUserId, page, safeSize);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

}

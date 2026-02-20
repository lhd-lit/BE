package LDHD.project.domain.bookmark.web.controller.dto;

import LDHD.project.domain.bookmark.Bookmark;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GetBookmarkListResponse {

    private final Long id;
    private final Long selfStudyId;
    private final String selfStudyTitle;  // 즐겨찾기 목록에서 제목 표시용
    private final LocalDateTime createdAt;

    public static GetBookmarkListResponse from(Bookmark bookmark) {
        return GetBookmarkListResponse.builder()
                .id(bookmark.getId())
                .selfStudyId(bookmark.getSelfStudy().getId())
                .selfStudyTitle(bookmark.getSelfStudy().getTitle())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}

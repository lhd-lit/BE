package LDHD.project.domain.bookmark.web.controller.dto;

import LDHD.project.domain.bookmark.Bookmark;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetGroupBookmarkListResponse {

    private Long bookmarkId;
    private Long studyGroupId;
    private String groupName;
    private String groupDescription;

    public static GetGroupBookmarkListResponse from(Bookmark bookmark) {
        return GetGroupBookmarkListResponse.builder()
                .bookmarkId(bookmark.getId())
                .studyGroupId(bookmark.getStudyGroup().getId())
                .groupName(bookmark.getStudyGroup().getName())
                .groupDescription(bookmark.getStudyGroup().getDescription())
                .build();
    }
}

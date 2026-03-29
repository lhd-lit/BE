package LDHD.project.domain.bookmark.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupBookmarkResponse {

    private Long bookmarkId;
    private Long studyGroupId;
    private Long userId;


}

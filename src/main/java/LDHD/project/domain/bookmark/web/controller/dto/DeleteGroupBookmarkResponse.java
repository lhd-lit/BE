package LDHD.project.domain.bookmark.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteGroupBookmarkResponse {

    private Long studyGroupId;
    private Long userId;
}

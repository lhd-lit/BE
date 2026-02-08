package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.selfStudy.SelfStudy;
import lombok.Getter;


@Getter
public class GroupDocumentAddResponse {

    private Long groupDocumentId;
    private Long selfStudyId;

    private String title;
    private String uploaderName;

    private GroupDocumentAddResponse(GroupDocument document) {

        this.groupDocumentId = document.getId();
        SelfStudy selfStudy = document.getSelfStudy();
        this.selfStudyId = selfStudy.getId();
        this.title = selfStudy.getTitle();
        this.uploaderName = selfStudy.getUploader().getName();
    }

    public static GroupDocumentAddResponse from(GroupDocument document) {
        return new GroupDocumentAddResponse(document);
    }
}

    package LDHD.project.domain.group.web.dto.document;

    import LDHD.project.domain.group.entity.GroupDocument;
    import lombok.Builder;
    import lombok.Getter;

    @Builder
    @Getter
    public class GroupDocumentAddResponse {

        private Long groupDocumentId;

        private String title;
        private String uploaderName;

        public static GroupDocumentAddResponse from(GroupDocument document) {

            return GroupDocumentAddResponse.builder()
                    .groupDocumentId(document.getId())
                    .title(document.getTitle())
                    .uploaderName(document.getUploader().getName())
                    .build();
        }
    }

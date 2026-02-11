package LDHD.project.domain.selfStudy.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSelfStudyRequest {

    String title;
    String description;
}

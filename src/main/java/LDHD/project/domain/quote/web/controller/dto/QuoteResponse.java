package LDHD.project.domain.quote.web.controller.dto;

import LDHD.project.domain.quote.Quote;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuoteResponse {

    private Long id;
    private String content;
    private String author;
    private String source;

    public static QuoteResponse from(Quote quote){
        return QuoteResponse.builder()
                .id(quote.getId())
                .content(quote.getContent())
                .author(quote.getAuthor())
                .source(quote.getSource())
                .build();
    }
}

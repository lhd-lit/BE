package LDHD.project.domain.quote.web.controller;

import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.quote.Repository.QuoteRepository;
import LDHD.project.domain.quote.service.QuoteService;
import LDHD.project.domain.quote.web.controller.dto.QuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quote API", description = "명언 대시보드")
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @Operation(summary = "랜덤 명언 조회", description = "대시보드 상단에 표시할 명언을 무작위로 1개 반환합니다.")
    @GetMapping("/random")
    public ResponseEntity<GlobalResponse> getRandomQuote() {

        QuoteResponse response = quoteService.getRandomQuote();

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }
}

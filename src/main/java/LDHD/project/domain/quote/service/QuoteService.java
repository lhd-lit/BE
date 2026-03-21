package LDHD.project.domain.quote.service;

import LDHD.project.domain.quote.Quote;
import LDHD.project.domain.quote.repository.QuoteRepository;
import LDHD.project.domain.quote.web.controller.dto.QuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private QuoteRepository quoteRepository;

    @Transactional(readOnly = true)
    public QuoteResponse getRandomQuote() {
        Quote quote = quoteRepository.findRandomQuote().orElseThrow(
                () -> new IllegalArgumentException("등록된 명언이 없습니다."));

        return QuoteResponse.from(quote);
    }
}

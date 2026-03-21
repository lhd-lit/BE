package LDHD.project.domain.quote.repository;

import LDHD.project.domain.quote.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    // RANDOM 함수로 1개 무작위로 가져오기
    @Query(value = "SELECT * FROM quote ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Quote> findRandomQuote();
}
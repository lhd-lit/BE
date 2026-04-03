package LDHD.project.domain.chat.client;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// WebClient
import org.springframework.web.reactive.function.client.WebClient;

// SSE 처리
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;

// Reactive Stream
import reactor.core.publisher.Flux;

// 기타
import java.time.Duration;
import java.util.Map;

@Primary
@Component
@RequiredArgsConstructor
public class FastApiClient implements AiClient {

    // WebClient 주입
    private final WebClient aiWebClient;

    @Override
    public Flux<String> streamResponse(String sessionId, String namespace, String question) {

        // FastAPI로 보낼 JSON Body 구성
        Map<String, String> body = Map.of(
                "session_id", sessionId,
                "namespace", namespace,
                "question", question
        );

        // WebClient 요청 시작
        return aiWebClient.post()
                .uri("/ai/ask") // FastAPI endpoint
                .contentType(MediaType.APPLICATION_JSON) // JSON 요청
                .accept(MediaType.TEXT_EVENT_STREAM) // SSE 요청
                .bodyValue(body) // Body 설정
                .retrieve()

                //  핵심: SSE를 안전하게 파싱
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})

                // data 부분만 추출
                .mapNotNull(ServerSentEvent::data)

                // 빈 데이터 제거
                .filter(data -> !data.isBlank())

                // 60초 타임아웃
                .timeout(Duration.ofSeconds(60))

                // 네트워크 오류 시 1회 재시도
                .retry(1);
    }
}
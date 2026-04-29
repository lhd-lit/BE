package LDHD.project.domain.chat.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;

// WebClient
import org.springframework.web.reactive.function.BodyInserters;
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

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class FastApiClient implements AiClient {

    // WebClient 주입
    private final WebClient aiWebClient;

    // PDF 업로드 구현
    @Override
    public String uploadPdf(byte[] fileBytes, String fileName, String namespace) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() { return fileName; }
            }).contentType(MediaType.APPLICATION_PDF);
            builder.part("namespace", namespace);

            Map response = aiWebClient.post()
                    .uri("/ai/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("AI PDF 업로드 완료 - namespace: {}", namespace);
            return namespace;

        } catch (Exception e) {
            log.error("AI PDF 업로드 실패 - namespace: {}", namespace, e);
            throw new RuntimeException("AI 서버 PDF 업로드 실패", e);
        }
    }

    // 스트리밍 응답
    @Override
    public Flux<String> streamResponse(String sessionId, String namespace, String question) {

        Map<String, String> body = Map.of(
                "session_id", sessionId,
                "namespace", namespace,
                "question", question
        );

        return aiWebClient.post()
                .uri("/ai/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> !data.isBlank())
                .timeout(Duration.ofSeconds(60))
                .retry(1)
                .doOnError(e -> log.error("AI 스트리밍 오류 - sessionId: {}", sessionId, e));
    }
}
package LDHD.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.client.MultipartBodyBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import LDHD.project.domain.ai.dto.AiRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient aiWebClient;

    // AI한테 질문
    public Flux<String> askAi(AiRequest request, String userIdentifier) {
        // 방어 로직 : 유저식별자 + 제목 조합으로 가공
        String safeNamespace = createSafeNamespace(userIdentifier, request.getLectureTitle());
        
        // AI 서버 규격에 맞춰서 규격 세팅
        request.setNamespace(safeNamespace);
        request.setSession_id("session_" + safeNamespace);

        log.info("AI 질문 요청 - Namespace: {}", safeNamespace);

        return aiWebClient.post()
                .uri("/ai/ask")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);
    }

    // 교안 업로드
    public Mono<String> uploadLecture(MultipartFile file, String title, String userIdentifier) {
        String safeNamespace = createSafeNamespace(userIdentifier, title);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());
        builder.part("namespace", safeNamespace);

        log.info("AI 업로드 요청 - Namespace: {}", safeNamespace);

        return aiWebClient.post()
                .uri("/ai/upload")
                .bodyValue(builder.build())
                .retrieve()
                .bodyToMono(String.class);
    }

    // 방어 로직 : 특수문자 제거 및 네임스페이스 생성 (글자 오류를 방지하기 위함)
    private String createSafeNamespace(String userIdentifier, String title) {
        if (title == null || title.isBlank()) return userIdentifier + "_unnamed";
        
        String cleanedTitle = title.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9가-힣_-]", "");
        
        return userIdentifier + "_" + cleanedTitle;
    }
}
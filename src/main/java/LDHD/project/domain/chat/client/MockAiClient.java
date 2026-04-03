package LDHD.project.domain.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Component
@Profile("local")
public class MockAiClient implements AiClient {

    /*
    @Override
    public String generateResponseWithContext(
            String question,
            String context,
            String chatHistory
    ) {
        log.info("MockAiClient (동기) 호출 - question: {}", question);

        // 3초 대기 (AI 호출 시뮬레이션)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return buildMockResponse(question, context, chatHistory);
    }
*/
    @Override
    public Flux<String> streamResponse(
            String sessionId,
            String namespace,
            String question
    ) {
        log.info("MockAiClient (스트리밍) 호출 - sessionId: {}, question: {}", sessionId, question);

        String fullResponse = buildMockResponse(question, "mock context", "mock history");

        // 문자열을 chunk 단위로 쪼개기
        String[] chunks = fullResponse.split(" ");

        return Flux.fromArray(chunks)
                .delayElements(Duration.ofMillis(300))
                .map(chunk -> chunk + " "); // 공백 복원
    }

    private String buildMockResponse(String question, String context, String chatHistory) {

        StringBuilder response = new StringBuilder();

        response.append("안녕하세요! 질문에 답변드리겠습니다.\n\n");
        response.append("**질문:** ").append(question).append("\n\n");

        // 문서 컨텍스트 요약
        if (context != null && !context.isBlank()) {
            String preview = context.length() > 100
                    ? context.substring(0, 100) + "..."
                    : context;

            response.append("**학습 자료 내용:**\n")
                    .append(preview)
                    .append("\n\n");
        }

        // 대화 히스토리 반영
        if (chatHistory != null && !chatHistory.isBlank()) {
            response.append("*(이전 대화를 참고했습니다)*\n\n");
        }

        response.append("이것은 **Mock AI 응답**입니다.\n\n");
        response.append("실제 환경에서는 FastAPI + LLM이 응답을 생성합니다.\n\n");

        response.append("**주요 특징:**\n");
        response.append("1. 스트리밍 응답 지원\n");
        response.append("2. WebSocket 실시간 전송\n");
        response.append("3. 문서 기반 질의응답\n");

        return response.toString();
    }
}
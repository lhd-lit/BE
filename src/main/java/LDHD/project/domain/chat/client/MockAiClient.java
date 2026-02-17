package LDHD.project.domain.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
//@Profile("local") : local 에서만 작동하므로 서버 환경에서는 AiClient가 스프링에 빈으로 등록이 되지 않는 오류 발생
public class MockAiClient implements AiClient {
    @Override
    public String generateResponseWithContext(
            String question,
            String context,
            String chatHistory
    ) {
        log.info("MockAiClient 호출 - question: {}", question);

        // 3초 대기 시뮬레이션 (실제 AI API 호출 시뮬레이션)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock 응답 생성
        StringBuilder response = new StringBuilder();

        response.append("안녕하세요! 질문에 답변드리겠습니다.\n\n");
        response.append("**질문:** ").append(question).append("\n\n");

        // 문서 컨텍스트 요약
        if (context != null && !context.isBlank()) {
            String preview = context.length() > 100
                    ? context.substring(0, 100) + "..."
                    : context;
            response.append("**학습 자료 내용:**\n").append(preview).append("\n\n");
        }

        // 대화 히스토리 확인
        if (chatHistory != null && !chatHistory.isBlank()) {
            response.append("*(이전 대화 내역을 참고했습니다)*\n\n");
        }

        response.append("이것은 **Mock AI 응답**입니다.\n\n");
        response.append("실제 환경에서는 Upstage Solar AI가 학습 자료를 분석하여 답변을 생성합니다.\n\n");
        response.append("**주요 포인트:**\n");
        response.append("1. 비동기 처리로 빠른 응답\n");
        response.append("2. WebSocket으로 실시간 전송\n");
        response.append("3. 문서 기반 정확한 답변\n");

        return response.toString();
    }
}

package LDHD.project.domain.chat.client;


public interface AiClient {
    // 학습 자료 기반 AI 응답 생성
    /* 사용자가 학습 자료(SelfStudy) 업로드
       문서에서 텍스트 추출(extractedText)
       사용자 질문 + 문서 컨텍스트 => Ai 응답 생성
     */
    String generateResponseWithContext(
            String question,
            String context,
            String chatHistory
    );
}

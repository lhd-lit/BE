package LDHD.project.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        // 1. 프론트엔드에서 연결할 웹소켓 주소
        // ws://localhost:8080/ws-stomp
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // 모든 도메인 허용 (CORS 해결)
                .withSockJS(); // SockJS 지원
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 메시지 보낼 때 (Publish) 경로: /pub/chat/message
        registry.setApplicationDestinationPrefixes("/pub");

        // 3. 메시지 받을 때 (Subscribe) 경로: /sub/chat/room/{id}
        registry.enableSimpleBroker("/sub");
    }

}

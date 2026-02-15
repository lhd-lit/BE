package LDHD.project.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;
    // local은 비밀번호가 없기 때문에 ":"를 붙혀서 비밀번호가 있으면 쓰고 없으면 없는대로 작동할 수 있도록 함
    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 1. 설정 객체 생성 (주소 + 포트)
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);

        // 2. 비밀번호 설정
        if (password != null && !password.isBlank()) {
            redisConfig.setPassword(password);
        }

        return new LettuceConnectionFactory(redisConfig);
    }

    // Refresh Token 문자열 전용
    @Bean(name = "tokenRedisTemplate")
    public RedisTemplate<String, String> tokenRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    // 일반 객체 JSON 저장용
    @Bean(name = "redisTemplate") // 이름을 "redisTemplate"으로 하면 기본 빈으로 잡힘
    @Primary // 이게 "메인"이라고 알려줌 (Repository가 이걸 갖다 씀)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer()); // JSON
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer()); // JSON
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}

package LDHD.project.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    // AI 응답 처리 전용 스레드 풀
    // 동시 AI 요청이 많아도 큐로 대기 처리, OOM 방지를 위해 큐 사이즈 제한
    @Bean(name = "aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 평시 유지 스레드 수
        // I/O 바운드 작업 → CPU 코어 수 * 2 기준
        executor.setCorePoolSize(10);

        // 큐가 꽉 찼을 때 최대로 늘어날 스레드 수 -> 너무 크면 메모리 위험
        executor.setMaxPoolSize(30);

        // 큐 사이즈: 큐가 꽉 차면 MaxPoolSize까지 스레드 증가
        // 100 초과 요청은 RejectedExecutionHandler로 처리
        executor.setQueueCapacity(100);

        // 유휴 스레드가 CorePoolSize 아래로 줄어들기까지 대기 시간 (초)
        executor.setKeepAliveSeconds(60);

        // true: CorePoolSize 이하 유휴 스레드도 KeepAlive 후 제거 (메모리 절약)
        executor.setAllowCoreThreadTimeOut(true);

        // 스레드 이름 prefix (로그/모니터링에서 구분 필수)
        executor.setThreadNamePrefix("ai-executor-");

        // 애플리케이션 종료 시 큐에 남은 작업이 완료될 때까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Graceful Shutdown 대기 시간
        // AI API 타임아웃을 고려하여 넉넉하게 설정 (30초)
        executor.setAwaitTerminationSeconds(30);

        // 큐 + MaxPool 모두 꽉 찼을 때 정책:
        // CallerRunsPolicy: 호출 스레드(WebSocket 핸들러)가 직접 실행 → 서비스 지연은 있지만 요청 유실 없음
        // (AbortPolicy는 예외 발생 → WebSocket 연결 끊김 위험)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("[AsyncConfig] aiExecutor 초기화 완료 - core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    // 공통 비동기 작업 전용 스레드 풀 (알림, 이메일 발송 등 확장 대비)
    // @Async("taskExecutor") 또는 @Async 단독 사용 시 적용 => AiChatService 에 적용
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("task-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);

        // 가벼운 작업이므로 CallerRunsPolicy로 유실 방지
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("[AsyncConfig] taskExecutor 초기화 완료 - core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}

package LDHD.project.domain.user.web.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StorageResponse {

    private long usedBytes;        // 사용한 용량 (bytes)
    private long totalBytes;       // 전체 용량 (bytes)
    private long availableBytes;   // 남은 용량 (bytes)
    private double usedPercent;    // 사용 비율 (%)

    private static final long TOTAL_STORAGE = 5L * 1024 * 1024 * 1024; // 5GB

    public static StorageResponse of(long usedBytes) {

        long available = Math.max(TOTAL_STORAGE - usedBytes, 0);
        double percent = Math.round((double) usedBytes / TOTAL_STORAGE * 1000) / 10.0;

        return StorageResponse.builder()
                .usedBytes(usedBytes)
                .totalBytes(TOTAL_STORAGE)
                .availableBytes(available)
                .usedPercent(percent)
                .build();
    }
}

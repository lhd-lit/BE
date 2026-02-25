package LDHD.project.domain.ai.controller;

import LDHD.project.domain.ai.dto.AiRequest;
import LDHD.project.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.security.Principal;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // AI 질문 API
    @PostMapping("/ask")
    public Flux<String> ask(@RequestBody AiRequest request, Principal principal) {
        // Principal에서 현재 로그인한 유저의 고유 식별자(이름 등)를 가져옴
        String userIdentifier = principal.getName(); 
        
        return aiService.askAi(request, userIdentifier);
    }

    // 교안 업로드 API
    @PostMapping("/upload")
    public Mono<String> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam("title") String title,
                               Principal principal) {
        String userIdentifier = principal.getName();
        return aiService.uploadLecture(file, title, userIdentifier);
    }
}
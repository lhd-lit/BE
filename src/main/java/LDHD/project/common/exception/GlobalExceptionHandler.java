package LDHD.project.common.exception;

import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.response.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<GlobalResponse> handleGeneralException(GeneralException e) {
        ErrorCode errorCode = e.getErrorCode();

        return GlobalResponse.onFailure(errorCode);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(","));

        return GlobalResponse.onFailure(errorCode, errorMessage);
    }

    @ExceptionHandler(com.fasterxml.jackson.core.JsonParseException.class) // JSON 파싱 오류 처리
    public ResponseEntity<GlobalResponse> handleJsonParseException(
            com.fasterxml.jackson.core.JsonParseException e) {
        log.error("JSON 파싱 오류: {}", e.getMessage());
        return GlobalResponse.onFailure(ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(com.fasterxml.jackson.databind.JsonMappingException.class) // JSON 매핑 오류 처리
    public ResponseEntity<GlobalResponse> handleJsonMappingException(
            com.fasterxml.jackson.databind.JsonMappingException e) {
        log.error("JSON 매핑 오류: {}", e.getMessage());
        return GlobalResponse.onFailure(ErrorCode.VALIDATION_FAILED);
    }

    // X-USER-ID 헤더 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<GlobalResponse> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("필수 헤더 누락 - {}", e.getHeaderName());
        return GlobalResponse.onFailure(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse> handleGenericException(Exception e) {
        ErrorCode errorCode =
                ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("Unexpected Error Occured");
        log.error(e.getMessage(), e);
        log.error(e.getClass().getSimpleName());

        return GlobalResponse.onFailure(errorCode);
    }
}

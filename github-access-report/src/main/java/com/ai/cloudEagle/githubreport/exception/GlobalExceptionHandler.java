package com.ai.cloudEagle.githubreport.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GitHubApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleGitHubApiException(GitHubApiException ex) {

        log.error("GitHub API error: {}", ex.getMessage(), ex);

        return ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex) {

        log.error("Unexpected error occurred", ex);

        return "Internal server error. Please try again later.";
    }
}
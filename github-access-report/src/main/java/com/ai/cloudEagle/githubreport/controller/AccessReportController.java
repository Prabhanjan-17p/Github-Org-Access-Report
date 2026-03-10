package com.ai.cloudEagle.githubreport.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.ai.cloudEagle.githubreport.dto.AccessReportDto;
import com.ai.cloudEagle.githubreport.dto.UserAccessDto;
import com.ai.cloudEagle.githubreport.service.AccessReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccessReportController {

    private final AccessReportService service;

    @GetMapping("/access-report/{org}")
    public AccessReportDto getFullReport(
            @PathVariable String org,
            @RequestHeader("Authorization") String token) {

        log.info("Received request for GitHub access report for org: {}", org);

        return service.generateReport(org, token);
    }

    @GetMapping("/access-report/{org}/summary")
    public AccessReportDto.ReportSummary getSummary(
            @PathVariable String org,
            @RequestHeader("Authorization") String token) {

        log.info("Received request for summary report for org: {}", org);

        return service.generateReport(org, token).getSummary();
    }

    @GetMapping("/access-report/{org}/users")
    public List<UserAccessDto> getUsers(
            @PathVariable String org,
            @RequestHeader("Authorization") String token) {

        log.info("Fetching all users with repository access for org: {}", org);

        return service.generateReport(org, token).getUsers();
    }

    @GetMapping("/access-report/{org}/users/{username}")
    public UserAccessDto getUserRepositories(
            @PathVariable String org,
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {

        log.info("Fetching repositories for user {} in org {}", username, org);

        return service.generateReport(org, token)
                .getUsers()
                .stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found in organization"));
    }

    @GetMapping("/health")
    public String health() {

        log.info("Health check endpoint called");

        return "GitHub Access Report Service is running";
    }
}
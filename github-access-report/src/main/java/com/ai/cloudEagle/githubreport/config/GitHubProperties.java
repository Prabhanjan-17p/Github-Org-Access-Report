package com.ai.cloudEagle.githubreport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    private String token;
    private String apiUrl = "https://api.github.com";
    private int maxConcurrentRequests = 10;

}
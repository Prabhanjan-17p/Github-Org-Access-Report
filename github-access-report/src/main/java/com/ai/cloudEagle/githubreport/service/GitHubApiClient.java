package com.ai.cloudEagle.githubreport.service;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.ai.cloudEagle.githubreport.config.GitHubProperties;
import com.ai.cloudEagle.githubreport.model.GitHubCollaborator;
import com.ai.cloudEagle.githubreport.model.GitHubRepository;
import com.ai.cloudEagle.githubreport.model.GitHubTeam;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class GitHubApiClient {

    private final GitHubProperties properties;

    private WebClient webClient(String token) {
    	
        return WebClient.builder()
                .baseUrl(properties.getApiUrl())
                .defaultHeader("Authorization", token)
                .build();
    }

    public Flux<GitHubRepository> fetchAllRepositories(String org, String token) {

        return webClient(token)
                .get()
                .uri("/orgs/" + org + "/repos")
                .retrieve()
                .bodyToFlux(GitHubRepository.class);
    }

    public Flux<GitHubCollaborator> fetchCollaborators(String org, String repo, String token) {

        return webClient(token)
                .get()
                .uri("/repos/" + org + "/" + repo + "/collaborators")
                .retrieve()
                .bodyToFlux(GitHubCollaborator.class);
    }

    public Flux<GitHubTeam> fetchAllTeams(String org, String token) {

        return webClient(token)
                .get()
                .uri("/orgs/" + org + "/teams")
                .retrieve()
                .bodyToFlux(GitHubTeam.class);
    }

    public Flux<GitHubCollaborator> fetchTeamMembers(String org, String team, String token) {

        return webClient(token)
                .get()
                .uri("/orgs/" + org + "/teams/" + team + "/members")
                .retrieve()
                .bodyToFlux(GitHubCollaborator.class);
    }

    public Flux<GitHubRepository> fetchTeamRepositories(String org, String team, String token) {

        return webClient(token)
                .get()
                .uri("/orgs/" + org + "/teams/" + team + "/repos")
                .retrieve()
                .bodyToFlux(GitHubRepository.class);
    }
}
package com.ai.cloudEagle.githubreport.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ai.cloudEagle.githubreport.config.GitHubProperties;
import com.ai.cloudEagle.githubreport.dto.AccessReportDto;
import com.ai.cloudEagle.githubreport.dto.RepositoryAccessDto;
import com.ai.cloudEagle.githubreport.dto.UserAccessDto;
import com.ai.cloudEagle.githubreport.exception.GitHubApiException;
import com.ai.cloudEagle.githubreport.model.GitHubCollaborator;
import com.ai.cloudEagle.githubreport.model.GitHubRepository;
import com.ai.cloudEagle.githubreport.model.GitHubTeam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessReportService {

    private final GitHubApiClient apiClient;
    private final GitHubProperties githubProperties;

    @Cacheable(value = "accessReports", key = "#org")
    public AccessReportDto generateReport(String org, String token) {

        log.info("Generating GitHub access report for org {}", org);

        long start = System.currentTimeMillis();

        Map<String, Map<String, RepositoryAccessDto>> userRepoMap = new ConcurrentHashMap<>();

        log.info("Fetching repositories for org: {}", org);

        List<GitHubRepository> repositories =
                apiClient.fetchAllRepositories(org, token)
                        .collectList()
                        .block();

        if (repositories == null || repositories.isEmpty()) {
            log.error("Failed to fetch repositories for org: {}", org);
            throw new GitHubApiException("Unable to fetch repositories from GitHub");
        }

        log.info("Total repositories fetched: {}", repositories.size());

        Map<String, GitHubRepository> repoMap =
                repositories.stream()
                        .collect(Collectors.toMap(
                                GitHubRepository::getFull_name,
                                r -> r
                        ));

        // STEP 2: Fetch contributors in parallel
        Flux.fromIterable(repositories)
                .flatMap(
                        repo -> fetchCollaborators(
                                org,
                                repo,
                                token,
                                userRepoMap
                        ),
                        githubProperties.getMaxConcurrentRequests()
                )
                .then()
                .block();

        // STEP 3: Fetch team-based access
        fetchTeamAccess(org, token, userRepoMap, repoMap).block();

        long elapsed = System.currentTimeMillis() - start;

        log.info("Access report generation completed in {} ms", elapsed);

        // STEP 4: Build report
        return buildReport(org, userRepoMap, repositories, elapsed);
    }


    private Mono<Void> fetchCollaborators(
            String org,
            GitHubRepository repo,
            String token,
            Map<String, Map<String, RepositoryAccessDto>> userRepoMap) {

        return apiClient
                .fetchCollaborators(org, repo.getName(), token)

                .doOnNext(user -> {

                    log.debug(
                            "Contributor {} found in repo {}",
                            user.getLogin(),
                            repo.getName()
                    );

                    RepositoryAccessDto access =
                            RepositoryAccessDto.builder()
                                    .repoName(repo.getName())
                                    .repoFullName(repo.getFull_name())
                                    .repoUrl(repo.getHtml_url())
                                    .permission("contributor")
                                    .visibility(
                                            repo.isPrivateRepo()
                                                    ? "private"
                                                    : "public"
                                    )
                                    .archived(false)
                                    .accessSource("direct")
                                    .build();

                    userRepoMap
                            .computeIfAbsent(
                                    user.getLogin(),
                                    k -> new ConcurrentHashMap<>()
                            )
                            .put(repo.getFull_name(), access);
                })

                .then()

                .onErrorResume(ex -> {

                    log.warn(
                            "Failed fetching contributors for {} : {}",
                            repo.getName(),
                            ex.getMessage()
                    );

                    return Mono.empty();
                });
    }


    private Mono<Void> fetchTeamAccess(
            String org,
            String token,
            Map<String, Map<String, RepositoryAccessDto>> userRepoMap,
            Map<String, GitHubRepository> reposByName) {

        return apiClient
                .fetchAllTeams(org, token)

                .flatMap(
                        team -> processTeam(
                                org,
                                token,
                                team,
                                userRepoMap,
                                reposByName
                        ),
                        githubProperties.getMaxConcurrentRequests()
                )

                .then()

                .onErrorResume(ex -> {

                    log.warn("Team fetch failed {}", ex.getMessage());

                    return Mono.empty();
                });
    }


    private Mono<Void> processTeam(
            String org,
            String token,
            GitHubTeam team,
            Map<String, Map<String, RepositoryAccessDto>> userRepoMap,
            Map<String, GitHubRepository> reposByName) {

        Mono<List<GitHubCollaborator>> members =
                apiClient
                        .fetchTeamMembers(org, team.getSlug(), token)
                        .collectList();

        Mono<List<GitHubRepository>> repos =
                apiClient
                        .fetchTeamRepositories(org, team.getSlug(), token)
                        .collectList();

        return Mono.zip(members, repos)

                .doOnNext(tuple -> {

                    List<GitHubCollaborator> teamMembers = tuple.getT1();
                    List<GitHubRepository> teamRepos = tuple.getT2();

                    for (GitHubCollaborator member : teamMembers) {

                        Map<String, RepositoryAccessDto> repoAccess =
                                userRepoMap.computeIfAbsent(
                                        member.getLogin(),
                                        k -> new ConcurrentHashMap<>()
                                );

                        for (GitHubRepository repo : teamRepos) {

                            repoAccess.computeIfAbsent(
                                    repo.getFull_name(),
                                    r -> {

                                        GitHubRepository canonical =
                                                reposByName.getOrDefault(
                                                        repo.getFull_name(),
                                                        repo
                                                );

                                        return RepositoryAccessDto
                                                .builder()
                                                .repoName(
                                                        canonical.getName()
                                                )
                                                .repoFullName(
                                                        canonical.getFull_name()
                                                )
                                                .repoUrl(
                                                        canonical.getHtml_url()
                                                )
                                                .permission(
                                                        team.getPermission()
                                                )
                                                .visibility(
                                                        canonical.isPrivateRepo()
                                                                ? "private"
                                                                : "public"
                                                )
                                                .archived(false)
                                                .accessSource(
                                                        "team:" + team.getSlug()
                                                )
                                                .build();
                                    }
                            );
                        }
                    }
                })

                .then()

                .onErrorResume(ex -> {

                    log.warn(
                            "Team {} processing error {}",
                            team.getSlug(),
                            ex.getMessage()
                    );

                    return Mono.empty();
                });
    }

    private AccessReportDto buildReport(
            String org,
            Map<String, Map<String, RepositoryAccessDto>> userRepoMap,
            List<GitHubRepository> repos,
            long elapsed) {

        List<UserAccessDto> users =
                userRepoMap.entrySet()
                        .stream()
                        .map(entry -> {

                            String username = entry.getKey();

                            List<RepositoryAccessDto> repoList =
                                    new ArrayList<>(
                                            entry.getValue().values()
                                    );

                            return UserAccessDto.builder()
                                    .username(username)
                                    .profileUrl(
                                            "https://github.com/" + username
                                    )
                                    .totalRepositories(repoList.size())
                                    .repositories(repoList)
                                    .build();
                        })
                        .sorted(
                                Comparator.comparing(
                                        UserAccessDto::getUsername
                                )
                        )
                        .toList();

        int collaborations =
                users.stream()
                        .mapToInt(UserAccessDto::getTotalRepositories)
                        .sum();

        return AccessReportDto.builder()
                .organization(org)
                .generatedAt(Instant.now())
                .summary(
                        AccessReportDto.ReportSummary.builder()
                                .totalRepositories(repos.size())
                                .totalUsers(users.size())
                                .totalCollaborations(collaborations)
                                .generationTimeMs(elapsed)
                                .build()
                )
                .users(users)
                .build();
    }
}
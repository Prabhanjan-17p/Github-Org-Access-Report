package com.ai.cloudEagle.githubreport.dto;


import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AccessReportDto {

    private String organization;
    private Instant generatedAt;
    private ReportSummary summary;
    private List<UserAccessDto> users;

    @Data
    @Builder
    public static class ReportSummary {

        private int totalRepositories;
        private int totalUsers;
        private int totalCollaborations;
        private long generationTimeMs;

    }

}

package com.ai.cloudEagle.githubreport.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepositoryAccessDto {

    private String repoName;
    private String repoFullName;
    private String repoUrl;
    private String permission;
    private String visibility;
    private boolean archived;
    private String accessSource;

}
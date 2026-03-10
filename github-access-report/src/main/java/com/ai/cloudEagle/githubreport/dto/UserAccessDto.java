package com.ai.cloudEagle.githubreport.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserAccessDto {

    private String username;
    private String profileUrl;
    private int totalRepositories;
    private List<RepositoryAccessDto> repositories;

}
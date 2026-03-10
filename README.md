# 🔐 GitHub Organization Access Report

A Spring Boot service that connects to the GitHub API and generates a structured report showing which users have access to which repositories within a GitHub organization.

This service authenticates with GitHub, retrieves repository and collaboration data, and exposes an API endpoint that returns a user → repository access mapping.

---

## 📋 Table of Contents

[![Overview](https://img.shields.io/badge/Overview-blue)](#overview)
[![Tech Stack](https://img.shields.io/badge/Tech%20Stack-blue)](#tech-stack)
[![Project Structure](https://img.shields.io/badge/Project%20Structure-blue)](#project-structure)
[![How to Run with Authentication](https://img.shields.io/badge/How%20to%20Run-blue)](#how-to-run-with-authentication)
[![API Endpoints](https://img.shields.io/badge/API%20Endpoints-blue)](#api-endpoints)
[![Sample Responses](https://img.shields.io/badge/Sample%20Responses-blue)](#sample-responses)
[![Design Decisions & Assumptions](https://img.shields.io/badge/Design%20Decisions%20%26%20Assumptions-blue)](#design-decisions--assumptions)

---

## Overview

This service:
- Authenticates with GitHub using a **Personal Access Token (Bearer token)**
- Retrieves all **repositories** of a GitHub organization
- Determines which users have access via **direct collaboration** or **team membership**
- Returns a structured JSON report aggregated per user
- Supports organizations with **100+ repos and 1000+ users** using **concurrent reactive API calls**

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| HTTP Client | Spring WebFlux (WebClient) |
| Concurrency | Project Reactor (Flux/Mono) |
| Caching | Spring Cache |
| Build Tool | Maven |
| Boilerplate | Lombok |

---

## Project Structure

```
src/main/java/com/ai/cloudEagle/githubreport/
├── GithubAccessReportApplication.java      # Entry point
├── config/
│   └── GitHubProperties.java               # GitHub config (token, baseUrl, concurrency)
├── controller/
│   └── AccessReportController.java         # REST API endpoints
├── dto/
│   ├── AccessReportDto.java                # Full report + summary
│   ├── RepositoryAccessDto.java            # Per-repo access info
│   └── UserAccessDto.java                  # Per-user access view
├── exception/
│   ├── GitHubApiException.java             # Custom exception
│   └── GlobalExceptionHandler.java         # Centralized error handling
├── model/
│   ├── GitHubCollaborator.java             # GitHub user model
│   ├── GitHubRepository.java               # GitHub repo model
│   └── GitHubTeam.java                     # GitHub team model
└── service/
    ├── AccessReportService.java            # Core business logic
    └── GitHubApiClient.java                # GitHub API calls via WebClient
```

---

## How to Run with Authentication

### Prerequisites

- Java 21
- Maven 3.8+
- Git
- A GitHub **Personal Access Token** (see [Authentication](#authentication))
  - The GitHub token is provided in the API request using the Authorization header (Bearer Token) and is not stored in the application configuration.
---
### Step 1- Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/github-access-report.git
cd github-access-report
```
---
### Step 2: Build the Project
**Run the following command to build the project:**

```properties
mvn clean install
```
---
### Step 3: Run the Application
**Start the Spring Boot application using:**
```bash
mvn spring-boot:run
```
**The server will start at:**
```
http://localhost:8080
```
---
### Step 4: Generate a GitHub Personal Access Token**

This service uses **GitHub Personal Access Token (PAT)** authentication.

1. Go to [GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)](https://github.com/settings/tokens)
2. Click **Generate new token (classic)**
3. Select the following scopes:
   - `repo` — Full control of private repositories
   - `read:org` — Read org and team membership
   - `read:user` — Read user profile data
4. Copy the generated token (starts with `ghp_`)

---
### Step 5. Call the API Using Bearer Token
**Open Postman and create a new GET request.**

Example endpoint:
```
http://localhost:8080/api/access-report/kanha-org
```
### Configure Authorization in Postman

1. Open the Authorization tab
2. Set Auth Type to:

Bearer Token

3. Paste your GitHub token into the Token field
- Postman will automatically send the request header:
- Authorization: Bearer <your_github_token>

4. Click Send
- You will receive the GitHub organization access report in JSON format.

---
## API Endpoints

Base URL: http://localhost:8080

All endpoints (except /api/health) require authentication using a GitHub Personal Access Token.

Authorization: Bearer <your_github_token>

--------------------------------------------------

1. Full Access Report `GET /api/access-report/{org}`

Returns the complete report — all users and all their repositories.

Example:
```bash
curl -H "Authorization: Bearer ghp_YOUR_TOKEN" http://localhost:8080/api/access-report/kanha-org
```

--------------------------------------------------

2. Summary Only `GET /api/access-report/{org}/summary`

Returns just the summary stats: total repos, users, collaborations, and generation time.

Example:
```bash
curl -H "Authorization: Bearer ghp_YOUR_TOKEN" http://localhost:8080/api/access-report/kanha-org/summary
```

--------------------------------------------------

3. All Users With Access `GET /api/access-report/{org}/users`

Returns a list of all users and the repositories they can access.

Example:
```bash
curl -H "Authorization: Bearer ghp_YOUR_TOKEN" http://localhost:8080/api/access-report/kanha-org/users
```

--------------------------------------------------

4. Single User Repository Access `GET /api/access-report/{org}/users/{username}`

Returns repositories accessible by a specific user.

Example:
```bash
curl -H "Authorization: Bearer ghp_YOUR_TOKEN" http://localhost:8080/api/access-report/kanha-org/users/Prabhanjan-17p
```

--------------------------------------------------

5. Health Check `GET /api/health`

No authentication required. Returns a simple status string.

Example:
```bash
curl http://localhost:8080/api/health
```
---

## Sample Responses

### GET `/api/access-report/kanha-org`

```json
{
  "organization": "kanha-org",
  "generatedAt": "2026-03-10T07:54:09.389062500Z",
  "summary": {
    "totalRepositories": 1,
    "totalUsers": 3,
    "totalCollaborations": 3,
    "generationTimeMs": 1648
  },
  "users": [
    {
      "username": "Kanha-amanta",
      "profileUrl": "https://github.com/Kanha-amanta",
      "totalRepositories": 1,
      "repositories": [
        {
          "repoName": "Git-Report",
          "repoFullName": "kanha-org/Git-Report",
          "repoUrl": "https://github.com/kanha-org/Git-Report",
          "permission": "contributor",
          "visibility": "public",
          "archived": false,
          "accessSource": "direct"
        }
      ]
    },
    {
      "username": "Prabhanjan-17p",
      "profileUrl": "https://github.com/Prabhanjan-17p",
      "totalRepositories": 1,
      "repositories": [
        {
          "repoName": "Git-Report",
          "repoFullName": "kanha-org/Git-Report",
          "repoUrl": "https://github.com/kanha-org/Git-Report",
          "permission": "contributor",
          "visibility": "public",
          "archived": false,
          "accessSource": "direct"
        }
      ]
    },
    {
      "username": "Soubhagyasw",
      "profileUrl": "https://github.com/Soubhagyasw",
      "totalRepositories": 1,
      "repositories": [
        {
          "repoName": "Git-Report",
          "repoFullName": "kanha-org/Git-Report",
          "repoUrl": "https://github.com/kanha-org/Git-Report",
          "permission": "contributor",
          "visibility": "public",
          "archived": false,
          "accessSource": "direct"
        }
      ]
    }
  ]
}
```

---

### GET `/api/access-report/kanha-org/summary`

```json
{
  "totalRepositories": 1,
  "totalUsers": 2,
  "totalCollaborations": 2,
  "generationTimeMs": 3843
}
```

---

### GET `/api/access-report/kanha-org/users`

```json
[
  {
    "username": "Kanha-amanta",
    "profileUrl": "https://github.com/Kanha-amanta",
    "totalRepositories": 1,
    "repositories": [
      {
        "repoName": "Git-Report",
        "repoFullName": "kanha-org/Git-Report",
        "repoUrl": "https://github.com/kanha-org/Git-Report",
        "permission": "contributor",
        "visibility": "public",
        "archived": false,
        "accessSource": "direct"
      }
    ]
  },
  {
    "username": "Prabhanjan-17p",
    "profileUrl": "https://github.com/Prabhanjan-17p",
    "totalRepositories": 1,
    "repositories": [
      {
        "repoName": "Git-Report",
        "repoFullName": "kanha-org/Git-Report",
        "repoUrl": "https://github.com/kanha-org/Git-Report",
        "permission": "contributor",
        "visibility": "public",
        "archived": false,
        "accessSource": "direct"
      }
    ]
  }
]
```

---

### GET `/api/access-report/kanha-org/users/Prabhanjan-17p`

```json
{
  "username": "Prabhanjan-17p",
  "profileUrl": "https://github.com/Prabhanjan-17p",
  "totalRepositories": 1,
  "repositories": [
    {
      "repoName": "Git-Report",
      "repoFullName": "kanha-org/Git-Report",
      "repoUrl": "https://github.com/kanha-org/Git-Report",
      "permission": "contributor",
      "visibility": "public",
      "archived": false,
      "accessSource": "direct"
    }
  ]
}
```

---

### GET `/api/health`

```
GitHub Access Report Service is running
```

---

## Design Decisions & Assumptions

### ✅ Reactive + Concurrent API Calls (Scale Requirement)

GitHub rate limits and large orgs make sequential calls too slow. The service uses **Spring WebFlux (Project Reactor)** with `Flux.flatMap(..., maxConcurrency)` to fetch repo collaborators and team repositories **in parallel**, controlled by `github.max-concurrent-requests` (default: 10).

### ✅ Dual Access Source Tracking

Access can come from two sources:
- `"direct"` — user is a direct collaborator on the repo
- `"team:<slug>"` — user inherits access through a GitHub team

Team access is merged without duplicates using `ConcurrentHashMap.computeIfAbsent`, so a user won't appear twice for the same repo.

### ✅ Caching with `@Cacheable`

Report generation can be slow for large orgs. Results are cached per organization using Spring Cache (`@Cacheable(value = "accessReports", key = "#org")`). This avoids redundant GitHub API calls on repeated requests for the same org.

> **Note:** Cache is in-memory (simple). For production, replace with Redis for distributed caching.

### ✅ Error Resilience

Each concurrent fetch (collaborators, team members, team repos) uses `.onErrorResume(ex -> Mono.empty())` so a single failing repo or team doesn't crash the entire report — partial data is returned gracefully with warnings logged.

### ✅ Token Passed Per Request (Not Hardcoded)

The GitHub token is accepted via the `Authorization` request header. This allows different callers to use different GitHub tokens without restarting the service — useful for multi-tenant or multi-org setups.

### ⚠️ Assumptions

| Assumption | Reason |
|---|---|
| GitHub token has `repo` + `read:org` scopes | Required to read private repos and org teams |
| `permission` field for direct collaborators is set to `"contributor"` | GitHub's `/collaborators` endpoint doesn't return permission level in the same call; a separate call per user would exceed rate limits |
| Archived repos are marked `archived: false` | The GitHub model field was not mapped in this version; can be added by extending `GitHubRepository` |
| Users are sorted alphabetically by username | Makes the report easier to scan and consistent across calls |
| Cache is per-org, not per-token | Simplification; production should key by both org + token |

---

## Author

Built by **Prabhanjan Amanta (Kanha)**
- GitHub: [@Prabhanjan-17p](https://github.com/Prabhanjan-17p)
- LinkedIn: [Pravanjana Amanta](https://www.linkedin.com/in/pravanjan-17p/)

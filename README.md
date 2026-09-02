# TeamUI

Self-hosted platform for managing support engineering and IT teams.

## Architecture

- **Java 21** + **Spring Boot 4.1.0**
- **PostgreSQL 16** with **Flyway** migrations
- **JWT** stateless authentication (JJWT 0.12.x)
- **Virtual Threads** enabled
- **Feature-based packaging** under `com.teamui.domain.*`

## Modules

| Module | Description | Key Entities |
|--------|-------------|--------------|
| **Auth** | JWT login, registration, invites | `User`, `Invite` |
| **Org Structure** | Streams → Clusters → Teams | `Stream`, `Cluster`, `Team`, `TeamMembership` |
| **Meetings** | 1:1 scheduling, shared/private notes, action items | `Meeting`, `MeetingAction` |
| **Timeline** | Achievements, incidents, notes, training | `Event`, `ExternalEvidence` |
| **Competency** | 7-axis star-radar with history & benchmarking | `Competency`, `UserCompetencyScore` |
| **Bus Factor** | IT systems, expertise matrix, risk alerts | `ITSystem`, `SystemExpertise` |
| **Pulse Surveys** | Pre-1:1 energy check-ins, team trends | `PulseSurvey` |

## Quick Start

```bash
# 1. Start PostgreSQL 16
# 2. Run migrations (Flyway executes automatically on startup)
# 3. Start application
./mvnw spring-boot:run

# API base URL: http://localhost:8080/api
```

## Role Hierarchy

- `TEAM_MEMBER` — can view own data, submit pulse surveys, perform self-assessments
- `TEAM_LEAD` / `IT_LEAD` — full team management: meetings, events, expertise, scores
- `STREAM_LEAD` / `STREAM_IT_LEAD` — stream-level oversight
- `ADMIN` — full platform access

## API Documentation

See inline Javadoc on controllers:
- `MeetingController` → `/api/meetings`
- `EventController` → `/api/events`
- `CompetencyController` → `/api/competencies`
- `BusFactorController` → `/api/systems`
- `PulseSurveyController` → `/api/pulse`

## Database

 migrations live in `src/main/resources/db/migration/`:
1. `V1__init.sql` — full schema + seed data (7 competencies)
2. `V2__optimistic_locking.sql` — version columns
3. `V3__competency_history.sql` — score snapshots

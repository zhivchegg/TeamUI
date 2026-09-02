# TeamUI — Training & Onboarding Guide

## 1. What is TeamUI?

TeamUI is a self‑hosted web platform designed for **support engineering and IT‑soprovozhdenie teams**. It connects people, processes, and knowledge in one place:

- **1:1 Meetings** with shared & private notes and action tracking.
- **Timeline** (achievements, incidents, notes) for every employee.
- **Competency Radar** (7 axes, self + lead scoring, trends, benchmarking).
- **Bus Factor** matrix — who knows what systems, and where the risks are.
- **Pulse Surveys** — 2‑minute pre‑meeting energy check‑ins with team trends.

The platform is built as a **modular monolith** (Spring Boot + PostgreSQL) with a REST API ready for a React/Vue SPA front‑end.

---

## 2. Users & Roles

| Role | What they can do |
|------|------------------|
| **TEAM_MEMBER** | View own data, submit pulse surveys, do self‑assessments, view team dashboards |
| **TEAM_LEAD / IT_LEAD** | Manage meetings, create timeline events, assign competency & expertise scores, register systems |
| **STREAM_LEAD / STREAM_IT_LEAD** | Oversee multiple teams and clusters |
| **ADMIN** | Full access to everything |

> Roles are granted through **team memberships** (a user can be a member in one team and a lead in another).

---

## 3. Module A — Meetings (1:1)

### What it does
A lead schedules a **1:1 meeting** with a team member. During or after the meeting they fill in:
- **Shared notes** — visible to both leads of the team.
- **Private notes** — visible only to the meeting author (permanent audit trail).
- **Action items** — who does what by when.

### Typical flow
1. **Lead** → `POST /api/meetings` (choose participant, date, status).
2. Before meeting → **Employee** submits a pulse check‑in (`POST /api/pulse`).
3. During meeting → Lead updates shared notes via `PUT /api/meetings/{id}/notes`.
4. After meeting → Lead adds action items `POST /api/meetings/{id}/actions`.
5. Later → mark actions done via `PUT /api/meetings/{id}/actions/{actionId}`.

### Optimistic locking
Every `PUT` and `DELETE` requires a `version` field. If someone else edited the meeting in parallel, you get **HTTP 409 Conflict**.

---

## 4. Module B — Timeline Events

### What it does
A chronological record of everything significant that happens to an employee:
- **ACHIEVEMENT** — critical incident resolution, major contribution.
- **INCIDENT** — something that went wrong.
- **TRAINING** — completed course.
- **NOTE** — free‑form context for 1:1s.
- **LATENESS / TASK_MISS** — negative events (configurable visibility).

### Visibility rule
Each event has a flag `visibleToEmployee`:
- **true** → employee sees it on their own timeline.
- **false** → only leads see it (e.g., a sensitive incident).

### Typical flow
1. **Lead** → `POST /api/events` (select user, type, title, impact, visibility).
2. **Lead / Employee** → `GET /api/events/user/{userId}` to view the timeline.
3. **Lead** → `GET /api/events/team/{teamId}?eventType=ACHIEVEMENT` to filter.

---

## 5. Module C — Competency Radar

### What it does
A **7‑axis star model** for assessing employee skills in support engineering:

1. Техническое расследование
2. Управление инцидентом
3. Коммуникация и документирование
4. Платформенное мышление и процессы
5. Командная устойчивость (Bus Factor)
6. Самоуправление и развитие
7. AI и автоматизация

Employee rates themselves (1–5), then the lead rates them. Both scores appear on a radar chart.

### Self‑assessment
- **Employee** → `POST /api/competencies/scores/user/{myId}/self`

### Lead assessment
- **Lead** → `POST /api/competencies/scores/user/{employeeId}/lead`

### Views
| Endpoint | What you see |
|----------|--------------|
| `GET /api/competencies/radar/{userId}` | Personal radar (self + lead) |
| `GET /api/competencies/radar/team/{teamId}` | Team average radar |
| `GET /api/competencies/history/user/{userId}` | All score changes over time |
| `GET /api/competencies/trend/user/{userId}/competency/{compId}?granularity=week` | Trend line for one axis |
| `GET /api/competencies/benchmark/user/{userId}/team/{teamId}` | Employee vs team average overlay |

### History
Every time a self or lead score is saved, a snapshot is stored. You can track how the employee grew over months.

---

## 6. Module D — Bus Factor

### What it does
Tracks **who knows what** about each IT system or service the team supports. The goal is simple: if one person leaves, the team must not lose critical knowledge.

### Expertise levels
| Level | Meaning |
|-------|---------|
| **EXPERT** | Can design, troubleshoot, and mentor |
| **ADVANCED** | Operates independently |
| **BASIC** | Needs guidance |
| **NONE** | No knowledge |

### Typical flow
1. **Lead** → `POST /api/systems` to register a new system (e.g., "Payment Gateway").
2. **Lead** → `POST /api/systems/{systemId}/expertise` to assign levels to team members.
3. **Lead** → `GET /api/systems/alerts/team/{teamId}` → see at‑risk systems.
4. **Lead** → `GET /api/systems/knowledge/team/{teamId}` → full matrix of people per system.

### Alert rule
A system is **at risk** if:
- There are **zero EXPERTs**, **OR**
- There is **only one EXPERT** and the system is **HIGH/CRITICAL**.

---

## 7. Module E — Pulse Surveys

### What it does
A **2‑minute pre‑1:1 check‑in** where the employee answers:
- Energy level (1–5)
- What is draining them most (`burdenNote`)
- Topics they want to discuss at the 1:1

### Typical flow
1. **Employee** → `POST /api/pulse` a few hours before the 1:1.
2. **Lead** → opens meeting view and sees the latest pulse data.
3. **Lead** → `GET /api/pulse/team/{teamId}/aggregate?daysBack=30` for team health snapshot.
4. **Lead** → `GET /api/pulse/team/{teamId}/trend?granularity=week&daysBack=90` to spot energy decline early.

---

## 8. For Developers — Architecture Overview

### Package structure
```
com.teamui
├── domain          # Entities + Repositories + Enums (per feature)
│   ├── user
│   ├── meeting
│   ├── event
│   ├── competency
│   ├── system
│   ├── pulse
│   └── ...
├── dto             # Java Records with Jakarta Validation
├── service         # Transactional business logic
├── web             # REST controllers
├── security        # JWT, Spring Security config
└── exception       # Global exception handler
```

### Adding a new module
1. Create entity + repository under `domain/<feature>/`.
2. Create DTO records under `dto/`.
3. Create `<Feature>Service` under `service/`.
4. Create `<Feature>Controller` under `web/`.
5. Add URL patterns to `SecurityConfig` if role‑based restrictions needed.
6. Write Flyway migration under `resources/db/migration/`.

### Key patterns
- **Optimistic locking**: `@Version Long version` on entities that support concurrent edits.
- **Soft deletes**: `left_at` + `@SQLRestriction("left_at IS NULL")` on memberships.
- **Visibility**: service layer filters fields based on viewer role (not DB‑level).
- **Pageable**: all list queries accept `Pageable` for pagination.

---

## 9. API Quick Reference

| Area | Base | Methods |
|------|------|---------|
| Auth | `/api/auth/**` | POST login, register |
| Meetings | `/api/meetings` | CRUD + notes + actions |
| Events | `/api/events` | CRUD + list by user/team |
| Competencies | `/api/competencies` | scores, radar, history, trend, benchmark |
| Bus Factor | `/api/systems` | systems, expertise, alerts, knowledge matrix |
| Pulse | `/api/pulse` | submit, list, aggregate, trend |

> All endpoints except `/api/auth/**` and `/actuator/health` require a valid `Authorization: Bearer <JWT>` header.

---

## 10. Glossary

| Term | Meaning |
|------|---------|
| **1:1** | One‑on‑one meeting between lead and employee |
| **Pulse** | Short pre‑meeting survey |
| **Radar** | Star chart showing competency scores |
| **Bus Factor** | Minimum number of people who can ruin a project if hit by a bus |
| **Timeline** | Chronological history of events for an employee |
| **Shared notes** | Meeting notes visible to both leads |
| **Private notes** | Meeting notes visible only to the author |

---

*Document version: 1.0*
*Last updated: 2026-08-31*

-- ============================================
-- TeamUI MVP — Модель данных (Rev 2026)
-- PostgreSQL 16+
-- Spring Boot 4.1.0 + Flyway
-- Java 21
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- 1. Пользователи
-- ============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    hired_at        DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 2. Стримы (Stream)
-- ============================================
CREATE TABLE streams (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(50) DEFAULT 'SUPPORT',
    stream_lead_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    stream_it_lead_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 3. Кластеры (Cluster)
-- ============================================
CREATE TABLE clusters (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    stream_id       UUID NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
    cluster_lead_id UUID REFERENCES users(id) ON DELETE SET NULL,
    cluster_it_lead_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 4. Команды (Team)
-- ============================================
CREATE TABLE teams (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    cluster_id      UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 5. Членство + роли
-- ============================================
CREATE TABLE team_memberships (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL CHECK (role IN ('TEAM_MEMBER', 'TEAM_LEAD', 'IT_LEAD')),
    joined_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    left_at         TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, team_id, role)
);

-- ============================================
-- 6. Пульс-сурвей (Pulse) — для Check-in перед 1:1
-- ============================================
CREATE TABLE pulse_surveys (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    energy_score    SMALLINT CHECK (energy_score BETWEEN 1 AND 5),
    burden_note     TEXT,                                      -- что больше всего высаживает
    topics          TEXT,                                      -- темы для 1:1 (free-form)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 7. 1:1 Встречи
-- ============================================
CREATE TABLE meetings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    participant_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lead_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    scheduled_date  TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PLANNED'
                                    CHECK (status IN ('PLANNED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    energy_score    SMALLINT CHECK (energy_score BETWEEN 1 AND 5),
    energy_note     TEXT,
    shared_notes    TEXT,
    private_notes   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 8. Actions (договорённости)
-- ============================================
CREATE TABLE meeting_actions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    meeting_id      UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    text            TEXT NOT NULL,
    owner_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date        DATE,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN'
                                    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 9. Events (достижения, заметки, обучение)
-- ============================================
CREATE TABLE events (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_by          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id             UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    event_type          VARCHAR(50) NOT NULL
                            CHECK (event_type IN ('ACHIEVEMENT', 'NOTE', 'TRAINING', 'INCIDENT', 'LATENESS', 'TASK_MISS')),
    category            VARCHAR(100),
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    impact_level        VARCHAR(50) CHECK (impact_level IN ('POSITIVE_LOW', 'POSITIVE_HIGH', 'NEGATIVE_LOW', 'NEGATIVE_HIGH')),
    is_visible_to_employee BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 10. Внешние доказательства (для будущей интеграции Git/Jira/Alertmanager)
-- ============================================
CREATE TABLE external_evidences (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    source          VARCHAR(100) NOT NULL CHECK (source IN ('GIT', 'JIRA', 'SLACK', 'ALERTMANAGER', 'MANUAL')),
    source_id       VARCHAR(255),                              -- внешний ID (commit hash, ticket key)
    event_type      VARCHAR(50) NOT NULL
                            CHECK (event_type IN ('INCIDENT', 'MERGE_REQUEST', 'DOCUMENTATION', 'TRAINING')),
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    occurred_at     TIMESTAMP WITH TIME ZONE,
    raw_data        JSONB,                                      -- произвольные данные из внешней системы
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 11. Компетенции (оси звёздной модели)
-- ============================================
CREATE TABLE competencies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    sort_order      INT NOT NULL DEFAULT 0
);

-- Seed: 7 компетенций для сопровождения (включая AI)
INSERT INTO competencies (name, description, sort_order) VALUES
    ('Техническое расследование', 'Умение быстро понять, что сломалось, по крупицам восстановить картину', 1),
    ('Управление инцидентом', 'Работа под давлением: от принятия тревоги до полного восстановления', 2),
    ('Коммуникация и документирование', 'Передача информации так, чтобы следующий не начинал с нуля', 3),
    ('Платформенное мышление и процессы', 'Создание self-service, автоматизация, масштабируемые решения', 4),
    ('Командная устойчивость (Bus Factor)', 'Снижение рисков: чтобы уход одного не означал потерю знаний', 5),
    ('Самоуправление и развитие', 'Проактивность: когда сотрудник сам тянет процесс', 6),
    ('AI и автоматизация', 'Применение генеративных моделей и скриптов для работы', 7);

-- ============================================
-- 12. Оценки компетенций (self + lead)
-- ============================================
CREATE TABLE user_competency_scores (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    competency_id   UUID NOT NULL REFERENCES competencies(id) ON DELETE CASCADE,
    self_score      SMALLINT CHECK (self_score BETWEEN 1 AND 5),
    lead_score      SMALLINT CHECK (lead_score BETWEEN 1 AND 5),
    scored_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    comment         TEXT,
    scored_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, competency_id)
);

-- ============================================
-- 13. Системы (для Bus Factor)
-- ============================================
CREATE TABLE systems (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    criticality     VARCHAR(50) DEFAULT 'HIGH' CHECK (criticality IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- 14. Экспертиза по системам (Bus Factor map)
-- ============================================
CREATE TABLE system_expertise (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    system_id       UUID NOT NULL REFERENCES systems(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    level           VARCHAR(50) NOT NULL CHECK (level IN ('EXPERT', 'ADVANCED', 'BASIC', 'NONE')),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(system_id, user_id)
);

-- ============================================
-- 15. Приглашения (Invites)
-- ============================================
CREATE TABLE invites (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    role            VARCHAR(50) NOT NULL,
    team_id         UUID REFERENCES teams(id) ON DELETE SET NULL,
    invited_by      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    used_at         TIMESTAMP WITH TIME ZONE,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- ИNDEXES
-- ============================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_team_memberships_user ON team_memberships(user_id);
CREATE INDEX idx_team_memberships_team_active ON team_memberships(team_id) WHERE left_at IS NULL;
CREATE INDEX idx_pulse_user ON pulse_surveys(user_id, created_at DESC);
CREATE INDEX idx_meetings_participant ON meetings(participant_id);
CREATE INDEX idx_meetings_lead ON meetings(lead_id);
CREATE INDEX idx_meetings_team_date ON meetings(team_id, scheduled_date DESC);
CREATE INDEX idx_meetings_status ON meetings(status);
CREATE INDEX idx_actions_owner_status ON meeting_actions(owner_id, status, due_date);
CREATE INDEX idx_actions_meeting ON meeting_actions(meeting_id);
CREATE INDEX idx_events_user ON events(user_id, created_at DESC);
CREATE INDEX idx_events_team_type ON events(team_id, event_type);
CREATE INDEX idx_external_user ON external_evidences(user_id);
CREATE INDEX idx_expertise_system ON system_expertise(system_id);
CREATE INDEX idx_expertise_user ON system_expertise(user_id);

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE meetings IS '1:1 встречи. shared_notes редактирует только lead_id. private_notes видны только lead_id.';
COMMENT ON COLUMN meetings.shared_notes IS 'Общие заметки. Видны обоим лидам команды. Редактирует только lead_id.';
COMMENT ON COLUMN meetings.private_notes IS 'Приватные заметки. Только lead_id. Вечный доступ. Delete на его совести.';
COMMENT ON TABLE external_evidences IS 'Сырые данные из внешних систем для будущей интеграции. Сейчас не используется в бизнес-логике.';
COMMENT ON TABLE pulse_surveys IS 'Imпульсные сурвей перед 1:1. Сотрудник отвечает за 2 минуты.';

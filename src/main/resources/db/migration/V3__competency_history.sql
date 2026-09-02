-- ============================================
-- TeamUI — Competency score history & team radar support
-- ============================================

CREATE TABLE user_competency_score_history (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    competency_id   UUID NOT NULL REFERENCES competencies(id) ON DELETE CASCADE,
    self_score      SMALLINT CHECK (self_score BETWEEN 1 AND 5),
    lead_score      SMALLINT CHECK (lead_score BETWEEN 1 AND 5),
    scored_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    comment         TEXT,
    scored_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comp_history_user_comp ON user_competency_score_history(user_id, competency_id, scored_at DESC);
CREATE INDEX idx_comp_history_scored_at ON user_competency_score_history(scored_at DESC);

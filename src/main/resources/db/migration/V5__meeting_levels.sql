-- ============================================
-- 5. Встречи на уровнях стрима и кластера
-- ============================================
-- Иерархия 1:1: Stream Lead -> Cluster Lead -> Team Lead -> Team Member.
-- Встреча принадлежит кластеру (cluster_id). Команда (team_id) нужна для уровней TEAM и CLUSTER.
-- На уровне STREAM команды нет.

ALTER TABLE meetings ALTER COLUMN team_id DROP NOT NULL;

ALTER TABLE meetings
    ADD COLUMN level VARCHAR(20) NOT NULL DEFAULT 'TEAM'
        CHECK (level IN ('TEAM', 'CLUSTER', 'STREAM'));

ALTER TABLE meetings
    ADD COLUMN cluster_id UUID REFERENCES clusters(id) ON DELETE CASCADE;

-- Существующие встречи относятся к командному уровню: кластер берём у команды
UPDATE meetings m
SET cluster_id = t.cluster_id
FROM teams t
WHERE t.id = m.team_id;

ALTER TABLE meetings ALTER COLUMN cluster_id SET NOT NULL;

ALTER TABLE meetings
    ADD CONSTRAINT meetings_team_by_level
        CHECK ((level = 'STREAM' AND team_id IS NULL) OR (level <> 'STREAM' AND team_id IS NOT NULL));

CREATE INDEX idx_meetings_cluster ON meetings(cluster_id);

// Типы повторяют records бэкенда (com.teamui.dto). Менять вместе с DTO.

export type MeetingStatus = 'PLANNED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type MeetingLevel = 'TEAM' | 'CLUSTER' | 'STREAM';
export type ActionStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

/** Конверт всех ответов API. Клиент (client.ts) разворачивает `data`, компоненты его не видят. */
export interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
}

export interface LoginResult {
  token: string;
  tokenType: string;
  userId: string;
  email: string;
  role: string;
  expiresIn: number;
}

export interface UserProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl: string | null;
}

export interface TeamMemberCard {
  id: string;
  fullName: string;
  avatarUrl: string | null;
  role: string;
  lastOneOnOneDate: string | null;
  openActionCount: number;
  competencyAverage: number | null;
}

export interface MeetingPreview {
  id: string;
  participantName: string;
  leadName: string;
  scheduledDate: string;
  status: MeetingStatus;
  energyScore: number | null;
}

/** Задача из блока «Мои действия» (com.teamui.dto.QuickActionDto). */
export interface QuickAction {
  id: string;
  text: string;
  ownerId: string;
  dueDate: string | null;
  overdue: boolean;
  source: string;
}

/** Блок участника на дашборде (com.teamui.dto.MemberMetricsDto), без радара и событий. */
export interface MemberMetrics {
  lastMeetings: MeetingPreview[];
  upcomingMeeting: MeetingPreview | null;
  openActions: QuickAction[];
}

export interface TeamMiniSummary {
  id: string;
  name: string;
  memberCount: number;
  overdueActions: number;
  avgEnergy: number | null;
  atRisk: boolean;
}

/** Здоровье кластера (com.teamui.dto.ClusterSummaryDto). */
export interface ClusterSummary {
  id: string;
  name: string;
  teamCount: number;
  memberCount: number;
  overdueActions: number;
  busFactorWarnings: number;
  teamSummaries: TeamMiniSummary[];
}

/** Агрегат стрима для STREAM_LEAD / STREAM_IT_LEAD (com.teamui.dto.StreamOverviewDto). */
export interface StreamOverview {
  streamName: string;
  clusterSummaries: ClusterSummary[];
  totalTeams: number;
  totalMembers: number;
  teamsAtRisk: number;
}

export interface Dashboard {
  userProfile: UserProfile;
  /** null, если пользователю не назначена роль в команде, кластере или стриме. */
  role: string | null;
  /** Название команды для участника и лидов команды, иначе null. */
  teamName: string | null;
  memberMetrics: MemberMetrics | null;
  teamHealth: {
    teamName: string;
    upcomingMeetings: MeetingPreview[];
    memberCards: TeamMemberCard[];
  } | null;
  streamOverview: StreamOverview | null;
  clusterOverview: ClusterSummary | null;
}

export interface ActionItem {
  id: string;
  meetingId: string;
  text: string;
  ownerId: string;
  ownerName: string;
  dueDate: string | null;
  status: ActionStatus;
  version: number;
}

export interface Meeting {
  id: string;
  participantId: string;
  participantName: string;
  leadId: string;
  leadName: string;
  /** null для встреч уровня STREAM. */
  teamId: string | null;
  level: MeetingLevel;
  clusterId: string;
  scheduledDate: string;
  status: MeetingStatus;
  energyScore: number | null;
  energyNote: string | null;
  sharedNotes: string | null;
  privateNotes: string | null;
  actions: ActionItem[];
  version: number;
}

/** Человек, с которым можно назначить встречу (com.teamui.dto.MeetingParticipantDto). */
export interface MeetingParticipant {
  id: string;
  fullName: string;
  level: MeetingLevel;
  /** Название команды для TEAM и CLUSTER, название кластера для STREAM. */
  scopeName: string;
}

/** Страница списка из `data` конверта (com.teamui.dto.PageResponse). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateMeetingBody {
  participantId: string;
  scheduledDate: string;
  status?: MeetingStatus;
}

/** Тело PUT /api/meetings/{id}: меняет встречу автор. */
export interface UpdateMeetingBody {
  scheduledDate: string;
  status: MeetingStatus;
  energyScore: number | null;
  energyNote: string | null;
  version: number;
}

export interface UpdateNotesBody {
  sharedNotes: string | null;
  privateNotes: string | null;
  version: number;
}

export interface ActionBody {
  text: string;
  ownerId: string;
  dueDate?: string | null;
  status?: ActionStatus;
}

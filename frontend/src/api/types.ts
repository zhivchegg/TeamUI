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

/** Риск потери экспертизы по системе (com.teamui.dto.BusFactorAlertDto). */
export interface BusFactorAlert {
  systemId: string;
  systemName: string;
  criticality: string;
  expertCount: number;
  advancedCount: number;
  totalMembers: number;
  atRisk: boolean;
  expertNames: string[];
}

/** Событие из таймлайна (com.teamui.dto.TimelineEventPreviewDto). */
export interface TimelineEvent {
  id: string;
  eventType: string;
  title: string;
  category: string;
  impactLevel: string;
  createdAt: string;
}

/** Сводка команды для лидов (com.teamui.dto.TeamHealthDto). */
export interface TeamHealth {
  teamName: string;
  upcomingMeetings: MeetingPreview[];
  overdueActions: QuickAction[];
  memberCards: TeamMemberCard[];
  busFactorAlerts: BusFactorAlert[];
  teamEnergyAverage: number | null;
  unacknowledgedEvents: TimelineEvent[];
}

/** Система с людьми по уровню экспертизы (com.teamui.dto.SystemWithPeopleDto). */
export interface SystemWithPeople {
  id: string;
  name: string;
  criticality: string;
  experts: string[];
  advanced: string[];
  basic: string[];
}

export type ExpertiseLevel = 'EXPERT' | 'ADVANCED' | 'BASIC' | 'NONE';
export type Criticality = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type EventType = 'ACHIEVEMENT' | 'NOTE' | 'TRAINING' | 'INCIDENT' | 'LATENESS' | 'TASK_MISS';
export type ImpactLevel = 'POSITIVE_LOW' | 'POSITIVE_HIGH' | 'NEGATIVE_LOW' | 'NEGATIVE_HIGH';

export interface PulseBody {
  energyScore: number;
  burdenNote: string | null;
  topics: string | null;
}

export interface CreateEventBody {
  userId: string;
  teamId: string;
  eventType: EventType;
  category: string | null;
  title: string;
  impactLevel: ImpactLevel | null;
  visibleToEmployee: boolean;
}

export interface CreateSystemBody {
  name: string;
  description: string | null;
  criticality: Criticality;
  teamId: string;
}

/** Уведомление пользователя (com.teamui.dto.NotificationResponse). */
export interface NotificationItem {
  id: string;
  text: string;
  meetingId: string | null;
  read: boolean;
  createdAt: string;
}

export interface Dashboard {
  userProfile: UserProfile;
  /** null, если пользователю не назначена роль в команде, кластере или стриме. */
  role: string | null;
  /** Название команды для участника и лидов команды, иначе null. */
  teamName: string | null;
  /** Идентификатор команды для участника и лидов команды, иначе null. */
  teamId: string | null;
  memberMetrics: MemberMetrics | null;
  teamHealth: TeamHealth | null;
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
  /** Вопросы участника к встрече. Видят участник и лиды. */
  participantAgenda: string | null;
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

/** Тело PUT /api/meetings/{id}/agenda: повестку пишет только участник. */
export interface UpdateAgendaBody {
  agenda: string | null;
  version: number;
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

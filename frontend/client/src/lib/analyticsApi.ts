import { extractApiErrorMessage } from "@/lib/apiError";

export interface DashboardOverviewDto {
  departments: number;
  users: number;
  activeUsers: number;
  courses: number;
  completedCourses: number;
  averageProgress: number;
  completionRate: number;
  generatedAt: string;
}

export interface DepartmentProgressDto {
  departmentId: string;
  departmentName: string;
  totalUsers: number;
  activeUsers: number;
  averageProgress: number;
  completionRate: number;
}

export interface CourseStatsDto {
  courseId: string;
  courseTitle: string;
  totalEnrollments: number;
  completions: number;
  lessonsCompleted: number;
  averageProgress: number;
  completionRate: number;
}

export interface UserEngagementDto {
  userId: string;
  userName: string;
  departmentName: string;
  assignedCourses: number;
  completedLessons: number;
  averageProgress: number;
  active: boolean;
}

export interface NotificationEffectivenessDto {
  type: string;
  channel: string;
  sentCount: number;
  readCount: number;
  deliveryRate: number;
}

export interface LearningReportDto {
  id: string;
  reportType: string;
  generatedAt: string;
  generatedBy?: string | null;
  format: string;
  data: unknown;
}

export interface AnalyticsDashboardDto {
  overview: DashboardOverviewDto;
  departmentProgress: DepartmentProgressDto[];
  courseStats: CourseStatsDto[];
  userEngagement: UserEngagementDto[];
  notificationEffectiveness: NotificationEffectivenessDto[];
  reports: LearningReportDto[];
}

const ANALYTICS_API_URL =
  import.meta.env.VITE_ANALYTICS_API_URL ?? "http://localhost:8080";

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem("auth_token");
  const headers = new Headers(init?.headers);
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 15000);
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  try {
    const res = await fetch(`${ANALYTICS_API_URL}${path}`, { ...init, headers, signal: controller.signal });
    if (!res.ok) {
      throw new Error(await extractApiErrorMessage(res));
    }
    return (await res.json()) as T;
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new Error("Сервис аналитики не ответил за 15 секунд.");
    }
    throw error;
  } finally {
    window.clearTimeout(timeout);
  }
}

export function fetchAnalyticsOverview(): Promise<DashboardOverviewDto> {
  return fetchJson<DashboardOverviewDto>("/analytics/dashboard/overview");
}

export function fetchAnalyticsDashboard(): Promise<AnalyticsDashboardDto> {
  return fetchJson<AnalyticsDashboardDto>("/analytics/dashboard");
}

export function fetchDepartmentProgress(): Promise<DepartmentProgressDto[]> {
  return fetchJson<DepartmentProgressDto[]>("/analytics/department-progress");
}

export function fetchCourseStats(): Promise<CourseStatsDto[]> {
  return fetchJson<CourseStatsDto[]>("/analytics/course-stats");
}

export function fetchUserEngagement(): Promise<UserEngagementDto[]> {
  return fetchJson<UserEngagementDto[]>("/analytics/user-engagement");
}

export function fetchNotificationEffectiveness(): Promise<NotificationEffectivenessDto[]> {
  return fetchJson<NotificationEffectivenessDto[]>("/analytics/notification-effectiveness");
}

export function fetchAnalyticsReports(): Promise<LearningReportDto[]> {
  return fetchJson<LearningReportDto[]>("/analytics/reports");
}

export function generateAnalyticsReport(payload = { reportType: "dashboard-overview", format: "json" }): Promise<LearningReportDto> {
  return fetchJson<LearningReportDto>("/analytics/reports/generate", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

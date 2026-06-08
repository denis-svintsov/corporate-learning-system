import { extractApiErrorMessage } from "@/lib/apiError";

export interface UserProfileDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  positionId?: string | null;
  positionTitle?: string | null;
  departmentId?: string | null;
  departmentName?: string | null;
  hireDate?: string | null;
  status: string;
  roles: string[];
}

export interface UserSettingsDto {
  userId: string;
  emailNotifications: boolean;
  pushNotifications: boolean;
  language: string;
  timezone: string;
}

export interface PositionDto {
  positionId: string;
  title: string;
  description?: string | null;
  grade?: string | null;
}

export interface DepartmentDto {
  departmentId: string;
  name: string;
  description?: string | null;
  managerId?: string | null;
  parentDepartmentId?: string | null;
}

export interface UserCourseProgressDto {
  courseId: string;
  courseTitle: string;
  completedLessons: number;
  totalLessons: number;
  progressPercentage: number;
}

export interface ProgressSummaryDto {
  userId: string;
  courses: UserCourseProgressDto[];
}

export interface LearningHistoryDto {
  id: string;
  action: string;
  timestamp: string;
  details?: string | null;
}

export interface UserCabinetDto {
  progress: ProgressSummaryDto;
  history: LearningHistoryDto[];
}

const USERS_API_URL =
  import.meta.env.VITE_USERS_API_URL ?? "http://localhost:8080";

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem("auth_token");
  const headers = new Headers(init?.headers);
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(url, { ...init, headers });
  if (!res.ok) {
    throw new Error(await extractApiErrorMessage(res));
  }
  return (await res.json()) as T;
}

export async function fetchUserProfile(userId: string): Promise<UserProfileDto> {
  return fetchJson<UserProfileDto>(`${USERS_API_URL}/users/${userId}`);
}

export async function fetchPositions(): Promise<PositionDto[]> {
  return fetchJson<PositionDto[]>(`${USERS_API_URL}/positions`);
}

export async function fetchDepartments(): Promise<DepartmentDto[]> {
  return fetchJson<DepartmentDto[]>(`${USERS_API_URL}/departments`);
}

export async function fetchUserCabinet(historyLimit = 20): Promise<UserCabinetDto> {
  return fetchJson<UserCabinetDto>(`${USERS_API_URL}/users/cabinet?historyLimit=${historyLimit}`);
}

export async function fetchUserCabinetProgress(): Promise<ProgressSummaryDto> {
  return fetchJson<ProgressSummaryDto>(`${USERS_API_URL}/users/cabinet/progress`);
}

export async function fetchUserCabinetHistory(limit = 20): Promise<LearningHistoryDto[]> {
  return fetchJson<LearningHistoryDto[]>(`${USERS_API_URL}/users/cabinet/history?limit=${limit}`);
}

export async function fetchUserSettings(userId: string): Promise<UserSettingsDto> {
  return fetchJson<UserSettingsDto>(`${USERS_API_URL}/users/${userId}/settings`);
}

export async function updateUserSettings(
  userId: string,
  payload: Partial<Pick<UserSettingsDto, "emailNotifications" | "pushNotifications" | "language" | "timezone">>,
): Promise<UserSettingsDto> {
  return fetchJson<UserSettingsDto>(`${USERS_API_URL}/users/${userId}/settings`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

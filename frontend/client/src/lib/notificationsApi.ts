import { extractApiErrorMessage } from "@/lib/apiError";

export type NotificationType = "COURSE_ASSIGNED" | "ASSIGNMENT_REQUESTED" | "LESSON_COMPLETED" | "COURSE_COMPLETED" | "SYSTEM";
export type NotificationChannel = "IN_APP" | "EMAIL" | "TELEGRAM";
export type NotificationStatus = "UNREAD" | "READ";

export interface NotificationDto {
  id: string;
  userId: string;
  type: NotificationType;
  channel: NotificationChannel;
  status: NotificationStatus;
  title: string;
  message: string;
  sourceService?: string | null;
  sourceId?: string | null;
  createdAt: string;
  readAt?: string | null;
}

export interface UnreadCountDto {
  unreadCount: number;
}

const NOTIFICATIONS_API_URL =
  import.meta.env.VITE_NOTIFICATIONS_API_URL ?? window.location.origin;

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem("auth_token");
  const headers = new Headers(init?.headers);
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(`${NOTIFICATIONS_API_URL}${path}`, { ...init, headers });
  if (!res.ok) {
    throw new Error(await extractApiErrorMessage(res));
  }
  if (res.status === 204) {
    return undefined as T;
  }
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export function fetchMyNotifications(): Promise<NotificationDto[]> {
  return fetchJson<NotificationDto[]>("/notifications/my");
}

export function fetchUnreadNotificationsCount(): Promise<UnreadCountDto> {
  return fetchJson<UnreadCountDto>("/notifications/unread-count");
}

export function markNotificationRead(id: string): Promise<NotificationDto> {
  return fetchJson<NotificationDto>(`/notifications/${id}/read`, {
    method: "PATCH",
  });
}

export function markAllNotificationsRead(): Promise<void> {
  return fetchJson<void>("/notifications/read-all", {
    method: "PATCH",
  });
}

export function buildNotificationStreamUrl(token: string): string {
  const params = new URLSearchParams({ token });
  return `${NOTIFICATIONS_API_URL}/notifications/stream?${params.toString()}`;
}

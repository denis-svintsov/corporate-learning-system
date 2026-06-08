import { extractApiErrorMessage } from "@/lib/apiError";

type ChatRoomType = "COURSE" | "GENERAL" | "PRIVATE";

export interface ChatRoomDto {
  id: string;
  courseId?: string | null;
  name: string;
  type: ChatRoomType;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface MessageDto {
  id: string;
  roomId: string;
  userId: string;
  content: string;
  timestamp: string;
  messageType: "TEXT" | "SYSTEM" | "FILE";
  attachments?: string[];
}

export interface ChatParticipantDto {
  userId: string;
  roomId: string;
  joinedAt: string;
  role: "PARTICIPANT" | "CURATOR" | "INSTRUCTOR";
  online: boolean;
}

const CHAT_API_URL = import.meta.env.VITE_COMMUNICATION_API_URL ?? "http://localhost:8080";

function getStoredUserId(): string | null {
  const raw = localStorage.getItem("auth_user");
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as { id?: string | null };
    const id = parsed?.id?.trim();
    return id || null;
  } catch {
    return null;
  }
}

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem("auth_token");
  const userId = getStoredUserId();
  const headers = new Headers(init?.headers);
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (userId && !headers.has("X-User-Id")) {
    headers.set("X-User-Id", userId);
  }
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const res = await fetch(url, { ...init, headers });
  if (!res.ok) {
    throw new Error(await extractApiErrorMessage(res));
  }
  return (await res.json()) as T;
}

export function fetchChatRooms(): Promise<ChatRoomDto[]> {
  return fetchJson<ChatRoomDto[]>(`${CHAT_API_URL}/chat/rooms`);
}

export function fetchRoomMessages(roomId: string, limit = 100): Promise<MessageDto[]> {
  return fetchJson<MessageDto[]>(`${CHAT_API_URL}/chat/rooms/${roomId}/messages?limit=${limit}`);
}

export function sendRoomMessage(roomId: string, content: string): Promise<MessageDto> {
  return fetchJson<MessageDto>(`${CHAT_API_URL}/chat/rooms/${roomId}/messages`, {
    method: "POST",
    body: JSON.stringify({
      content,
      messageType: "TEXT",
      attachments: [],
    }),
  });
}

export function fetchRoomParticipants(roomId: string): Promise<ChatParticipantDto[]> {
  return fetchJson<ChatParticipantDto[]>(`${CHAT_API_URL}/chat/rooms/${roomId}/participants`);
}

export function joinCourseRoom(courseId: string): Promise<ChatRoomDto> {
  return fetchJson<ChatRoomDto>(`${CHAT_API_URL}/chat/rooms/course/${courseId}/join`, {
    method: "POST",
  });
}

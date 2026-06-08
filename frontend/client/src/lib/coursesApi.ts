import { extractApiErrorMessage } from "@/lib/apiError";

export type CourseStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";
export type DifficultyLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export interface CourseDto {
  id: string;
  title: string;
  description?: string;
  categoryId?: string | null;
  difficulty?: DifficultyLevel | null;
  durationMinutes?: number | null;
  status?: CourseStatus | null;
  tagIds?: string[];
  allowedRoles?: string[];
  allowedDepartmentIds?: string[];
  specializations?: string[];
  instructions?: string | null;
  aggregatorUrl?: string | null;
  coverUrl?: string | null;
  companyCost?: number | null;
  partnerName?: string | null;
  partnerLocation?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

export type AttendanceStatus = "PRESENT" | "ABSENT" | "EXCUSED";

export interface CourseParticipantDto {
  userId: string;
  assignmentId: string;
  assignmentStatus?: string | null;
  dueDate?: string | null;
  assignedAt?: string | null;
}

export interface AttendanceDto {
  id: string;
  courseId: string;
  userId: string;
  attendanceDate: string;
  status: AttendanceStatus;
  comment?: string | null;
  markedBy: string;
  updatedAt?: string | null;
}

export type CoursePayload = {
  title: string;
  description?: string;
  categoryId?: string | null;
  difficulty: DifficultyLevel;
  durationMinutes?: number | null;
  status?: CourseStatus;
  tagIds?: string[];
  allowedRoles?: string[];
  allowedDepartmentIds?: string[];
  specializations?: string[];
  instructions?: string | null;
  aggregatorUrl?: string | null;
  coverUrl?: string | null;
  companyCost?: number | null;
  partnerName?: string | null;
  partnerLocation?: string | null;
  startDate?: string | null;
  endDate?: string | null;
};

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ProgressSummaryDto {
  userId: string;
  courses: {
    courseId: string;
    courseTitle: string;
    completedLessons: number;
    totalLessons: number;
    progressPercentage: number;
  }[];
}

export interface CertificateDto {
  id: string;
  courseId: string;
  issueDate: string;
  certificateUrl: string;
  hash: string;
}

export interface AssignedCourseDto {
  id: string;
  userId: string;
  courseId: string;
  courseTitle?: string | null;
  courseDescription?: string | null;
  courseCategoryId?: string | null;
  courseDifficulty?: DifficultyLevel | null;
  courseDurationMinutes?: number | null;
  courseStatus?: CourseStatus | null;
  courseCoverUrl?: string | null;
  courseAggregatorUrl?: string | null;
  courseInstructions?: string | null;
  courseSpecializations?: string[];
  courseCompanyCost?: number | null;
  courseStartDate?: string | null;
  courseEndDate?: string | null;
  assignedBy?: string | null;
  dueDate?: string | null;
  status?: string | null;
  createdAt?: string | null;
}

export type AssignmentRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface AssignmentRequestDto {
  id: string;
  userId: string;
  requestedBy: string;
  courseId: string;
  courseTitle?: string | null;
  courseDifficulty?: DifficultyLevel | null;
  courseDurationMinutes?: number | null;
  courseCompanyCost?: number | null;
  comment?: string | null;
  dueDate?: string | null;
  status: AssignmentRequestStatus;
  reviewedBy?: string | null;
  reviewerComment?: string | null;
  createdAt?: string | null;
  reviewedAt?: string | null;
}

export interface AssignmentPolicyDto {
  maxCoursesPerQuarter: number;
  updatedAt?: string | null;
}

const COURSES_API_URL =
  import.meta.env.VITE_COURSES_API_URL ?? "http://localhost:8080";

function buildUrl(path: string, params?: Record<string, string | number | undefined | null>) {
  const url = new URL(path, COURSES_API_URL);
  if (!params) return url.toString();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    url.searchParams.set(key, String(value));
  });
  return url.toString();
}

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

export async function fetchCourses(params?: {
  q?: string;
  categoryId?: string;
  difficulty?: DifficultyLevel;
  status?: CourseStatus;
  tagId?: string;
  positionId?: string;
  page?: number;
  size?: number;
}): Promise<Page<CourseDto>> {
  const url = buildUrl("/courses", params);
  return fetchJson<Page<CourseDto>>(url);
}

export async function fetchManagedCourses(params?: {
  q?: string;
  difficulty?: DifficultyLevel;
  status?: CourseStatus;
  page?: number;
  size?: number;
}): Promise<Page<CourseDto>> {
  const url = buildUrl("/courses/manage", params);
  return fetchJson<Page<CourseDto>>(url);
}

export async function createCourse(payload: CoursePayload): Promise<CourseDto> {
  return fetchJson<CourseDto>(buildUrl("/courses"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function uploadCourseCover(file: File): Promise<{ objectKey: string; coverUrl: string }> {
  const token = localStorage.getItem("auth_token");
  const formData = new FormData();
  formData.append("file", file);
  const headers = new Headers();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(buildUrl("/courses/covers"), {
    method: "POST",
    headers,
    body: formData,
  });
  if (!res.ok) {
    throw new Error(await extractApiErrorMessage(res));
  }
  const data = (await res.json()) as { objectKey: string; coverUrl: string };
  return {
    ...data,
    coverUrl: new URL(data.coverUrl, COURSES_API_URL).toString(),
  };
}

export async function updateCourse(id: string, payload: CoursePayload & { status: CourseStatus }): Promise<CourseDto> {
  return fetchJson<CourseDto>(buildUrl(`/courses/${id}`), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export async function fetchRecommendedCourses(params?: {
  page?: number;
  size?: number;
}): Promise<Page<CourseDto>> {
  const url = buildUrl("/courses/recommended", params);
  return fetchJson<Page<CourseDto>>(url);
}

export async function fetchCourse(id: string): Promise<CourseDto> {
  return fetchJson<CourseDto>(buildUrl(`/courses/${id}`));
}

export async function fetchAssignedCourses(): Promise<AssignedCourseDto[]> {
  return fetchJson<AssignedCourseDto[]>(buildUrl("/courses/assigned-courses/my"));
}

export async function fetchCourseParticipants(courseId: string): Promise<CourseParticipantDto[]> {
  return fetchJson<CourseParticipantDto[]>(buildUrl(`/courses/${courseId}/participants`));
}

export async function fetchCourseAttendance(courseId: string, date: string): Promise<AttendanceDto[]> {
  return fetchJson<AttendanceDto[]>(buildUrl(`/courses/${courseId}/attendance`, { date }));
}

export async function markCourseAttendance(courseId: string, request: {
  userId: string;
  attendanceDate: string;
  status: AttendanceStatus;
  comment?: string;
}): Promise<AttendanceDto> {
  return fetchJson<AttendanceDto>(buildUrl(`/courses/${courseId}/attendance`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function confirmCourseCompletion(courseId: string, userId: string): Promise<AssignedCourseDto> {
  return fetchJson<AssignedCourseDto>(buildUrl(`/courses/${courseId}/complete`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId }),
  });
}

export async function assignCourse(request: {
  userId: string;
  courseId: string;
  assignedBy: string;
  dueDate?: string;
}) {
  return fetchJson(buildUrl("/courses/assign"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function submitAssignmentRequest(request: {
  userId: string;
  courseId: string;
  requestedBy: string;
  comment?: string;
  dueDate?: string;
}) {
  return fetchJson<AssignmentRequestDto>(buildUrl("/courses/assignment-requests"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function fetchMyAssignmentRequests(): Promise<AssignmentRequestDto[]> {
  return fetchJson<AssignmentRequestDto[]>(buildUrl("/courses/assignment-requests/my"));
}

export async function fetchAssignmentRequests(status?: AssignmentRequestStatus): Promise<AssignmentRequestDto[]> {
  return fetchJson<AssignmentRequestDto[]>(buildUrl("/courses/assignment-requests", { status }));
}

export async function approveAssignmentRequest(requestId: string, payload?: { reviewerComment?: string; dueDate?: string }) {
  return fetchJson<AssignmentRequestDto>(buildUrl(`/courses/assignment-requests/${requestId}/approve`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload ?? {}),
  });
}

export async function rejectAssignmentRequest(requestId: string, payload?: { reviewerComment?: string }) {
  return fetchJson<AssignmentRequestDto>(buildUrl(`/courses/assignment-requests/${requestId}/reject`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload ?? {}),
  });
}

export async function fetchAssignmentPolicy(): Promise<AssignmentPolicyDto> {
  return fetchJson<AssignmentPolicyDto>(buildUrl("/courses/assignment-policy"));
}

export async function updateAssignmentPolicy(maxCoursesPerQuarter: number): Promise<AssignmentPolicyDto> {
  return fetchJson<AssignmentPolicyDto>(buildUrl("/courses/assignment-policy"), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ maxCoursesPerQuarter }),
  });
}

export async function fetchMyProgress(userId: string): Promise<ProgressSummaryDto> {
  return fetchJson<ProgressSummaryDto>(buildUrl("/progress/my"), {
    headers: { "X-User-Id": userId },
  });
}

export async function fetchMyCertificates(userId: string): Promise<CertificateDto[]> {
  return fetchJson<CertificateDto[]>(buildUrl("/certificates/my"), {
    headers: { "X-User-Id": userId },
  });
}

export async function downloadCertificate(id: string, userId: string): Promise<Blob> {
  const token = localStorage.getItem("auth_token");
  const headers = new Headers({ "X-User-Id": userId });
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(buildUrl(`/certificates/${id}`), {
    headers,
  });
  if (!res.ok) {
    throw new Error(await extractApiErrorMessage(res));
  }
  return await res.blob();
}

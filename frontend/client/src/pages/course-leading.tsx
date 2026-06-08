import { Layout } from "@/components/layout/Layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";
import {
  AttendanceDto,
  AttendanceStatus,
  CourseDto,
  CourseParticipantDto,
  confirmCourseCompletion,
  fetchCourseAttendance,
  fetchCourseParticipants,
  fetchManagedCourses,
  markCourseAttendance,
} from "@/lib/coursesApi";
import { joinCourseRoom } from "@/lib/chatApi";
import { fetchUserProfile } from "@/lib/usersApi";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarCheck, MessageSquare, Users } from "lucide-react";
import { useMemo, useState } from "react";
import { useLocation } from "wouter";

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function statusLabel(status?: AttendanceStatus) {
  if (status === "PRESENT") return "Присутствовал";
  if (status === "ABSENT") return "Отсутствовал";
  if (status === "EXCUSED") return "Уважительная причина";
  return "Не отмечен";
}

function assignmentStatusLabel(status?: string | null) {
  if (status === "COMPLETED") return "Завершен";
  if (status === "IN_PROGRESS") return "В процессе";
  if (status === "OVERDUE") return "Просрочен";
  return "Назначен";
}

function difficultyLabel(difficulty?: string | null) {
  if (difficulty === "INTERMEDIATE") return "Средний";
  if (difficulty === "ADVANCED") return "Продвинутый";
  return "Начальный";
}

export default function CourseLeadingPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [, navigate] = useLocation();
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [attendanceDate, setAttendanceDate] = useState(todayIso());
  const [comments, setComments] = useState<Record<string, string>>({});

  const canLeadCourses = (user?.roles ?? []).some((role) =>
    ["ADMIN", "HR", "TECHNOLOG", "EXPERT"].includes(role),
  );

  const coursesQuery = useQuery({
    queryKey: ["leading-courses"],
    queryFn: () => fetchManagedCourses({ status: "ACTIVE", size: 100 }),
    enabled: canLeadCourses,
  });

  const courses = coursesQuery.data?.content ?? [];
  const selectedCourse: CourseDto | null = useMemo(
    () => courses.find((course) => course.id === selectedCourseId) ?? courses[0] ?? null,
    [courses, selectedCourseId],
  );

  const participantsQuery = useQuery({
    queryKey: ["course-participants", selectedCourse?.id],
    queryFn: () => fetchCourseParticipants(selectedCourse!.id),
    enabled: !!selectedCourse?.id && canLeadCourses,
  });

  const attendanceQuery = useQuery({
    queryKey: ["course-attendance", selectedCourse?.id, attendanceDate],
    queryFn: () => fetchCourseAttendance(selectedCourse!.id, attendanceDate),
    enabled: !!selectedCourse?.id && canLeadCourses,
  });

  const participants = participantsQuery.data ?? [];
  const attendanceByUser = useMemo(() => {
    const map = new Map<string, AttendanceDto>();
    (attendanceQuery.data ?? []).forEach((item) => map.set(item.userId, item));
    return map;
  }, [attendanceQuery.data]);

  const participantIds = useMemo(
    () => Array.from(new Set(participants.map((participant) => participant.userId).filter(Boolean))),
    [participants],
  );

  const { data: participantNames = new Map<string, string>() } = useQuery({
    queryKey: ["leading-participant-profiles", participantIds],
    enabled: participantIds.length > 0,
    queryFn: async () => {
      const settled = await Promise.allSettled(
        participantIds.map(async (id) => {
          const profile = await fetchUserProfile(id);
          const fullName = [profile.lastName, profile.firstName].filter(Boolean).join(" ").trim();
          return [id, fullName || profile.email || id] as const;
        }),
      );
      const map = new Map<string, string>();
      settled.forEach((result) => {
        if (result.status === "fulfilled") {
          map.set(result.value[0], result.value[1]);
        }
      });
      return map;
    },
  });

  const markMutation = useMutation({
    mutationFn: ({ participant, status }: { participant: CourseParticipantDto; status: AttendanceStatus }) =>
      markCourseAttendance(selectedCourse!.id, {
        userId: participant.userId,
        attendanceDate,
        status,
        comment: comments[participant.userId]?.trim() || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["course-attendance", selectedCourse?.id, attendanceDate] });
    },
    onError: (error) => {
      toast({
        title: "Не удалось отметить посещаемость",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const chatMutation = useMutation({
    mutationFn: (courseId: string) => joinCourseRoom(courseId),
    onSuccess: (_room, courseId) => {
      navigate(`/chat?courseId=${courseId}`);
    },
    onError: (error) => {
      toast({
        title: "Не удалось открыть чат курса",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const completionMutation = useMutation({
    mutationFn: (participant: CourseParticipantDto) => confirmCourseCompletion(selectedCourse!.id, participant.userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["course-participants", selectedCourse?.id] });
      queryClient.invalidateQueries({ queryKey: ["assigned-courses"] });
      queryClient.invalidateQueries({ queryKey: ["certificates"] });
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
      toast({
        title: "Завершение подтверждено",
        description: "Сертификат сотрудника будет доступен на странице сертификатов.",
      });
    },
    onError: (error) => {
      toast({
        title: "Не удалось подтвердить завершение",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const handleConfirmCompletion = (participant: CourseParticipantDto) => {
    const participantName = participantNames.get(participant.userId) ?? "сотрудника";
    const courseTitle = selectedCourse?.title ?? "курс";
    const confirmed = window.confirm(
      `Подтвердить завершение курса "${courseTitle}" для ${participantName}? После этого будет создан сертификат.`,
    );
    if (confirmed) {
      completionMutation.mutate(participant);
    }
  };

  if (!canLeadCourses) {
    return (
      <Layout>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
          Доступ запрещен. Ведение курсов доступно эксперту, технологу, HR и администратору.
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-secondary">Ведение курсов</h1>
          <p className="text-muted-foreground">Участники, чат курса и учет посещаемости</p>
        </div>

        <div className="grid gap-6 xl:grid-cols-[340px_1fr]">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <CalendarCheck className="h-4 w-4" />
                Активные курсы
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {coursesQuery.isLoading && <div className="text-sm text-muted-foreground">Загрузка...</div>}
              {courses.map((course) => (
                <button
                  key={course.id}
                  className={`w-full rounded-md border p-3 text-left transition hover:bg-muted ${
                    selectedCourse?.id === course.id ? "border-primary bg-primary/5" : ""
                  }`}
                  onClick={() => setSelectedCourseId(course.id)}
                >
                  <div className="font-medium">{course.title}</div>
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Badge variant="outline">{difficultyLabel(course.difficulty)}</Badge>
                    <Badge variant="secondary">{course.startDate ?? "без даты"}</Badge>
                  </div>
                </button>
              ))}
              {!coursesQuery.isLoading && courses.length === 0 && (
                <div className="rounded-md border p-4 text-sm text-muted-foreground">Активных курсов пока нет.</div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                  <CardTitle>{selectedCourse?.title ?? "Курс не выбран"}</CardTitle>
                  <div className="mt-1 text-sm text-muted-foreground">
                    {selectedCourse?.description || "Выберите активный курс из списка."}
                  </div>
                </div>
                {selectedCourse && (
                  <Button disabled={chatMutation.isPending} onClick={() => chatMutation.mutate(selectedCourse.id)}>
                    <MessageSquare className="h-4 w-4" />
                    Чат курса
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                <div className="space-y-2">
                  <Label>Дата занятия</Label>
                  <Input type="date" value={attendanceDate} onChange={(event) => setAttendanceDate(event.target.value)} />
                </div>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Users className="h-4 w-4" />
                  Участников: {participants.length}
                </div>
              </div>

              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Участник</TableHead>
                    <TableHead>Назначение</TableHead>
                    <TableHead>Статус на дату</TableHead>
                    <TableHead>Комментарий</TableHead>
                    <TableHead className="text-right">Сохранить отметку</TableHead>
                    <TableHead className="text-right">Завершение курса</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {participants.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        У курса пока нет назначенных участников.
                      </TableCell>
                    </TableRow>
                  )}
                  {participants.map((participant) => {
                    const attendance = attendanceByUser.get(participant.userId);
                    return (
                      <TableRow key={participant.userId}>
                        <TableCell className="font-medium">
                          {participantNames.get(participant.userId) ?? participant.userId}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">{assignmentStatusLabel(participant.assignmentStatus)}</Badge>
                        </TableCell>
                        <TableCell>
                          <Select
                            value={attendance?.status ?? "PRESENT"}
                            onValueChange={(value) =>
                              markMutation.mutate({ participant, status: value as AttendanceStatus })
                            }
                          >
                            <SelectTrigger className="w-48"><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="PRESENT">Присутствовал</SelectItem>
                              <SelectItem value="ABSENT">Отсутствовал</SelectItem>
                              <SelectItem value="EXCUSED">Уважительная причина</SelectItem>
                            </SelectContent>
                          </Select>
                          <div className="mt-1 text-xs text-muted-foreground">{statusLabel(attendance?.status)}</div>
                        </TableCell>
                        <TableCell>
                          <Textarea
                            className="min-h-10"
                            rows={1}
                            value={comments[participant.userId] ?? attendance?.comment ?? ""}
                            onChange={(event) =>
                              setComments((prev) => ({ ...prev, [participant.userId]: event.target.value }))
                            }
                          />
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={markMutation.isPending}
                            onClick={() =>
                              markMutation.mutate({
                                participant,
                                status: attendance?.status ?? "PRESENT",
                              })
                            }
                          >
                            Сохранить
                          </Button>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            size="sm"
                            disabled={
                              completionMutation.isPending ||
                              participant.assignmentStatus === "COMPLETED"
                            }
                            onClick={() => handleConfirmCompletion(participant)}
                          >
                            {participant.assignmentStatus === "COMPLETED" ? "Завершен" : "Подтвердить завершение"}
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </div>
    </Layout>
  );
}

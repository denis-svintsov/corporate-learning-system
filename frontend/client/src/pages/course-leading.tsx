import { Layout } from "@/components/layout/Layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
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
import { formatFullName } from "@/lib/userName";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Award, CalendarCheck, MessageSquare, Save, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
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
  if (status === "FAILED") return "Не пройден";
  if (status === "IN_PROGRESS") return "В процессе";
  if (status === "OVERDUE") return "Просрочен";
  return "Назначен";
}

function difficultyLabel(difficulty?: string | null) {
  if (difficulty === "INTERMEDIATE") return "Средний";
  if (difficulty === "ADVANCED") return "Продвинутый";
  return "Начальный";
}

const EMPTY_PARTICIPANTS: CourseParticipantDto[] = [];

export default function CourseLeadingPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [, navigate] = useLocation();
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [attendanceDate, setAttendanceDate] = useState(todayIso());
  const [comments, setComments] = useState<Record<string, string>>({});
  const [attendanceDrafts, setAttendanceDrafts] = useState<Record<string, AttendanceStatus>>({});
  const [selectedParticipantIds, setSelectedParticipantIds] = useState<string[]>([]);
  const [bulkStatus, setBulkStatus] = useState<AttendanceStatus>("PRESENT");
  const [resultDrafts, setResultDrafts] = useState<Record<string, "PASSED" | "FAILED">>({});

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
  const isFinalCourseDay = !!selectedCourse?.endDate && attendanceDate === selectedCourse.endDate;

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

  const participants = participantsQuery.data ?? EMPTY_PARTICIPANTS;

  useEffect(() => {
    setAttendanceDrafts({});
    setSelectedParticipantIds([]);
  }, [selectedCourse?.id, attendanceDate]);

  useEffect(() => {
    setResultDrafts((prev) => {
      let changed = false;
      const next = { ...prev };
      participants.forEach((participant) => {
        if (!next[participant.userId]) {
          next[participant.userId] = (participant.attendancePercentage ?? 0) >= 50 ? "PASSED" : "FAILED";
          changed = true;
        }
      });
      return changed ? next : prev;
    });
  }, [participants]);

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
          return [id, formatFullName(profile)] as const;
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

  const currentAttendanceStatus = (participant: CourseParticipantDto, attendance?: AttendanceDto) =>
    attendanceDrafts[participant.userId] ?? attendance?.status ?? "PRESENT";

  const selectedParticipantSet = useMemo(() => new Set(selectedParticipantIds), [selectedParticipantIds]);
  const allParticipantIds = useMemo(() => participants.map((participant) => participant.userId), [participants]);
  const allParticipantsSelected = allParticipantIds.length > 0 && selectedParticipantIds.length === allParticipantIds.length;

  const toggleParticipant = (userId: string, checked: boolean) => {
    setSelectedParticipantIds((prev) => {
      if (checked) return Array.from(new Set([...prev, userId]));
      return prev.filter((id) => id !== userId);
    });
  };

  const toggleAllParticipants = (checked: boolean) => {
    setSelectedParticipantIds(checked ? allParticipantIds : []);
  };

  const applyStatusToSelected = (status: AttendanceStatus) => {
    const targetIds = selectedParticipantIds.length > 0 ? selectedParticipantIds : allParticipantIds;
    setBulkStatus(status);
    setSelectedParticipantIds(targetIds);
    setAttendanceDrafts((prev) => {
      const next = { ...prev };
      targetIds.forEach((id) => {
        next[id] = status;
      });
      return next;
    });
  };

  const saveAttendanceMutation = useMutation({
    mutationFn: async () => {
      const selected = participants.filter((participant) => selectedParticipantSet.has(participant.userId));
      if (selected.length === 0) {
        throw new Error("Выберите участников для отметки.");
      }
      await Promise.all(
        selected.map((participant) => {
          const attendance = attendanceByUser.get(participant.userId);
          return markCourseAttendance(selectedCourse!.id, {
            userId: participant.userId,
            attendanceDate,
            status: currentAttendanceStatus(participant, attendance),
            comment: comments[participant.userId]?.trim() || undefined,
          });
        }),
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["course-attendance", selectedCourse?.id, attendanceDate] });
      toast({ title: "Посещаемость сохранена", description: `Отмечено участников: ${selectedParticipantIds.length}` });
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
    mutationFn: async (targetParticipants: CourseParticipantDto[]) => {
      if (targetParticipants.length === 0) {
        throw new Error("Выберите участников, которым нужно завершить курс.");
      }
      await Promise.all(
        targetParticipants.map((participant) =>
          confirmCourseCompletion(
            selectedCourse!.id,
            participant.userId,
            resultDrafts[participant.userId] !== "FAILED",
          ),
        ),
      );
      return targetParticipants.length;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["course-participants", selectedCourse?.id] });
      queryClient.invalidateQueries({ queryKey: ["assigned-courses"] });
      queryClient.invalidateQueries({ queryKey: ["user-cabinet-progress"] });
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

  const handleConfirmCompletion = () => {
    const targetParticipants = participants.filter(
      (participant) =>
        selectedParticipantSet.has(participant.userId) &&
        participant.assignmentStatus !== "COMPLETED" &&
        participant.assignmentStatus !== "FAILED",
    );
    if (targetParticipants.length === 0) {
      toast({
        title: "Некого завершать",
        description: "Выберите участников, у которых курс еще не завершен.",
        variant: "destructive",
      });
      return;
    }
    const courseTitle = selectedCourse?.title ?? "курс";
    const passedCount = targetParticipants.filter((participant) => resultDrafts[participant.userId] !== "FAILED").length;
    const failedCount = targetParticipants.length - passedCount;
    const confirmed = window.confirm(
      `Сохранить итог курса "${courseTitle}" для выбранных участников (${targetParticipants.length})?\n\nПройдут курс: ${passedCount}\nНе пройдут курс: ${failedCount}\n\nСертификаты будут созданы только тем, кто прошел курс.`,
    );
    if (confirmed) {
      completionMutation.mutate(targetParticipants);
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

              <div className="flex flex-col gap-3 rounded-md border bg-muted/20 p-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
                  <div className="space-y-2">
                    <Label>Быстрая отметка</Label>
                    <Select value={bulkStatus} onValueChange={(value) => applyStatusToSelected(value as AttendanceStatus)}>
                      <SelectTrigger className="w-full sm:w-64"><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="PRESENT">Присутствовали</SelectItem>
                        <SelectItem value="ABSENT">Отсутствовали</SelectItem>
                        <SelectItem value="EXCUSED">Уважительная причина</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                    <Button
                      variant="outline"
                      disabled={participants.length === 0}
                      onClick={() => applyStatusToSelected("PRESENT")}
                    >
                      Все присутствовали
                    </Button>
                    <Button
                      disabled={saveAttendanceMutation.isPending || selectedParticipantIds.length === 0}
                      onClick={() => saveAttendanceMutation.mutate()}
                    >
                      <Save className="h-4 w-4" />
                      Сохранить посещаемость
                    </Button>
                  </div>
                </div>
                <div className="text-sm text-muted-foreground">
                  Выбрано для сохранения: {selectedParticipantIds.length} из {participants.length}. В строках можно изменить статус и комментарий перед сохранением.
                </div>
              </div>

              <div className="flex flex-col gap-3 rounded-md border p-4 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <div className="flex items-center gap-2 font-medium">
                    <Award className="h-4 w-4" />
                    Итог курса
                  </div>
                  <div className="mt-1 text-sm text-muted-foreground">
                    {isFinalCourseDay
                      ? "Последний день курса: проверьте посещаемость и выберите, кто прошел курс."
                      : "Итог доступен в последний день курса. До этого ведется только посещаемость."}
                  </div>
                </div>
                <Button
                  variant="secondary"
                  disabled={!isFinalCourseDay || completionMutation.isPending || selectedParticipantIds.length === 0}
                  onClick={handleConfirmCompletion}
                >
                  Сохранить итог выбранных
                </Button>
              </div>

              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">
                      <Checkbox
                        checked={allParticipantsSelected}
                        onCheckedChange={(checked) => toggleAllParticipants(checked === true)}
                        aria-label="Выбрать всех участников"
                      />
                    </TableHead>
                    <TableHead>Участник</TableHead>
                    <TableHead>Назначение</TableHead>
                    <TableHead>Посещаемость</TableHead>
                    <TableHead>Статус на дату</TableHead>
                    {isFinalCourseDay && <TableHead>Итог</TableHead>}
                    <TableHead>Комментарий</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {participants.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={isFinalCourseDay ? 7 : 6} className="text-center text-muted-foreground">
                        У курса пока нет назначенных участников.
                      </TableCell>
                    </TableRow>
                  )}
                  {participants.map((participant) => {
                    const attendance = attendanceByUser.get(participant.userId);
                    const attendancePercentage = participant.attendancePercentage ?? 0;
                    const isLowAttendance = isFinalCourseDay && attendancePercentage < 50;
                    const result = resultDrafts[participant.userId] ?? (attendancePercentage >= 50 ? "PASSED" : "FAILED");
                    return (
                      <TableRow key={participant.userId} className={isLowAttendance ? "bg-destructive/5" : undefined}>
                        <TableCell>
                          <Checkbox
                            checked={selectedParticipantSet.has(participant.userId)}
                            onCheckedChange={(checked) => toggleParticipant(participant.userId, checked === true)}
                            aria-label={`Выбрать ${participantNames.get(participant.userId) ?? participant.userId}`}
                          />
                        </TableCell>
                        <TableCell className="font-medium">
                          {participantNames.get(participant.userId) ?? participant.userId}
                        </TableCell>
                        <TableCell>
                          <Badge variant={participant.assignmentStatus === "FAILED" ? "destructive" : "outline"}>
                            {assignmentStatusLabel(participant.assignmentStatus)}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className={isLowAttendance ? "font-medium text-destructive" : "font-medium"}>
                            {attendancePercentage}%
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {participant.presentDays ?? 0}/{participant.totalCourseDays ?? 0} дней
                          </div>
                          {isLowAttendance && (
                            <div className="mt-1 flex items-center gap-1 text-xs text-destructive">
                              <AlertTriangle className="h-3 w-3" />
                              Ниже 50%
                            </div>
                          )}
                        </TableCell>
                        <TableCell>
                          <Select
                            value={currentAttendanceStatus(participant, attendance)}
                            onValueChange={(value) =>
                              setAttendanceDrafts((prev) => ({
                                ...prev,
                                [participant.userId]: value as AttendanceStatus,
                              }))
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
                        {isFinalCourseDay && (
                          <TableCell>
                            <Select
                              value={result}
                              disabled={participant.assignmentStatus === "COMPLETED" || participant.assignmentStatus === "FAILED"}
                              onValueChange={(value) =>
                                setResultDrafts((prev) => ({
                                  ...prev,
                                  [participant.userId]: value as "PASSED" | "FAILED",
                                }))
                              }
                            >
                              <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="PASSED">Прошел</SelectItem>
                                <SelectItem value="FAILED">Не прошел</SelectItem>
                              </SelectContent>
                            </Select>
                          </TableCell>
                        )}
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

import { Link, useParams } from "wouter";
import { Layout } from "@/components/layout/Layout";
import { useQuery } from "@tanstack/react-query";
import { fetchAssignedCourses, fetchCourse, fetchMyProgress } from "@/lib/coursesApi";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/contexts/AuthContext";
import {
  Building2,
  ExternalLink,
  MapPin,
  MessageSquare,
  CalendarPlus,
  CircleCheck,
  CircleDashed,
  CircleDot,
} from "lucide-react";

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleDateString("ru-RU");
}

function formatCalendarDate(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function assignmentLabel(status?: string | null) {
  if (!status) return "Не назначен";
  if (status === "ASSIGNED") return "Назначен";
  if (status === "IN_PROGRESS") return "В процессе";
  if (status === "COMPLETED") return "Завершен";
  if (status === "OVERDUE") return "Просрочен";
  return status;
}

function assignmentProgress(status?: string | null) {
  if (status === "COMPLETED") return 100;
  if (status === "IN_PROGRESS") return 50;
  if (status === "ASSIGNED") return 10;
  if (status === "OVERDUE") return 0;
  return 0;
}

export default function CourseDetail() {
  const params = useParams();
  const courseId = params?.id as string | undefined;
  const { user } = useAuth();

  const { data: course, isLoading, isError, error } = useQuery({
    queryKey: ["course", courseId],
    queryFn: () => {
      if (!courseId) return Promise.reject(new Error("Course id missing"));
      return fetchCourse(courseId);
    },
    enabled: !!courseId,
  });

  const { data: assignedCourses = [] } = useQuery({
    queryKey: ["assigned-courses", user?.id],
    queryFn: () => fetchAssignedCourses(),
    enabled: !!user?.id,
  });

  const { data: progressData } = useQuery({
    queryKey: ["progress", user?.id],
    queryFn: () => fetchMyProgress(user!.id),
    enabled: !!user?.id,
  });

  const assignment = assignedCourses.find((a) => a.courseId === courseId);
  const isAdmin = (user?.roles ?? []).includes("ADMIN");

  if (isLoading) {
    return (
      <Layout>
        <div className="rounded-lg border p-8 text-center text-sm text-muted-foreground">
          Загрузка курса...
        </div>
      </Layout>
    );
  }

  if (isError || !course) {
    return (
      <Layout>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
          Не удалось загрузить курс. {(error as Error)?.message}
        </div>
      </Layout>
    );
  }

  const durationLabel = course.durationMinutes
    ? `${Math.round((course.durationMinutes / 60) * 10) / 10} ч`
    : "-";

  const progress = progressData?.courses.find((c) => c.courseId === courseId)?.progressPercentage
    ?? assignmentProgress(assignment?.status);
  const isStarted = progress > 0 || assignment?.status === "IN_PROGRESS" || assignment?.status === "COMPLETED";
  const isCompleted = progress >= 100 || assignment?.status === "COMPLETED";
  const calendarDate = formatCalendarDate(assignment?.dueDate ?? course.startDate ?? course.endDate);
  const calendarHref = calendarDate
    ? `/calendar?date=${calendarDate}&courseId=${course.id}`
    : `/calendar?courseId=${course.id}`;

  return (
    <Layout>
      <div className="space-y-6">
        <section className="overflow-hidden rounded-2xl border bg-card">
          <div
            className="h-64 w-full bg-gradient-to-br from-sky-100 via-slate-50 to-indigo-100"
            style={{
              backgroundImage: course.coverUrl ? `url(${course.coverUrl})` : undefined,
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          />
          <div className="p-6 space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary">{course.status ?? "ACTIVE"}</Badge>
              <Badge variant="outline">{course.difficulty ?? "-"}</Badge>
              <Badge variant="outline">{durationLabel}</Badge>
              <Badge variant="outline">{assignmentLabel(assignment?.status)}</Badge>
            </div>
            <div className="space-y-2">
              <h1 className="text-3xl font-bold tracking-tight">{course.title}</h1>
              <p className="text-muted-foreground">{course.description || "Описание пока не заполнено."}</p>
            </div>
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle>Даты прохождения</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="rounded-lg border p-4">
                    <div className="text-xs text-muted-foreground">Фиксированный старт потока</div>
                    <div className="mt-1 font-semibold">{formatDate(course.startDate)}</div>
                  </div>
                  <div className="rounded-lg border p-4">
                    <div className="text-xs text-muted-foreground">Фиксированное завершение потока</div>
                    <div className="mt-1 font-semibold">{formatDate(course.endDate)}</div>
                  </div>
                  <div className="rounded-lg border p-4">
                    <div className="text-xs text-muted-foreground">Дата назначения вам</div>
                    <div className="mt-1 font-semibold">{formatDate(assignment?.createdAt)}</div>
                  </div>
                  <div className="rounded-lg border p-4">
                    <div className="text-xs text-muted-foreground">Ваш дедлайн</div>
                    <div className="mt-1 font-semibold">{formatDate(assignment?.dueDate)}</div>
                  </div>
                </div>
                {(!course.startDate || !course.endDate) && (
                  <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
                    Для курса еще не выставлены фиксированные даты потока.
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Как проходит курс</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-sm text-muted-foreground whitespace-pre-line">
                  {course.instructions || "Инструкции пока не заполнены."}
                </p>
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="rounded-lg border p-4">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <Building2 className="h-4 w-4" />
                      Партнер
                    </div>
                    <div className="mt-2 text-sm text-muted-foreground">{course.partnerName || "Не указан"}</div>
                  </div>
                  <div className="rounded-lg border p-4">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <MapPin className="h-4 w-4" />
                      Площадка
                    </div>
                    <div className="mt-2 text-sm text-muted-foreground">{course.partnerLocation || "Не указана"}</div>
                  </div>
                </div>
                <div className="flex flex-wrap gap-3 pt-2">
                  {course.aggregatorUrl ? (
                    <Button asChild>
                      <a href={course.aggregatorUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2">
                        <ExternalLink className="h-4 w-4" />
                        Перейти к курсу партнера
                      </a>
                    </Button>
                  ) : (
                    <Button disabled>Ссылка партнера не задана</Button>
                  )}
                  {assignment ? (
                    <Button asChild variant="outline" className="inline-flex items-center gap-2">
                      <Link href={`/chat?courseId=${course.id}`}>
                        <MessageSquare className="h-4 w-4" />
                        Перейти в чат курса
                      </Link>
                    </Button>
                  ) : (
                    <Button variant="outline" disabled className="inline-flex items-center gap-2">
                      <MessageSquare className="h-4 w-4" />
                      Чат доступен после назначения
                    </Button>
                  )}
                  <Button variant="outline" asChild className="inline-flex items-center gap-2">
                    <Link href={calendarHref}>
                      <CalendarPlus className="h-4 w-4" />
                      Перейти в календарь
                    </Link>
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Статус прохождения</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Прогресс</span>
                    <span className="font-semibold">{progress}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-muted">
                    <div className="h-2 rounded-full bg-primary" style={{ width: `${progress}%` }} />
                  </div>
                </div>
                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2">
                    {assignment ? <CircleCheck className="h-4 w-4 text-emerald-600" /> : <CircleDashed className="h-4 w-4 text-muted-foreground" />}
                    <span>Курс назначен</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {isStarted
                      ? <CircleCheck className="h-4 w-4 text-emerald-600" />
                      : <CircleDot className="h-4 w-4 text-muted-foreground" />}
                    <span>Прохождение начато</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {isCompleted
                      ? <CircleCheck className="h-4 w-4 text-emerald-600" />
                      : <CircleDot className="h-4 w-4 text-muted-foreground" />}
                    <span>Курс завершен</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Ключевые параметры</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">Категория</span>
                  <span>{course.categoryId ?? "-"}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">Сложность</span>
                  <span>{course.difficulty ?? "-"}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">Длительность</span>
                  <span>{durationLabel}</span>
                </div>
              </CardContent>
            </Card>

            {isAdmin && course.companyCost != null && (
              <Card>
                <CardHeader>
                  <CardTitle>Для администратора</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-xs text-muted-foreground">Стоимость для компании</div>
                  <div className="mt-1 text-2xl font-bold">{course.companyCost.toLocaleString("ru-RU")} ₽</div>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}

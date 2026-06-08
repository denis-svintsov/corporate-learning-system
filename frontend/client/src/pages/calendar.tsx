import { Layout } from "@/components/layout/Layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/contexts/AuthContext";
import { AssignedCourseDto, fetchAssignedCourses } from "@/lib/coursesApi";
import { useQuery } from "@tanstack/react-query";
import { format, isSameDay, startOfDay } from "date-fns";
import { ru } from "date-fns/locale";
import { AlertTriangle, CalendarClock, CheckCircle2, Clock, GraduationCap } from "lucide-react";
import { useMemo, useState } from "react";

type ScheduleEventType = "start" | "deadline" | "end";

type ScheduleEvent = {
  id: string;
  courseId: string;
  courseTitle: string;
  date: Date;
  time: string;
  type: ScheduleEventType;
  status?: string | null;
  description: string;
};

const eventMeta: Record<ScheduleEventType, { label: string; className: string; icon: typeof GraduationCap }> = {
  start: {
    label: "Старт",
    className: "border-primary",
    icon: GraduationCap,
  },
  deadline: {
    label: "Дедлайн",
    className: "border-orange-500",
    icon: AlertTriangle,
  },
  end: {
    label: "Завершение",
    className: "border-emerald-500",
    icon: CheckCircle2,
  },
};

function parseDate(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return startOfDay(date);
}

function parseCalendarQueryDate() {
  if (typeof window === "undefined") return new Date();
  const raw = new URLSearchParams(window.location.search).get("date");
  if (!raw) return new Date();
  const [year, month, day] = raw.split("-").map(Number);
  if (!year || !month || !day) return new Date();
  return new Date(year, month - 1, day);
}

function buildScheduleEvents(courses: AssignedCourseDto[]): ScheduleEvent[] {
  return courses.flatMap((course) => {
    if (!course.courseId) return [];

    const title = course.courseTitle ?? "Курс без названия";
    const events: ScheduleEvent[] = [];
    const startDate = parseDate(course.courseStartDate);
    const dueDate = parseDate(course.dueDate);
    const endDate = parseDate(course.courseEndDate);

    if (startDate) {
      events.push({
        id: `${course.id}-start`,
        courseId: course.courseId,
        courseTitle: title,
        date: startDate,
        time: "09:00",
        type: "start",
        status: course.status,
        description: "Начало обучения по назначенному курсу",
      });
    }

    if (dueDate) {
      events.push({
        id: `${course.id}-deadline`,
        courseId: course.courseId,
        courseTitle: title,
        date: dueDate,
        time: "18:00",
        type: "deadline",
        status: course.status,
        description: "Последний день выполнения назначенного курса",
      });
    }

    if (endDate && (!dueDate || !isSameDay(endDate, dueDate))) {
      events.push({
        id: `${course.id}-end`,
        courseId: course.courseId,
        courseTitle: title,
        date: endDate,
        time: "18:00",
        type: "end",
        status: course.status,
        description: "Плановое завершение курса",
      });
    }

    return events;
  }).sort((a, b) => a.date.getTime() - b.date.getTime() || a.time.localeCompare(b.time));
}

export default function CalendarPage() {
  const { user } = useAuth();
  const [date, setDate] = useState<Date | undefined>(() => parseCalendarQueryDate());
  const highlightedCourseId = typeof window === "undefined"
    ? null
    : new URLSearchParams(window.location.search).get("courseId");

  const { data: assignedCourses = [], isLoading, isError, error } = useQuery({
    queryKey: ["calendar-assigned-courses", user?.id],
    queryFn: () => fetchAssignedCourses(),
    enabled: !!user?.id,
  });

  const events = useMemo(() => buildScheduleEvents(assignedCourses), [assignedCourses]);
  const selectedDayEvents = useMemo(
    () => events.filter((event) => date && isSameDay(event.date, date)),
    [date, events],
  );
  const upcomingEvents = useMemo(() => {
    const today = startOfDay(new Date());
    return events.filter((event) => event.date >= today).slice(0, 5);
  }, [events]);
  const eventDates = useMemo(() => events.map((event) => event.date), [events]);
  const selectedDateLabel = date ? format(date, "d MMMM yyyy", { locale: ru }) : "выбранную дату";

  return (
    <Layout>
      <div className="flex h-full flex-col space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-secondary">Расписание</h1>
          <p className="text-muted-foreground">Календарь стартов, дедлайнов и завершения назначенных курсов</p>
        </div>

        <div className="grid gap-6 xl:grid-cols-[340px_1fr]">
          <div className="space-y-6">
            <Card>
              <CardContent className="p-4">
                <Calendar
                  mode="single"
                  selected={date}
                  onSelect={setDate}
                  locale={ru}
                  modifiers={{ hasEvent: eventDates }}
                  modifiersClassNames={{
                    hasEvent: "after:absolute after:bottom-1 after:h-1 after:w-1 after:rounded-full after:bg-primary",
                  }}
                  className="w-full rounded-md border shadow-sm"
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <CalendarClock className="h-4 w-4" />
                  Ближайшие события
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {isLoading && <div className="text-sm text-muted-foreground">Загрузка расписания...</div>}
                {!isLoading && upcomingEvents.length === 0 && (
                  <div className="text-sm text-muted-foreground">Ближайших событий пока нет.</div>
                )}
                {upcomingEvents.map((event) => {
                  const meta = eventMeta[event.type];
                  const Icon = meta.icon;
                  return (
                    <button
                      key={event.id}
                      type="button"
                      onClick={() => setDate(event.date)}
                      className="flex w-full items-start gap-3 rounded-md border p-3 text-left transition-colors hover:bg-muted/50"
                    >
                      <Icon className="mt-0.5 h-4 w-4 text-muted-foreground" />
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-sm font-medium">{event.courseTitle}</div>
                        <div className="text-xs text-muted-foreground">
                          {format(event.date, "d MMMM", { locale: ru })} • {event.time}
                        </div>
                      </div>
                      <Badge variant="outline" className="shrink-0 text-xs">{meta.label}</Badge>
                    </button>
                  );
                })}
              </CardContent>
            </Card>
          </div>

          <Card className="min-h-[520px]">
            <CardHeader>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <CardTitle>События на {selectedDateLabel}</CardTitle>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {selectedDayEvents.length > 0
                      ? `Найдено событий: ${selectedDayEvents.length}`
                      : "На выбранный день событий нет"}
                  </p>
                </div>
                <Button variant="outline" size="sm" onClick={() => setDate(new Date())}>
                  Сегодня
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {isError && (
                <div className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                  Не удалось загрузить расписание. {(error as Error).message}
                </div>
              )}

              {!isError && selectedDayEvents.length === 0 && (
                <div className="flex min-h-[320px] flex-col items-center justify-center rounded-md border border-dashed p-8 text-center">
                  <CalendarClock className="mb-3 h-10 w-10 text-muted-foreground" />
                  <h3 className="font-semibold">Свободный день</h3>
                  <p className="mt-1 max-w-md text-sm text-muted-foreground">
                    Выберите дату с отметкой в календаре или посмотрите ближайшие события слева.
                  </p>
                </div>
              )}

              <div className="space-y-4">
                {selectedDayEvents.map((event) => {
                  const meta = eventMeta[event.type];
                  const Icon = meta.icon;
                  return (
                    <div
                      key={event.id}
                      className={`flex gap-4 border-l-4 ${meta.className} py-2 pl-4 ${
                        highlightedCourseId === event.courseId ? "rounded-md bg-primary/5 pr-3" : ""
                      }`}
                    >
                      <div className="flex w-16 shrink-0 items-center gap-1 pt-1 text-sm font-bold">
                        <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                        {event.time}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="mb-2 flex flex-wrap items-center gap-2">
                          <Badge variant="secondary" className="gap-1">
                            <Icon className="h-3 w-3" />
                            {meta.label}
                          </Badge>
                          {event.status && <Badge variant="outline">{event.status}</Badge>}
                        </div>
                        <h4 className="font-semibold">{event.courseTitle}</h4>
                        <p className="mt-1 text-sm text-muted-foreground">{event.description}</p>
                        <Button variant="outline" size="sm" className="mt-3" asChild>
                          <a href={`/course/${event.courseId}`}>Открыть курс</a>
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {!isLoading && !isError && events.length === 0 && (
                <div className="mt-6 rounded-md bg-muted/50 p-4 text-sm text-muted-foreground">
                  Расписание появится после назначения курсов с датами старта, окончания или дедлайна.
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </Layout>
  );
}

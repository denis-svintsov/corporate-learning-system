import { Layout } from "@/components/layout/Layout";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { CalendarDays, Clock, Award, ArrowRight, AlertTriangle, BookOpen } from "lucide-react";
import { Link } from "wouter";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery } from "@tanstack/react-query";
import { fetchAssignedCourses, fetchAssignmentPolicy, fetchMyCertificates, fetchMyProgress } from "@/lib/coursesApi";
import { useMemo } from "react";

export default function Dashboard() {
  const { user } = useAuth();
  const { data: progressData } = useQuery({
    queryKey: ["progress", user?.id],
    queryFn: () => fetchMyProgress(user!.id),
    enabled: !!user?.id,
  });
  const { data: certificates } = useQuery({
    queryKey: ["certificates", user?.id],
    queryFn: () => fetchMyCertificates(user!.id),
    enabled: !!user?.id,
  });
  const { data: assignedCourses = [] } = useQuery({
    queryKey: ["assigned-courses", user?.id],
    queryFn: () => fetchAssignedCourses(),
    enabled: !!user?.id,
  });
  const { data: assignmentPolicy } = useQuery({
    queryKey: ["assignment-policy"],
    queryFn: () => fetchAssignmentPolicy(),
  });

  const activeCourse = useMemo(() => {
    const courses = progressData?.courses ?? [];
    if (courses.length === 0) return null;
    return courses.reduce((best, current) =>
      current.progressPercentage > best.progressPercentage ? current : best,
    );
  }, [progressData]);

  const completedLessons = progressData?.courses?.reduce(
    (sum, course) => sum + course.completedLessons,
    0,
  ) ?? 0;

  const quarterLimit = assignmentPolicy?.maxCoursesPerQuarter ?? 3;

  const isInCurrentQuarter = (iso?: string | null) => {
    if (!iso) return false;
    const d = new Date(iso);
    const now = new Date();
    if (Number.isNaN(d.getTime())) return false;
    const q = Math.floor(d.getMonth() / 3);
    const qNow = Math.floor(now.getMonth() / 3);
    return d.getFullYear() === now.getFullYear() && q === qNow;
  };

  const currentQuarterStart = useMemo(() => {
    const now = new Date();
    const startMonth = Math.floor(now.getMonth() / 3) * 3;
    return new Date(now.getFullYear(), startMonth, 1, 0, 0, 0, 0);
  }, []);

  const isActiveAssignmentStatus = (status?: string | null) =>
    status === "ASSIGNED" || status === "IN_PROGRESS";

  const selectedCountByAssignments = assignedCourses.filter((c) => isActiveAssignmentStatus(c.status) && isInCurrentQuarter(c.createdAt)).length
    + assignedCourses.filter((c) => {
      if (!isActiveAssignmentStatus(c.status)) return false;
      if (isInCurrentQuarter(c.createdAt)) return false;
      if (c.dueDate || c.courseEndDate) return false;
      if (!c.createdAt) return true;
      const created = new Date(c.createdAt);
      if (Number.isNaN(created.getTime())) return true;
      return created < currentQuarterStart;
    }).length;
  const certificatesCount = certificates?.length ?? 0;

  const nearestCourse = useMemo(() => {
    const now = new Date();
    const withStartDate = assignedCourses
      .filter((c) => !!c.courseStartDate && !!c.courseId)
      .map((c) => ({ ...c, start: new Date(c.courseStartDate as string) }))
      .filter((c) => !Number.isNaN(c.start.getTime()) && c.start >= now)
      .sort((a, b) => a.start.getTime() - b.start.getTime());
    if (withStartDate.length > 0) {
      return withStartDate[0];
    }

    const withDueDate = assignedCourses
      .filter((c) => !!c.dueDate && !!c.courseId)
      .map((c) => ({ ...c, due: new Date(c.dueDate as string) }))
      .filter((c) => !Number.isNaN(c.due.getTime()) && c.due >= now)
      .sort((a, b) => a.due.getTime() - b.due.getTime());
    return withDueDate[0] ?? null;
  }, [assignedCourses]);

  const daysToNearest = useMemo(() => {
    const nearestDateRaw = nearestCourse?.courseStartDate ?? nearestCourse?.dueDate;
    if (!nearestDateRaw) return null;
    const due = new Date(nearestDateRaw);
    const now = new Date();
    const msPerDay = 1000 * 60 * 60 * 24;
    return Math.max(0, Math.ceil((due.getTime() - now.getTime()) / msPerDay));
  }, [nearestCourse]);
  
  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold tracking-tight text-secondary">Дашборд</h1>
          <Button asChild>
            <Link href="/catalog">Подобрать курсы</Link>
          </Button>
        </div>

        {/* Stats Grid */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Выбрано курсов</CardTitle>
              <CalendarDays className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{selectedCountByAssignments}/{quarterLimit}</div>
              <p className="text-xs text-muted-foreground">
                {selectedCountByAssignments >= quarterLimit ? "Лимит достигнут" : `Необходимо выбрать еще ${Math.max(0, quarterLimit - selectedCountByAssignments)}`}
              </p>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Пройдено уроков</CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{completedLessons}</div>
              <p className="text-xs text-muted-foreground">Суммарно по активным курсам</p>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Сертификаты</CardTitle>
              <Award className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{certificatesCount}</div>
              <p className="text-xs text-muted-foreground">Всего получено</p>
            </CardContent>
          </Card>
        </div>

        {/* Attention Required */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
          <Card className="col-span-4">
            <CardHeader>
              <CardTitle>Требует внимания</CardTitle>
              <CardDescription>Задачи, сформированные по вашим данным</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {selectedCountByAssignments < quarterLimit && (
                <div className="flex items-center justify-between rounded-lg border p-4 shadow-sm bg-accent/20">
                  <div className="flex items-center gap-4">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
                      <AlertTriangle className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold">Завершите выбор курсов</h4>
                      <p className="text-sm text-muted-foreground">
                        Выбрано {selectedCountByAssignments}/{quarterLimit} курсов
                      </p>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" asChild>
                    <Link href="/selection">Перейти</Link>
                  </Button>
                </div>
              )}

              {activeCourse && activeCourse.progressPercentage < 100 && (
                <div className="flex items-center justify-between rounded-lg border p-4 shadow-sm">
                  <div className="flex items-center gap-4">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 text-blue-600">
                      <BookOpen className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold">Продолжите обучение</h4>
                      <p className="text-sm text-muted-foreground">
                        {activeCourse.courseTitle}: {activeCourse.progressPercentage}%
                      </p>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" asChild>
                    <Link href={`/course/${activeCourse.courseId}`}>Открыть</Link>
                  </Button>
                </div>
              )}

              {!activeCourse && selectedCountByAssignments >= quarterLimit && (
                <div className="rounded-lg border p-4 text-sm text-muted-foreground">
                  Сейчас нет активных задач. Вы в хорошем темпе.
                </div>
              )}
            </CardContent>
          </Card>

          <Card className="col-span-3">
            <CardHeader>
              <CardTitle>Ближайший курс</CardTitle>
              <CardDescription>
                {nearestCourse && daysToNearest != null ? `До старта осталось ${daysToNearest} дн.` : "Нет курса с ближайшей датой старта"}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {!nearestCourse && !activeCourse && (
                <div className="rounded-lg border p-6 text-sm text-muted-foreground">
                  Нет активных курсов в прогрессе.
                </div>
              )}
              {nearestCourse && (
                <>
                  <div className="aspect-video w-full overflow-hidden rounded-md bg-gradient-to-br from-slate-100 via-slate-50 to-slate-200" />
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <Badge variant="secondary">Ближайший старт</Badge>
                      <span className="text-sm text-muted-foreground">
                        {nearestCourse.courseStartDate
                          ? new Date(nearestCourse.courseStartDate).toLocaleDateString("ru-RU")
                          : nearestCourse.dueDate
                            ? new Date(nearestCourse.dueDate).toLocaleDateString("ru-RU")
                            : "-"}
                      </span>
                    </div>
                    <h3 className="font-bold text-lg leading-tight mb-2">{nearestCourse.courseTitle ?? "Без названия"}</h3>
                    <p className="text-sm text-muted-foreground line-clamp-2">
                      Статус назначения: {nearestCourse.status ?? "ASSIGNED"}
                    </p>
                  </div>
                  <div className="pt-2">
                    <div className="flex items-center justify-between text-sm mb-1">
                      <span>Прогресс подготовки</span>
                      <span className="font-medium">{nearestCourse.status === "COMPLETED" ? 100 : nearestCourse.status === "IN_PROGRESS" ? 50 : 10}%</span>
                    </div>
                    <Progress value={nearestCourse.status === "COMPLETED" ? 100 : nearestCourse.status === "IN_PROGRESS" ? 50 : 10} className="h-2" />
                  </div>
                  <Button className="w-full mt-4" asChild>
                    <Link href={`/course/${nearestCourse.courseId}`}>
                      Перейти к материалам <ArrowRight className="ml-2 h-4 w-4" />
                    </Link>
                  </Button>
                </>
              )}
              {!nearestCourse && activeCourse && (
                <>
                  <div className="aspect-video w-full overflow-hidden rounded-md bg-gradient-to-br from-slate-100 via-slate-50 to-slate-200" />
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <Badge variant="secondary">Активный курс</Badge>
                      <span className="text-sm text-muted-foreground">В процессе</span>
                    </div>
                    <h3 className="font-bold text-lg leading-tight mb-2">{activeCourse.courseTitle}</h3>
                    <p className="text-sm text-muted-foreground line-clamp-2">
                      Прогресс по курсу: {activeCourse.progressPercentage}%
                    </p>
                  </div>
                  <div className="pt-2">
                    <div className="flex items-center justify-between text-sm mb-1">
                      <span>Прогресс подготовки</span>
                      <span className="font-medium">{activeCourse.progressPercentage}%</span>
                    </div>
                    <Progress value={activeCourse.progressPercentage} className="h-2" />
                  </div>
                  <Button className="w-full mt-4" asChild>
                    <Link href={`/course/${activeCourse.courseId}`}>
                      Перейти к материалам <ArrowRight className="ml-2 h-4 w-4" />
                    </Link>
                  </Button>
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </Layout>
  );
}

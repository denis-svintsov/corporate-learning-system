import { Layout } from "@/components/layout/Layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";
import {
  CourseStatsDto,
  DepartmentProgressDto,
  fetchAnalyticsDashboard,
  generateAnalyticsReport,
  UserEngagementDto,
} from "@/lib/analyticsApi";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, BarChart3, BookOpen, Building2, Download, FileText, GraduationCap, Users } from "lucide-react";

function formatPercent(value?: number | null) {
  return `${Math.round((value ?? 0) * 10) / 10}%`;
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("ru-RU");
}

function ErrorBlock({ error }: { error: unknown }) {
  return (
    <div className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
      Не удалось загрузить аналитику. {(error as Error)?.message ?? "Неизвестная ошибка"}
    </div>
  );
}

function EmptyRow({ colSpan, text }: { colSpan: number; text: string }) {
  return (
    <TableRow>
      <TableCell colSpan={colSpan} className="text-center text-muted-foreground">
        {text}
      </TableCell>
    </TableRow>
  );
}

function DepartmentTable({ rows }: { rows: DepartmentProgressDto[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Подразделение</TableHead>
          <TableHead>Сотрудники</TableHead>
          <TableHead>Активные</TableHead>
          <TableHead>Средний прогресс</TableHead>
          <TableHead>Завершение</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.length === 0 && <EmptyRow colSpan={5} text="Нет данных по подразделениям" />}
        {rows.map((row) => (
          <TableRow key={row.departmentId}>
            <TableCell className="font-medium">{row.departmentName}</TableCell>
            <TableCell>{row.totalUsers}</TableCell>
            <TableCell>{row.activeUsers}</TableCell>
            <TableCell>
              <div className="flex min-w-44 items-center gap-3">
                <Progress value={row.averageProgress} className="h-2" />
                <span className="w-12 text-right text-sm">{formatPercent(row.averageProgress)}</span>
              </div>
            </TableCell>
            <TableCell>{formatPercent(row.completionRate)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function CourseTable({ rows }: { rows: CourseStatsDto[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Курс</TableHead>
          <TableHead>Назначения</TableHead>
          <TableHead>Завершения</TableHead>
          <TableHead>Уроки</TableHead>
          <TableHead>Средний прогресс</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.length === 0 && <EmptyRow colSpan={5} text="Нет данных по курсам" />}
        {rows.map((row) => (
          <TableRow key={row.courseId}>
            <TableCell>
              <div className="font-medium">{row.courseTitle}</div>
              <a href={`/course/${row.courseId}`} className="text-xs text-muted-foreground underline underline-offset-2">
                Открыть курс
              </a>
            </TableCell>
            <TableCell>{row.totalEnrollments}</TableCell>
            <TableCell>{row.completions}</TableCell>
            <TableCell>{row.lessonsCompleted}</TableCell>
            <TableCell>
              <div className="flex min-w-44 items-center gap-3">
                <Progress value={row.averageProgress} className="h-2" />
                <span className="w-12 text-right text-sm">{formatPercent(row.averageProgress)}</span>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function EngagementTable({ rows }: { rows: UserEngagementDto[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Сотрудник</TableHead>
          <TableHead>Подразделение</TableHead>
          <TableHead>Курсы</TableHead>
          <TableHead>Уроки</TableHead>
          <TableHead>Прогресс</TableHead>
          <TableHead>Статус</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.length === 0 && <EmptyRow colSpan={6} text="Нет данных по сотрудникам" />}
        {rows.map((row) => (
          <TableRow key={row.userId}>
            <TableCell className="font-medium">{row.userName || row.userId}</TableCell>
            <TableCell>{row.departmentName || "-"}</TableCell>
            <TableCell>{row.assignedCourses}</TableCell>
            <TableCell>{row.completedLessons}</TableCell>
            <TableCell>{formatPercent(row.averageProgress)}</TableCell>
            <TableCell>
              <Badge variant={row.active ? "default" : "secondary"}>
                {row.active ? "Активен" : "Без активности"}
              </Badge>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

export default function AnalyticsPage() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const { user } = useAuth();
  const canViewAnalytics = (user?.roles ?? []).some((role) =>
    ["ADMIN", "HR", "MANAGER", "TECHNOLOG"].includes(role),
  );

  const dashboardQuery = useQuery({
    queryKey: ["analytics-dashboard", user?.id],
    queryFn: fetchAnalyticsDashboard,
    enabled: !!user?.id && canViewAnalytics,
  });

  const reportMutation = useMutation({
    mutationFn: () => generateAnalyticsReport(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["analytics-dashboard", user?.id] });
      toast({ title: "Отчет сформирован", description: "Новый отчет добавлен в историю аналитики." });
    },
    onError: (error) => {
      toast({
        title: "Ошибка формирования отчета",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const overview = dashboardQuery.data?.overview;
  const departments = dashboardQuery.data?.departmentProgress ?? [];
  const courses = dashboardQuery.data?.courseStats ?? [];
  const engagement = dashboardQuery.data?.userEngagement ?? [];
  const notifications = dashboardQuery.data?.notificationEffectiveness ?? [];
  const reports = dashboardQuery.data?.reports ?? [];

  if (!canViewAnalytics) {
    return (
      <Layout>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
          Доступ запрещен. Страница доступна только ADMIN, HR, MANAGER или TECHNOLOG.
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-secondary">Аналитика</h1>
            <p className="text-muted-foreground">Дашборды HR и руководителей по обучению сотрудников</p>
          </div>
          <Button onClick={() => reportMutation.mutate()} disabled={reportMutation.isPending}>
            <FileText className="mr-2 h-4 w-4" />
            {reportMutation.isPending ? "Формируем..." : "Сформировать отчет"}
          </Button>
        </div>

        {dashboardQuery.isError && <ErrorBlock error={dashboardQuery.error} />}

        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Сотрудники</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{overview?.users ?? 0}</div>
              <p className="text-xs text-muted-foreground">Активных: {overview?.activeUsers ?? 0}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Подразделения</CardTitle>
              <Building2 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{overview?.departments ?? 0}</div>
              <p className="text-xs text-muted-foreground">Оргструктура обучения</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Курсы</CardTitle>
              <BookOpen className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{overview?.courses ?? 0}</div>
              <p className="text-xs text-muted-foreground">Завершено: {overview?.completedCourses ?? 0}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Средний прогресс</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{formatPercent(overview?.averageProgress)}</div>
              <p className="text-xs text-muted-foreground">Завершение: {formatPercent(overview?.completionRate)}</p>
            </CardContent>
          </Card>
        </div>

        <Tabs defaultValue="departments">
          <TabsList>
            <TabsTrigger value="departments">Подразделения</TabsTrigger>
            <TabsTrigger value="courses">Курсы</TabsTrigger>
            <TabsTrigger value="employees">Сотрудники</TabsTrigger>
            <TabsTrigger value="reports">Отчеты</TabsTrigger>
          </TabsList>

          <TabsContent value="departments" className="mt-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Building2 className="h-5 w-5" />
                  Прогресс по подразделениям
                </CardTitle>
                <CardDescription>Средний прогресс и доля завершенных курсов по каждому отделу</CardDescription>
              </CardHeader>
              <CardContent>
                {dashboardQuery.isLoading ? (
                  <div className="text-sm text-muted-foreground">Загрузка подразделений...</div>
                ) : (
                  <DepartmentTable rows={departments} />
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="courses" className="mt-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <BarChart3 className="h-5 w-5" />
                  Статистика по курсам
                </CardTitle>
                <CardDescription>Назначения, завершения, уроки и средний процент прохождения</CardDescription>
              </CardHeader>
              <CardContent>
                {dashboardQuery.isLoading ? (
                  <div className="text-sm text-muted-foreground">Загрузка курсов...</div>
                ) : (
                  <CourseTable rows={courses} />
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="employees" className="mt-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <GraduationCap className="h-5 w-5" />
                  Вовлеченность сотрудников
                </CardTitle>
                <CardDescription>Активность сотрудников по назначенным курсам и урокам</CardDescription>
              </CardHeader>
              <CardContent>
                {dashboardQuery.isLoading ? (
                  <div className="text-sm text-muted-foreground">Загрузка сотрудников...</div>
                ) : (
                  <EngagementTable rows={engagement} />
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="reports" className="mt-4">
            <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <FileText className="h-5 w-5" />
                    История отчетов
                  </CardTitle>
                  <CardDescription>Сформированные снимки аналитического дашборда</CardDescription>
                </CardHeader>
                <CardContent>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Тип</TableHead>
                        <TableHead>Формат</TableHead>
                        <TableHead>Дата</TableHead>
                        <TableHead>Автор</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {reports.length === 0 && <EmptyRow colSpan={4} text="Отчеты еще не формировались" />}
                      {reports.map((report) => (
                        <TableRow key={report.id}>
                          <TableCell className="font-medium">{report.reportType}</TableCell>
                          <TableCell>{report.format.toUpperCase()}</TableCell>
                          <TableCell>{formatDate(report.generatedAt)}</TableCell>
                          <TableCell>{report.generatedBy || "-"}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Уведомления</CardTitle>
                  <CardDescription>Эффективность каналов уведомлений</CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                  {notifications.map((row) => (
                    <div key={`${row.type}-${row.channel}`} className="rounded-md border p-3">
                      <div className="flex items-center justify-between gap-2">
                        <div className="font-medium">{row.type}</div>
                        <Badge variant="outline">{row.channel}</Badge>
                      </div>
                      <div className="mt-2 text-sm text-muted-foreground">
                        Отправлено: {row.sentCount} • Прочитано: {row.readCount}
                      </div>
                      <Progress value={row.deliveryRate} className="mt-3 h-2" />
                    </div>
                  ))}
                  {notifications.length === 0 && (
                    <div className="text-sm text-muted-foreground">Данных по уведомлениям пока нет.</div>
                  )}
                  <Button variant="outline" className="w-full" onClick={() => reportMutation.mutate()} disabled={reportMutation.isPending}>
                    <Download className="mr-2 h-4 w-4" />
                    Сохранить снимок
                  </Button>
                </CardContent>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </Layout>
  );
}

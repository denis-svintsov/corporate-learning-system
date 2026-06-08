import { Layout } from "@/components/layout/Layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Check, X } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  approveAssignmentRequest,
  AssignmentRequestDto,
  AssignmentRequestStatus,
  fetchAssignmentPolicy,
  fetchAssignmentRequests,
  rejectAssignmentRequest,
  updateAssignmentPolicy,
} from "@/lib/coursesApi";
import { useEffect, useMemo, useState } from "react";
import { fetchUserProfile } from "@/lib/usersApi";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";

function statusBadge(status: AssignmentRequestStatus) {
  if (status === "APPROVED") return <Badge className="bg-green-600">Утверждена</Badge>;
  if (status === "REJECTED") return <Badge variant="destructive">Отклонена</Badge>;
  return <Badge variant="secondary">Ожидает</Badge>;
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "-";
  return d.toLocaleDateString("ru-RU");
}

function durationLabel(minutes?: number | null) {
  if (!minutes) return null;
  const hours = Math.round((minutes / 60) * 10) / 10;
  return `${hours} ч`;
}

export default function AdminApproval() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const { user } = useAuth();
  const isAdmin = (user?.roles ?? []).includes("ADMIN") || (user?.roles ?? []).includes("HR");
  const [limitInput, setLimitInput] = useState<string>("3");

  const { data: requests = [], isLoading, isError, error } = useQuery({
    queryKey: ["assignment-requests-admin"],
    queryFn: () => fetchAssignmentRequests(),
    enabled: isAdmin,
  });

  const { data: assignmentPolicy, isLoading: isPolicyLoading } = useQuery({
    queryKey: ["assignment-policy"],
    queryFn: () => fetchAssignmentPolicy(),
    enabled: isAdmin,
  });

  useEffect(() => {
    if (assignmentPolicy?.maxCoursesPerQuarter) {
      setLimitInput(String(assignmentPolicy.maxCoursesPerQuarter));
    }
  }, [assignmentPolicy?.maxCoursesPerQuarter]);

  const userIds = useMemo(() => {
    const ids = requests.map((r) => r.userId).filter(Boolean);
    return Array.from(new Set(ids));
  }, [requests]);

  const { data: userNames = new Map<string, string>() } = useQuery({
    queryKey: ["assignment-requests-users", userIds],
    enabled: isAdmin && userIds.length > 0,
    queryFn: async () => {
      const settled = await Promise.allSettled(
        userIds.map(async (id) => {
          const profile = await fetchUserProfile(id);
          const fullName = [profile.lastName, profile.firstName].filter(Boolean).join(" ").trim();
          return [id, fullName || profile.email || id] as const;
        }),
      );
      const map = new Map<string, string>();
      settled.forEach((res) => {
        if (res.status === "fulfilled") {
          map.set(res.value[0], res.value[1]);
        }
      });
      return map;
    },
  });

  const approveMutation = useMutation({
    mutationFn: (requestId: string) => approveAssignmentRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assignment-requests-admin"] });
      queryClient.invalidateQueries({ queryKey: ["assigned-courses"] });
    },
    onError: (e) => {
      toast({
        title: "Ошибка утверждения",
        description: (e as Error).message,
        variant: "destructive",
      });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (requestId: string) => rejectAssignmentRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assignment-requests-admin"] });
    },
    onError: (e) => {
      toast({
        title: "Ошибка отклонения",
        description: (e as Error).message,
        variant: "destructive",
      });
    },
  });

  const updatePolicyMutation = useMutation({
    mutationFn: (value: number) => updateAssignmentPolicy(value),
    onSuccess: (updated) => {
      queryClient.setQueryData(["assignment-policy"], updated);
      toast({
        title: "Лимит обновлен",
        description: `Теперь на сотрудника доступно ${updated.maxCoursesPerQuarter} курсов в квартал.`,
      });
    },
    onError: (e) => {
      toast({
        title: "Ошибка обновления лимита",
        description: (e as Error).message,
        variant: "destructive",
      });
    },
  });

  const pending = requests.filter((r) => r.status === "PENDING");
  const approved = requests.filter((r) => r.status === "APPROVED");
  const rejected = requests.filter((r) => r.status === "REJECTED");

  const renderRows = (rows: AssignmentRequestDto[], actions: boolean) => (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Сотрудник</TableHead>
          <TableHead>Курс</TableHead>
          <TableHead>Комментарий</TableHead>
          <TableHead>Дата заявки</TableHead>
          <TableHead>Статус</TableHead>
          {actions && <TableHead className="text-right">Действия</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.length === 0 && (
          <TableRow>
            <TableCell colSpan={actions ? 6 : 5} className="text-center text-muted-foreground">
              Нет заявок
            </TableCell>
          </TableRow>
        )}
        {rows.map((req) => (
          <TableRow key={req.id}>
            <TableCell>
              <div className="flex items-center gap-3">
                <Avatar className="h-9 w-9">
                  <AvatarFallback>{(userNames.get(req.userId) ?? req.userId).slice(0, 1).toUpperCase()}</AvatarFallback>
                </Avatar>
                <div className="font-medium">{userNames.get(req.userId) ?? req.userId}</div>
              </div>
            </TableCell>
            <TableCell>
              <div className="space-y-2">
                <div className="font-medium">{req.courseTitle ?? req.courseId}</div>
                <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                  {req.courseDifficulty && <Badge variant="outline">{req.courseDifficulty}</Badge>}
                  {durationLabel(req.courseDurationMinutes) && <Badge variant="outline">{durationLabel(req.courseDurationMinutes)}</Badge>}
                  {req.courseId && (
                    <a href={`/course/${req.courseId}`} className="underline underline-offset-2 hover:text-foreground">
                      Открыть курс
                    </a>
                  )}
                </div>
              </div>
            </TableCell>
            <TableCell className="max-w-xs text-sm text-muted-foreground">
              {req.comment || "-"}
            </TableCell>
            <TableCell>{formatDate(req.createdAt)}</TableCell>
            <TableCell>{statusBadge(req.status)}</TableCell>
            {actions && (
              <TableCell className="text-right">
                <div className="flex justify-end gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-green-600 hover:text-green-700 hover:bg-green-50"
                    disabled={approveMutation.isPending || rejectMutation.isPending}
                    onClick={() => approveMutation.mutate(req.id)}
                  >
                    <Check className="h-4 w-4" />
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    disabled={approveMutation.isPending || rejectMutation.isPending}
                    onClick={() => rejectMutation.mutate(req.id)}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );

  if (!isAdmin) {
    return (
      <Layout>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
          Доступ запрещен. Страница доступна только HR/Admin.
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-secondary">Панель модерации назначений</h1>
          <p className="text-muted-foreground">Заявки сотрудников на назначение курсов</p>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium">Ожидают</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-orange-500">{pending.length}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium">Утверждено</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">{approved.length}</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium">Отклонено</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-red-600">{rejected.length}</div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Лимит курсов на сотрудника</CardTitle>
            <CardDescription>Ограничение количества курсов в текущем квартале</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-3 md:flex-row md:items-end">
              <div className="w-full max-w-xs space-y-2">
                <div className="text-sm font-medium">Курсов в квартал</div>
                <Input
                  type="number"
                  min={1}
                  max={100}
                  value={limitInput}
                  disabled={isPolicyLoading || updatePolicyMutation.isPending}
                  onChange={(e) => setLimitInput(e.target.value)}
                />
              </div>
              <Button
                className="md:w-auto"
                disabled={isPolicyLoading || updatePolicyMutation.isPending}
                onClick={() => {
                  const parsed = Number(limitInput);
                  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 100) {
                    toast({
                      title: "Некорректное значение",
                      description: "Укажите целое число от 1 до 100.",
                      variant: "destructive",
                    });
                    return;
                  }
                  updatePolicyMutation.mutate(parsed);
                }}
              >
                Сохранить лимит
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Заявки на назначение</CardTitle>
            <CardDescription>Утверждение перед фактическим назначением курса</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading && <div className="text-sm text-muted-foreground">Загрузка заявок...</div>}
            {isError && (
              <div className="text-sm text-destructive">
                Ошибка загрузки заявок: {(error as Error).message}
              </div>
            )}
            {!isLoading && !isError && (
              <Tabs defaultValue="pending">
                <TabsList className="mb-4">
                  <TabsTrigger value="pending">Ожидают ({pending.length})</TabsTrigger>
                  <TabsTrigger value="approved">Утверждены ({approved.length})</TabsTrigger>
                  <TabsTrigger value="rejected">Отклонены ({rejected.length})</TabsTrigger>
                  <TabsTrigger value="all">Все ({requests.length})</TabsTrigger>
                </TabsList>
                <TabsContent value="pending">{renderRows(pending, true)}</TabsContent>
                <TabsContent value="approved">{renderRows(approved, false)}</TabsContent>
                <TabsContent value="rejected">{renderRows(rejected, false)}</TabsContent>
                <TabsContent value="all">{renderRows(requests, false)}</TabsContent>
              </Tabs>
            )}
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}

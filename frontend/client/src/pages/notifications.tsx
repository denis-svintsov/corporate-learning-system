import { Layout } from "@/components/layout/Layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import {
  fetchMyNotifications,
  fetchUnreadNotificationsCount,
  markAllNotificationsRead,
  markNotificationRead,
  NotificationDto,
} from "@/lib/notificationsApi";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, Check, CheckCheck } from "lucide-react";

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("ru-RU");
}

function typeLabel(type: NotificationDto["type"]) {
  switch (type) {
    case "COURSE_ASSIGNED":
      return "Курс";
    case "LESSON_COMPLETED":
      return "Урок";
    case "COURSE_COMPLETED":
      return "Завершение";
    default:
      return "Система";
  }
}

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: fetchMyNotifications,
    refetchInterval: 10000,
    refetchOnWindowFocus: true,
  });

  const unreadQuery = useQuery({
    queryKey: ["notifications-unread-count"],
    queryFn: fetchUnreadNotificationsCount,
    refetchInterval: 10000,
    refetchOnWindowFocus: true,
  });

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
    },
    onError: (error) => {
      toast({
        title: "Не удалось обновить уведомление",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
      toast({ title: "Уведомления прочитаны" });
    },
    onError: (error) => {
      toast({
        title: "Не удалось обновить уведомления",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const notifications = notificationsQuery.data ?? [];
  const unreadCount = unreadQuery.data?.unreadCount ?? notifications.filter((item) => item.status === "UNREAD").length;

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-secondary">Уведомления</h1>
            <p className="text-muted-foreground">События по назначенным курсам, урокам и сертификатам</p>
          </div>
          <Button
            variant="outline"
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending || unreadCount === 0}
          >
            <CheckCheck className="mr-2 h-4 w-4" />
            Прочитать все
          </Button>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Всего</CardTitle>
              <Bell className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{notifications.length}</div>
              <p className="text-xs text-muted-foreground">Внутренние уведомления</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Непрочитанные</CardTitle>
              <Bell className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{unreadCount}</div>
              <p className="text-xs text-muted-foreground">Требуют внимания</p>
            </CardContent>
          </Card>
        </div>

        {notificationsQuery.isError && (
          <div className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
            Не удалось загрузить уведомления. {(notificationsQuery.error as Error).message}
          </div>
        )}

        <div className="space-y-3">
          {notificationsQuery.isLoading && (
            <Card>
              <CardContent className="p-6 text-sm text-muted-foreground">Загрузка уведомлений...</CardContent>
            </Card>
          )}

          {!notificationsQuery.isLoading && notifications.length === 0 && (
            <Card>
              <CardContent className="p-6 text-sm text-muted-foreground">Уведомлений пока нет.</CardContent>
            </Card>
          )}

          {notifications.map((notification) => (
            <Card key={notification.id} className={notification.status === "UNREAD" ? "border-primary/40" : ""}>
              <CardContent className="flex flex-col gap-4 p-4 md:flex-row md:items-start md:justify-between">
                <div className="space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-semibold">{notification.title}</h3>
                    <Badge variant={notification.status === "UNREAD" ? "default" : "secondary"}>
                      {notification.status === "UNREAD" ? "Новое" : "Прочитано"}
                    </Badge>
                    <Badge variant="outline">{typeLabel(notification.type)}</Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{notification.message}</p>
                  <div className="text-xs text-muted-foreground">{formatDate(notification.createdAt)}</div>
                </div>

                {notification.status === "UNREAD" && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => markReadMutation.mutate(notification.id)}
                    disabled={markReadMutation.isPending}
                  >
                    <Check className="mr-2 h-4 w-4" />
                    Прочитано
                  </Button>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </Layout>
  );
}

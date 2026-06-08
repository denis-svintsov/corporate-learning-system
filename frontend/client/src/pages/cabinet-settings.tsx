import { Layout } from "@/components/layout/Layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchUserSettings, updateUserSettings } from "@/lib/usersApi";
import { useEffect, useState } from "react";
import { useToast } from "@/hooks/use-toast";
import { CabinetNav } from "@/components/cabinet/CabinetNav";

export default function CabinetSettingsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [pushNotifications, setPushNotifications] = useState(true);
  const [language, setLanguage] = useState("ru");
  const [timezone, setTimezone] = useState("Europe/Moscow");

  const { data: settings, isLoading } = useQuery({
    queryKey: ["user-settings", user?.id],
    queryFn: () => fetchUserSettings(user!.id),
    enabled: !!user?.id,
  });

  useEffect(() => {
    if (!settings) return;
    setEmailNotifications(settings.emailNotifications);
    setPushNotifications(settings.pushNotifications);
    setLanguage(settings.language || "ru");
    setTimezone(settings.timezone || "Europe/Moscow");
  }, [settings]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!user?.id) throw new Error("Пользователь не авторизован");
      return updateUserSettings(user.id, {
        emailNotifications,
        pushNotifications,
        language,
        timezone,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-settings", user?.id] });
      toast({ title: "Настройки сохранены" });
    },
    onError: (err: Error) => {
      toast({
        title: "Ошибка сохранения",
        description: err.message,
        variant: "destructive",
      });
    },
  });

  return (
    <Layout>
      <div className="space-y-6">
        <h1 className="text-3xl font-bold tracking-tight text-secondary">Личный кабинет</h1>
        <CabinetNav />

        <Card>
          <CardHeader>
            <CardTitle>Настройки</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {isLoading && <div className="text-sm text-muted-foreground">Загрузка настроек...</div>}
            {!isLoading && (
              <>
                <div className="flex items-center justify-between">
                  <Label htmlFor="email-notifications">Email-уведомления</Label>
                  <Switch
                    id="email-notifications"
                    checked={emailNotifications}
                    onCheckedChange={setEmailNotifications}
                  />
                </div>
                <div className="flex items-center justify-between">
                  <Label htmlFor="push-notifications">Push-уведомления</Label>
                  <Switch
                    id="push-notifications"
                    checked={pushNotifications}
                    onCheckedChange={setPushNotifications}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="language">Язык</Label>
                  <Input id="language" value={language} onChange={(e) => setLanguage(e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="timezone">Часовой пояс</Label>
                  <Input id="timezone" value={timezone} onChange={(e) => setTimezone(e.target.value)} />
                </div>
                <Button
                  className="w-full"
                  onClick={() => saveMutation.mutate()}
                  disabled={saveMutation.isPending}
                >
                  {saveMutation.isPending ? "Сохранение..." : "Сохранить настройки"}
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}

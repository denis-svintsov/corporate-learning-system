import { Layout } from "@/components/layout/Layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery } from "@tanstack/react-query";
import { fetchUserProfile } from "@/lib/usersApi";
import { CabinetNav } from "@/components/cabinet/CabinetNav";

export default function CabinetProfilePage() {
  const { user } = useAuth();

  const { data: profile, isLoading } = useQuery({
    queryKey: ["user-profile", user?.id],
    queryFn: () => fetchUserProfile(user!.id),
    enabled: !!user?.id,
  });

  return (
    <Layout>
      <div className="space-y-6">
        <h1 className="text-3xl font-bold tracking-tight text-secondary">Личный кабинет</h1>
        <CabinetNav />

        <Card>
          <CardHeader>
            <CardTitle>Данные сотрудника</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading && <div className="text-sm text-muted-foreground">Загрузка профиля...</div>}
            {!isLoading && profile && (
              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <div className="text-xs text-muted-foreground">ФИО</div>
                  <div className="font-medium">{profile.lastName} {profile.firstName}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Email</div>
                  <div className="font-medium">{profile.email}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Должность</div>
                  <div className="font-medium">{profile.positionTitle || "Не указана"}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Отдел</div>
                  <div className="font-medium">{profile.departmentName || "Не указан"}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground">Дата приема</div>
                  <div className="font-medium">{profile.hireDate || "Не указана"}</div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}

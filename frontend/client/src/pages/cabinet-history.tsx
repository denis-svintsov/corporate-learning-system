import { Layout } from "@/components/layout/Layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useQuery } from "@tanstack/react-query";
import { fetchUserCabinetHistory } from "@/lib/usersApi";
import { CabinetNav } from "@/components/cabinet/CabinetNav";

export default function CabinetHistoryPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["user-cabinet-history"],
    queryFn: () => fetchUserCabinetHistory(50),
  });

  return (
    <Layout>
      <div className="space-y-6">
        <h1 className="text-3xl font-bold tracking-tight text-secondary">Личный кабинет</h1>
        <CabinetNav />

        <Card>
          <CardHeader>
            <CardTitle>История обучения</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {isLoading && <div className="text-sm text-muted-foreground">Загрузка истории...</div>}
            {!isLoading && (data?.length ?? 0) === 0 && (
              <div className="text-sm text-muted-foreground">История пока пустая.</div>
            )}
            {(data ?? []).map((item) => (
              <div key={item.id} className="rounded-md border p-3">
                <div className="flex items-center justify-between gap-2">
                  <div className="font-medium text-sm">{item.action}</div>
                  <div className="text-xs text-muted-foreground">
                    {new Date(item.timestamp).toLocaleString("ru-RU")}
                  </div>
                </div>
                {item.details && (
                  <div className="mt-1 text-xs text-muted-foreground break-all">{item.details}</div>
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}

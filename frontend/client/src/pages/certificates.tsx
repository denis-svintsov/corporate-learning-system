import { Layout } from "@/components/layout/Layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Search, FileText, Calendar } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery } from "@tanstack/react-query";
import { downloadCertificate, fetchCourse, fetchMyCertificates } from "@/lib/coursesApi";
import { useEffect, useMemo, useState } from "react";
import { useToast } from "@/hooks/use-toast";

export default function Certificates() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [search, setSearch] = useState("");
  const [courseTitles, setCourseTitles] = useState<Record<string, string>>({});
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["certificates", user?.id],
    queryFn: () => fetchMyCertificates(user!.id),
    enabled: !!user?.id,
    refetchInterval: 10000,
    refetchOnWindowFocus: true,
  });

  const certificates = data ?? [];

  useEffect(() => {
    let active = true;
    const loadTitles = async () => {
      const missing = certificates
        .map((c) => c.courseId)
        .filter((id) => id && !courseTitles[id]);
      if (missing.length === 0) return;
      const entries = await Promise.all(
        missing.map(async (id) => {
          try {
            const course = await fetchCourse(id);
            return [id, course.title] as const;
          } catch {
            return [id, "Курс"] as const;
          }
        }),
      );
      if (!active) return;
      setCourseTitles((prev) => {
        const next = { ...prev };
        entries.forEach(([id, title]) => {
          next[id] = title;
        });
        return next;
      });
    };
    loadTitles();
    return () => {
      active = false;
    };
  }, [certificates, courseTitles]);

  const handleDownload = async (id: string, title: string) => {
    if (!user?.id) return;
    try {
      const blob = await downloadCertificate(id, user.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      const safeTitle = title.replace(/[\\/:*?"<>|]+/g, " ").trim() || "Сертификат";
      link.download = `${safeTitle}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      toast({
        title: "Ошибка скачивания",
        description: (err as Error).message,
        variant: "destructive",
      });
    }
  };

  const formattedCertificates = useMemo(() => {
    const term = search.trim().toLowerCase();
    return certificates
      .map((cert) => ({
        ...cert,
        title: courseTitles[cert.courseId] ?? "Название курса загружается...",
        issueDate: cert.issueDate ? new Date(cert.issueDate) : null,
      }))
      .filter((cert) => !term || cert.title.toLowerCase().includes(term))
      .sort((a, b) => {
        const left = a.issueDate?.getTime() ?? 0;
        const right = b.issueDate?.getTime() ?? 0;
        return right - left;
      });
  }, [certificates, courseTitles, search]);

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-secondary">Мои сертификаты</h1>
            <p className="text-muted-foreground">Просматривайте и скачивайте полученные сертификаты</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
            <div className="relative w-full max-w-sm">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="Поиск по названию..."
                className="pl-9"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {isLoading && (
            <div className="col-span-full rounded-lg border p-6 text-center text-sm text-muted-foreground">
              Загрузка сертификатов...
            </div>
          )}
          {isError && (
            <div className="col-span-full rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
              Не удалось загрузить сертификаты. {(error as Error).message}
            </div>
          )}
          {!isLoading && !isError && formattedCertificates.length === 0 && (
            <div className="col-span-full rounded-lg border p-6 text-center text-sm text-muted-foreground">
              Сертификаты пока отсутствуют.
            </div>
          )}
          {formattedCertificates.map((cert) => (
            <Card key={cert.id} className="group overflow-hidden transition-all hover:shadow-lg">
              <div className="aspect-[4/3] bg-muted relative flex items-center justify-center p-8 bg-slate-100">
                <FileText className="h-16 w-16 text-slate-300" />
                <div className="absolute inset-0 bg-black/0 group-hover:bg-black/5 transition-colors" />
              </div>
              <CardContent className="p-4">
                <h3 className="font-semibold line-clamp-1">{cert.title}</h3>
                <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-3 w-3" />
                    {cert.issueDate ? cert.issueDate.toLocaleDateString("ru-RU") : "—"}
                  </span>
                </div>
                <Button
                  variant="outline"
                  className="w-full mt-4 h-8 text-xs"
                  onClick={() => handleDownload(cert.id, cert.title)}
                >
                  Скачать PDF
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </Layout>
  );
}

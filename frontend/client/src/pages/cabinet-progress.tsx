import { Layout } from "@/components/layout/Layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useQuery } from "@tanstack/react-query";
import { fetchUserCabinetProgress } from "@/lib/usersApi";
import { CabinetNav } from "@/components/cabinet/CabinetNav";
import { fetchAssignedCourses } from "@/lib/coursesApi";
import { CircleCheck, CircleDashed, CircleDot } from "lucide-react";

function assignmentProgress(status?: string | null) {
  if (status === "COMPLETED") return 100;
  if (status === "IN_PROGRESS") return 50;
  if (status === "OVERDUE") return 0;
  if (status === "ASSIGNED") return 10;
  return 0;
}

export default function CabinetProgressPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["user-cabinet-progress"],
    queryFn: () => fetchUserCabinetProgress(),
  });

  const { data: assignedCourses = [] } = useQuery({
    queryKey: ["assigned-courses-for-progress"],
    queryFn: () => fetchAssignedCourses(),
  });

  return (
    <Layout>
      <div className="space-y-6">
        <h1 className="text-3xl font-bold tracking-tight text-secondary">Личный кабинет</h1>
        <CabinetNav />

        <div className="space-y-4">
          {isLoading && (
            <Card>
              <CardContent className="pt-6 text-sm text-muted-foreground">Загрузка прогресса...</CardContent>
            </Card>
          )}
          {!isLoading && (data?.courses?.length ?? 0) === 0 && (
            <Card>
              <CardContent className="pt-6 text-sm text-muted-foreground">Пока нет данных по курсам.</CardContent>
            </Card>
          )}
          {(data?.courses ?? []).map((course) => {
            const assignment = assignedCourses.find((a) => a.courseId === course.courseId);
            const progress = Math.max(course.progressPercentage, assignmentProgress(assignment?.status));

            return (
              <Card key={course.courseId}>
                <CardHeader>
                  <CardTitle>{course.courseTitle}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <div className="mb-2 flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">Статус прохождения</span>
                      <span className="font-semibold">Прогресс {progress}%</span>
                    </div>
                    <Progress value={progress} className="h-2" />
                  </div>

                  <div className="space-y-2 text-sm">
                    <div className="flex items-center gap-2">
                      {assignment ? (
                        <CircleCheck className="h-4 w-4 text-emerald-600" />
                      ) : (
                        <CircleDashed className="h-4 w-4 text-muted-foreground" />
                      )}
                      <span>Курс назначен</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {assignment?.status === "IN_PROGRESS" || assignment?.status === "COMPLETED" ? (
                        <CircleCheck className="h-4 w-4 text-emerald-600" />
                      ) : (
                        <CircleDot className="h-4 w-4 text-muted-foreground" />
                      )}
                      <span>Прохождение начато</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {assignment?.status === "COMPLETED" ? (
                        <CircleCheck className="h-4 w-4 text-emerald-600" />
                      ) : (
                        <CircleDot className="h-4 w-4 text-muted-foreground" />
                      )}
                      <span>Курс завершен</span>
                    </div>
                  </div>

                  <div className="text-xs text-muted-foreground">
                    Уроки: {course.completedLessons}/{course.totalLessons}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>
    </Layout>
  );
}

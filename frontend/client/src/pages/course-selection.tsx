import { Layout } from "@/components/layout/Layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Check, GripVertical, Info } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link } from "wouter";
import { extractApiErrorMessage } from "@/lib/apiError";
import {
  CourseDto,
  fetchAssignmentPolicy,
  fetchAssignedCourses,
  fetchMyAssignmentRequests,
  fetchRecommendedCourses,
  submitAssignmentRequest,
} from "@/lib/coursesApi";

interface Position {
  positionId: string;
  title: string;
}

export default function CourseSelection() {
  const [step, setStep] = useState(1);
  const [selectedCourses, setSelectedCourses] = useState<string[]>([]);
  const [requestComment, setRequestComment] = useState("");
  const { toast } = useToast();
  const { user } = useAuth();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["courses-recommended", user?.positionId],
    queryFn: () => fetchRecommendedCourses({ page: 0, size: 60 }),
  });

  const { data: assignedCourses = [] } = useQuery({
    queryKey: ["assigned-courses", user?.id],
    queryFn: () => fetchAssignedCourses(),
    enabled: !!user?.id,
  });

  const { data: myRequests = [] } = useQuery({
    queryKey: ["assignment-requests-my", user?.id],
    queryFn: () => fetchMyAssignmentRequests(),
    enabled: !!user?.id,
  });

  const { data: assignmentPolicy } = useQuery({
    queryKey: ["assignment-policy"],
    queryFn: () => fetchAssignmentPolicy(),
  });

  const authBaseUrl =
    import.meta.env.VITE_AUTH_API_URL ?? "http://localhost:8080/auth";

  const { data: positions = [] } = useQuery({
    queryKey: ["positions"],
    queryFn: async (): Promise<Position[]> => {
      const res = await fetch(`${authBaseUrl}/positions`);
      if (!res.ok) {
        throw new Error(await extractApiErrorMessage(res, "Не удалось загрузить список должностей"));
      }
      return res.json();
    },
  });

  const positionTitle = useMemo(() => {
    if (!user?.positionId) return null;
    const found = positions.find((p) => p.positionId === user.positionId);
    return found?.title ?? null;
  }, [positions, user?.positionId]);
  const hasPosition = !!user?.positionId;

  const isInCurrentQuarter = (iso?: string | null) => {
    if (!iso) return false;
    const d = new Date(iso);
    const now = new Date();
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

  const quarterLimit = assignmentPolicy?.maxCoursesPerQuarter ?? 3;
  const currentQuarterAssignedCount = assignedCourses.filter(
    (c) => isActiveAssignmentStatus(c.status) && isInCurrentQuarter(c.createdAt),
  ).length;
  const carriedUndatedActiveCount = assignedCourses.filter((c) => {
    if (!isActiveAssignmentStatus(c.status)) return false;
    if (isInCurrentQuarter(c.createdAt)) return false;
    if (c.dueDate || c.courseEndDate) return false;
    if (!c.createdAt) return true;
    const created = new Date(c.createdAt);
    if (Number.isNaN(created.getTime())) return true;
    return created < currentQuarterStart;
  }).length;
  const currentQuarterPendingCount = myRequests.filter(
    (request) => request.status === "PENDING" && isInCurrentQuarter(request.createdAt),
  ).length;
  const currentQuarterTotalCount = currentQuarterAssignedCount + carriedUndatedActiveCount + currentQuarterPendingCount;
  const isQuarterLimitReached = currentQuarterTotalCount >= quarterLimit;

  const assignedCourseIds = new Set(assignedCourses.map((c) => c.courseId).filter(Boolean));
  const pendingCourseIds = new Set(
    myRequests
      .filter((request) => request.status === "PENDING" && !!request.courseId)
      .map((request) => request.courseId),
  );
  const courses = (data?.content ?? []).filter((c) => !assignedCourseIds.has(c.id));
  const selectableCoursesCount = courses.filter((c) => !pendingCourseIds.has(c.id)).length;
  const remainingQuota = Math.max(quarterLimit - currentQuarterTotalCount, 0);
  const requiredSelectionCount = Math.min(remainingQuota, selectableCoursesCount);
  const selectedCourseList = useMemo(
    () => courses.filter((course) => selectedCourses.includes(course.id)),
    [courses, selectedCourses],
  );

  useEffect(() => {
    if (selectedCourses.length > requiredSelectionCount) {
      setSelectedCourses((prev) => prev.slice(0, requiredSelectionCount));
    }
  }, [requiredSelectionCount, selectedCourses.length]);

  const assignMutation = useMutation({
    mutationFn: async (courseIds: string[]) => {
      if (!user?.id) {
        throw new Error("Пользователь не авторизован");
      }
      await Promise.all(
        courseIds.map((courseId) =>
          submitAssignmentRequest({
            userId: user.id,
            courseId,
            requestedBy: user.id,
            comment: requestComment.trim() || undefined,
          }),
        ),
      );
    },
  });

  const toggleCourse = (id: string) => {
    if (pendingCourseIds.has(id)) {
      toast({
        title: "Курс уже на модерации",
        description: "Этот курс уже отправлен на согласование и недоступен для повторного выбора.",
        variant: "destructive"
      });
      return;
    }
    if (selectedCourses.includes(id)) {
      setSelectedCourses(prev => prev.filter(c => c !== id));
    } else {
      if (selectedCourses.length >= requiredSelectionCount) {
        toast({
          title: `Максимум ${requiredSelectionCount} курса`,
          description: `Вы не можете выбрать более ${requiredSelectionCount} курсов одновременно.`,
          variant: "destructive"
        });
        return;
      }
      setSelectedCourses(prev => [...prev, id]);
    }
  };

  const nextStep = () => {
    if (step === 1 && !hasPosition) {
      toast({
        title: "Должность не указана",
        description: "Выбор курсов доступен после указания должности в профиле.",
        variant: "destructive"
      });
      return;
    }
    if (step === 2 && selectableCoursesCount === 0) {
      toast({
        title: "Нет доступных курсов",
        description: "Сейчас нет курсов, доступных для выбора в этом квартале.",
        variant: "destructive"
      });
      return;
    }
    if (step === 2 && requiredSelectionCount > 0 && selectedCourses.length !== requiredSelectionCount) {
      toast({
        title: `Выберите ${requiredSelectionCount} курса`,
        description: `Для продолжения необходимо выбрать ровно ${requiredSelectionCount} курсов.`,
        variant: "destructive"
      });
      return;
    }
    if (step === 2 && isQuarterLimitReached) {
      toast({
        title: "Курсы уже назначены",
        description: `В текущем квартале у вас уже ${currentQuarterTotalCount} из ${quarterLimit} курсов (назначенные + заявки).`,
        variant: "destructive"
      });
      return;
    }
    setStep(prev => prev + 1);
  };

  const prevStep = () => setStep(prev => prev - 1);

  const difficultyLabel = (course: CourseDto) => {
    if (!course.difficulty) return "Сложность не указана";
    if (course.difficulty === "BEGINNER") return "Начальный";
    if (course.difficulty === "INTERMEDIATE") return "Средний";
    return "Продвинутый";
  };

  const durationLabel = (course: CourseDto) => {
    if (!course.durationMinutes) return "Длительность не указана";
    const hours = Math.round((course.durationMinutes / 60) * 10) / 10;
    return `${hours} ч`;
  };

  return (
    <Layout>
      <div className="mx-auto max-w-5xl space-y-8">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-secondary">Выбор курсов на текущий квартал</h1>
          <p className="text-muted-foreground">Пройдите этапы выбора для формирования плана обучения</p>
        </div>

        {/* Wizard Progress */}
        <div className="relative">
          <Progress value={(step / 4) * 100} className="h-2" />
          <div className="absolute top-0 mt-[-6px] flex w-full justify-between px-1">
            {[1, 2, 3, 4].map((s) => (
              <div 
                key={s}
                className={`flex h-5 w-5 items-center justify-center rounded-full text-xs font-bold transition-colors ${
                  step >= s ? 'bg-primary text-white' : 'bg-muted text-muted-foreground'
                }`}
              >
                {s}
              </div>
            ))}
          </div>
          <div className="mt-2 flex w-full justify-between px-0 text-xs text-muted-foreground">
            <span>Должность</span>
            <span>Выбор курсов</span>
            <span>Утверждение</span>
            <span>Готово</span>
          </div>
        </div>

        {/* Step 1: Position */}
        {step === 1 && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <Card>
              <CardContent className="p-6">
                <h2 className="text-xl font-semibold mb-4">Ваша должность</h2>
                <div className="space-y-4">
                  <div className="rounded-lg border bg-card p-4">
                    <div className="text-sm text-muted-foreground">Должность из профиля</div>
                    <div className="mt-1 text-lg font-semibold">
                      {positionTitle ?? (user?.positionId ? "Должность не найдена" : "Не указана")}
                    </div>
                  </div>
                  {!hasPosition && (
                    <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
                      Чтобы выбрать курсы, сначала укажите должность в профиле.
                    </div>
                  )}
                  <div className="flex justify-end">
                    <Button onClick={nextStep} disabled={!hasPosition}>Продолжить</Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {/* Step 2: Course Selection */}
        {step === 2 && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold">Выберите {requiredSelectionCount} курса из списка</h2>
              <div className="text-sm font-medium">
                Выбрано: <span className={selectedCourses.length === requiredSelectionCount ? "text-green-600" : "text-primary"}>{selectedCourses.length}/{requiredSelectionCount}</span>
              </div>
            </div>

            {isLoading && (
              <div className="rounded-lg border p-6 text-center text-sm text-muted-foreground">
                Загрузка курсов...
              </div>
            )}
            {isError && (
              <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
                Не удалось загрузить курсы. {(error as Error).message}
              </div>
            )}
            {isQuarterLimitReached && (
              <div className="rounded-lg border border-amber-300 bg-amber-50 p-6 text-sm text-amber-900">
                Лимит достигнут: в текущем квартале у вас уже {currentQuarterTotalCount} из {quarterLimit} курсов
                (назначенные + заявки на согласовании).
              </div>
            )}
            {!isLoading && !isError && selectableCoursesCount === 0 && (
              <div className="rounded-lg border p-6 text-center text-sm text-muted-foreground">
                Нет курсов, доступных для выбора.
              </div>
            )}
            <div className="grid gap-4">
              {courses.map((course) => {
                const isPending = pendingCourseIds.has(course.id);
                return (
                <div 
                  key={course.id}
                  className={`group relative flex items-start gap-4 rounded-lg border p-4 transition-all ${
                    isPending ? "opacity-80" : "hover:shadow-md"
                  } ${
                    selectedCourses.includes(course.id) ? 'border-primary bg-primary/5 ring-1 ring-primary' : 'bg-card'
                  }`}
                  onClick={() => {
                    if (isQuarterLimitReached || isPending) return;
                    toggleCourse(course.id);
                  }}
                >
                  <div className="h-24 w-40 flex-shrink-0 overflow-hidden rounded-md bg-gradient-to-br from-slate-100 via-slate-50 to-slate-200 flex items-center justify-center text-slate-500 text-xs font-medium">
                    Курс
                  </div>
                  <div className="flex-1">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h3 className="font-bold">{course.title}</h3>
                        <p className="text-sm text-muted-foreground">
                          {difficultyLabel(course)} • {durationLabel(course)}
                        </p>
                      </div>
                    </div>
                    <p className="mt-2 text-sm text-muted-foreground line-clamp-2">
                      {course.description || "Описание пока не заполнено."}
                    </p>
                    <div className="mt-3 flex gap-2">
                      <Badge variant="secondary" className="text-xs">{course.categoryId ?? "Без категории"}</Badge>
                      <Badge variant="outline" className="text-xs">{course.status ?? "Статус"}</Badge>
                      {isPending && <Badge className="text-xs bg-amber-500">На модерации</Badge>}
                    </div>
                  </div>
                  <div className={`absolute right-4 top-4 h-6 w-6 rounded-full border-2 transition-colors ${
                    isPending
                      ? "border-amber-500 bg-amber-500 text-white"
                      : selectedCourses.includes(course.id)
                        ? 'border-primary bg-primary text-white'
                        : 'border-muted-foreground'
                  }`}>
                    {isPending && <Check className="h-4 w-4 m-0.5" />}
                    {!isPending && selectedCourses.includes(course.id) && <Check className="h-4 w-4 m-0.5" />}
                  </div>
                </div>
                );
              })}
            </div>

            <div className="flex justify-end gap-3 pt-4">
              <Button variant="outline" onClick={prevStep}>Назад</Button>
              <Button
                onClick={nextStep}
                disabled={
                  isQuarterLimitReached ||
                  selectableCoursesCount === 0 ||
                  (requiredSelectionCount > 0 && selectedCourses.length !== requiredSelectionCount)
                }
              >
                Далее
              </Button>
            </div>
          </motion.div>
        )}

        {/* Step 3: Confirmation */}
        {step === 3 && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <Card>
              <CardContent className="p-6">
                <h2 className="text-xl font-semibold mb-6">Приоритизация и отправка</h2>
                
                <div className="space-y-4 mb-8">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground bg-blue-50 p-3 rounded-md border border-blue-100 text-blue-800">
                    <Info className="h-4 w-4" />
                    Перетащите курсы, чтобы расставить приоритеты. Курс №1 будет рассмотрен в первую очередь.
                  </div>

                  {selectedCourseList.map((course, index) => (
                    <div key={course.id} className="flex items-center gap-4 rounded-lg border bg-card p-4 shadow-sm">
                      <div className="flex h-8 w-8 cursor-move items-center justify-center text-muted-foreground hover:text-foreground">
                        <GripVertical className="h-5 w-5" />
                      </div>
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary text-white font-bold text-sm">
                        {index + 1}
                      </div>
                      <div className="flex-1">
                        <h4 className="font-semibold">{course.title}</h4>
                        <p className="text-sm text-muted-foreground">{difficultyLabel(course)}</p>
                      </div>
                      <div className="font-medium">{durationLabel(course)}</div>
                    </div>
                  ))}
                  
                  <div className="flex justify-end border-t pt-4">
                    <div className="text-right">
                      <p className="text-sm text-muted-foreground">Итоговая длительность:</p>
                      <p className="text-xl font-bold text-primary">
                        {selectedCourseList.reduce((sum, c) => sum + (c.durationMinutes ?? 0), 0)} мин
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <label className="text-sm font-medium">Комментарий для руководителя (обоснование выбора)</label>
                  <Textarea
                    placeholder="Опишите, как эти курсы помогут в вашей работе..."
                    className="min-h-[100px]"
                    value={requestComment}
                    onChange={(e) => setRequestComment(e.target.value)}
                  />
                </div>

                <div className="flex justify-end gap-3 pt-6">
                  <Button variant="outline" onClick={prevStep}>Назад</Button>
                  <Button
                    className="bg-green-600 hover:bg-green-700"
                    disabled={assignMutation.isPending}
                    onClick={async () => {
                      try {
                        await assignMutation.mutateAsync(selectedCourses);
                        setStep(4);
                      } catch (err) {
                        toast({
                          title: "Ошибка при отправке",
                          description: (err as Error).message,
                          variant: "destructive",
                        });
                      }
                    }}
                  >
                    Отправить на согласование
                  </Button>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {/* Step 4: Done */}
        {step === 4 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <Card>
              <CardContent className="p-6">
                <div className="space-y-6">
                  <h2 className="text-2xl font-semibold">Курс отправлен на согласование</h2>
                  <div className="rounded-lg border bg-emerald-50 p-4 text-sm text-emerald-900 border-emerald-200">
                    Ваши заявки отправлены на модерацию. После утверждения курсы появятся в назначенных.
                  </div>
                  <div className="flex justify-end">
                    <Button asChild>
                      <Link href="/catalog">Перейти к назначенным курсам</Link>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}
      </div>
    </Layout>
  );
}

import { Layout } from "@/components/layout/Layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/contexts/AuthContext";
import {
  CourseDto,
  CoursePayload,
  CourseStatus,
  DifficultyLevel,
  createCourse,
  fetchManagedCourses,
  updateCourse,
  uploadCourseCover,
} from "@/lib/coursesApi";
import { DepartmentDto, PositionDto, fetchDepartments, fetchPositions } from "@/lib/usersApi";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, Check, ChevronsUpDown, Edit, ImageUp, Plus, Save, Search, X } from "lucide-react";
import { ReactNode, useMemo, useState } from "react";

const ROLE_OPTIONS = [
  { value: "USER", label: "Сотрудник" },
  { value: "MANAGER", label: "Руководитель подразделения" },
  { value: "HR", label: "HR-менеджер" },
  { value: "TECHNOLOG", label: "Технолог" },
  { value: "EXPERT", label: "Эксперт" },
  { value: "ADMIN", label: "Администратор" },
];

function statusLabel(status?: CourseStatus | null) {
  if (status === "ACTIVE") return "Активен";
  if (status === "ARCHIVED") return "Архив";
  return "Черновик";
}

function difficultyLabel(difficulty?: DifficultyLevel | null) {
  if (difficulty === "INTERMEDIATE") return "Средний";
  if (difficulty === "ADVANCED") return "Продвинутый";
  return "Начальный";
}

function formatDate(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toLocaleDateString("ru-RU");
}

function nearestCourseDate(course: CourseDto) {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const timestamps = [course.startDate, course.endDate]
    .map((value) => {
      if (!value) return null;
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return null;
      date.setHours(0, 0, 0, 0);
      return date.getTime();
    })
    .filter((value): value is number => value !== null);

  const upcoming = timestamps.filter((value) => value >= now.getTime()).sort((a, b) => a - b);
  if (upcoming[0] != null) return { group: 0, value: upcoming[0] };
  const past = timestamps.sort((a, b) => b - a);
  if (past[0] != null) return { group: 1, value: -past[0] };
  return { group: 2, value: Number.MAX_SAFE_INTEGER };
}

const emptyForm = {
  title: "",
  description: "",
  difficulty: "BEGINNER" as DifficultyLevel,
  status: "DRAFT" as CourseStatus,
  durationMinutes: "",
  allowedRoles: ["USER"] as string[],
  allowedDepartmentIds: [] as string[],
  specializations: [] as string[],
  instructions: "",
  aggregatorUrl: "",
  coverUrl: "",
  companyCost: "",
  partnerName: "",
  partnerLocation: "",
  startDate: "",
  endDate: "",
};

type CourseFormState = typeof emptyForm;

type Option = {
  value: string;
  label: string;
  description?: string | null;
};

function toForm(course: CourseDto): CourseFormState {
  return {
    title: course.title ?? "",
    description: course.description ?? "",
    difficulty: course.difficulty ?? "BEGINNER",
    status: course.status ?? "DRAFT",
    durationMinutes: course.durationMinutes == null ? "" : String(course.durationMinutes),
    allowedRoles: course.allowedRoles ?? ["USER"],
    allowedDepartmentIds: course.allowedDepartmentIds ?? [],
    specializations: course.specializations ?? [],
    instructions: course.instructions ?? "",
    aggregatorUrl: course.aggregatorUrl ?? "",
    coverUrl: course.coverUrl ?? "",
    companyCost: course.companyCost == null ? "" : String(course.companyCost),
    partnerName: course.partnerName ?? "",
    partnerLocation: course.partnerLocation ?? "",
    startDate: course.startDate ?? "",
    endDate: course.endDate ?? "",
  };
}

function toPayload(form: CourseFormState): CoursePayload & { status: CourseStatus } {
  return {
    title: form.title.trim(),
    description: form.description.trim(),
    difficulty: form.difficulty,
    status: form.status,
    durationMinutes: form.durationMinutes ? Number(form.durationMinutes) : null,
    allowedRoles: form.allowedRoles,
    allowedDepartmentIds: form.allowedDepartmentIds,
    specializations: form.specializations,
    instructions: form.instructions.trim() || null,
    aggregatorUrl: form.aggregatorUrl.trim() || null,
    coverUrl: form.coverUrl.trim() || null,
    companyCost: form.companyCost ? Number(form.companyCost) : null,
    partnerName: form.partnerName.trim() || null,
    partnerLocation: form.partnerLocation.trim() || null,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
  };
}

function MultiCombobox({
  label,
  options,
  selected,
  placeholder,
  emptyText,
  onChange,
}: {
  label: string;
  options: Option[];
  selected: string[];
  placeholder: string;
  emptyText: string;
  onChange: (value: string[]) => void;
}) {
  const selectedOptions = options.filter((option) => selected.includes(option.value));
  const summary = selectedOptions.length === 0
    ? placeholder
    : selectedOptions.length === 1
      ? selectedOptions[0].label
      : `Выбрано: ${selectedOptions.length}`;

  const toggle = (value: string) => {
    if (selected.includes(value)) {
      onChange(selected.filter((item) => item !== value));
      return;
    }
    onChange([...selected, value]);
  };

  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <Popover>
        <PopoverTrigger asChild>
          <Button variant="outline" role="combobox" className="min-h-10 w-full justify-between px-3">
            <span className={cn("truncate text-left font-normal", selectedOptions.length === 0 && "text-muted-foreground")}>
              {summary}
            </span>
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-[var(--radix-popover-trigger-width)] p-0" align="start">
          <Command>
            <CommandInput placeholder="Поиск..." />
            <CommandList>
              <CommandEmpty>{emptyText}</CommandEmpty>
              <CommandGroup>
                {options.map((option) => (
                  <CommandItem key={option.value} value={`${option.label} ${option.value}`} onSelect={() => toggle(option.value)}>
                    <span
                      className={cn(
                        "flex h-4 w-4 shrink-0 items-center justify-center rounded border",
                        selected.includes(option.value) ? "border-primary bg-primary text-primary-foreground" : "border-muted-foreground/30",
                      )}
                    >
                      {selected.includes(option.value) && <Check className="h-3 w-3" />}
                    </span>
                    <div className="min-w-0">
                      <div className="truncate">{option.label}</div>
                      {option.description && (
                        <div className="truncate text-xs text-muted-foreground">{option.description}</div>
                      )}
                    </div>
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
      {selectedOptions.length > 0 && (
        <div className="rounded-md border bg-muted/30 p-2">
          <div className="grid gap-1 sm:grid-cols-2">
            {selectedOptions.map((option) => (
              <div key={option.value} className="flex min-w-0 items-center justify-between gap-2 rounded bg-background px-2 py-1.5 text-sm">
                <span className="truncate">{option.label}</span>
                <button
                  type="button"
                  className="rounded p-0.5 text-muted-foreground transition hover:bg-muted hover:text-foreground"
                  onClick={() => toggle(option.value)}
                  aria-label={`Убрать ${option.label}`}
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function CoverPreview({ coverUrl }: { coverUrl: string }) {
  if (!coverUrl) {
    return (
      <div className="flex aspect-video items-center justify-center rounded-md border bg-muted text-sm text-muted-foreground">
        Обложка не загружена
      </div>
    );
  }
  return (
    <div
      className="aspect-video rounded-md border bg-cover bg-center"
      style={{ backgroundImage: `url(${coverUrl})` }}
    />
  );
}

function FieldBlock({ children }: { children: ReactNode }) {
  return <div className="space-y-2">{children}</div>;
}

export default function CourseManagementPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [form, setForm] = useState<CourseFormState>(emptyForm);

  const canEditCourses = (user?.roles ?? []).some((role) => ["ADMIN", "TECHNOLOG"].includes(role));

  const coursesQuery = useQuery({
    queryKey: ["managed-courses", query],
    queryFn: () => fetchManagedCourses({ q: query, size: 100 }),
    enabled: canEditCourses,
  });

  const positionsQuery = useQuery({
    queryKey: ["positions"],
    queryFn: fetchPositions,
    enabled: canEditCourses,
  });

  const departmentsQuery = useQuery({
    queryKey: ["departments"],
    queryFn: fetchDepartments,
    enabled: canEditCourses,
  });

  const positionOptions: Option[] = useMemo(
    () => (positionsQuery.data ?? []).map((position: PositionDto) => ({
      value: position.positionId,
      label: position.title,
      description: position.grade,
    })),
    [positionsQuery.data],
  );

  const departmentOptions: Option[] = useMemo(
    () => (departmentsQuery.data ?? []).map((department: DepartmentDto) => ({
      value: department.departmentId,
      label: department.name,
      description: department.description,
    })),
    [departmentsQuery.data],
  );

  const courses = useMemo(() => {
    const source = coursesQuery.data?.content ?? [];
    return [...source].sort((a, b) => {
      const left = nearestCourseDate(a);
      const right = nearestCourseDate(b);
      if (left.group !== right.group) return left.group - right.group;
      if (left.value !== right.value) return left.value - right.value;
      return (a.title ?? "").localeCompare(b.title ?? "", "ru");
    });
  }, [coursesQuery.data]);
  const selectedCourse = useMemo(
    () => courses.find((course) => course.id === selectedCourseId) ?? null,
    [courses, selectedCourseId],
  );

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = toPayload(form);
      if (!payload.title) {
        throw new Error("Укажите название курса.");
      }
      if (selectedCourse) {
        return updateCourse(selectedCourse.id, payload);
      }
      const { status, ...createPayload } = payload;
      void status;
      return createCourse(createPayload);
    },
    onSuccess: (course) => {
      queryClient.invalidateQueries({ queryKey: ["managed-courses"] });
      setSelectedCourseId(course.id);
      setForm(toForm(course));
      toast({ title: "Курс сохранен", description: course.title });
    },
    onError: (error) => {
      toast({
        title: "Не удалось сохранить курс",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const coverMutation = useMutation({
    mutationFn: uploadCourseCover,
    onSuccess: (result) => {
      setField("coverUrl", result.coverUrl);
      toast({ title: "Обложка загружена", description: "Файл сохранен в MinIO." });
    },
    onError: (error) => {
      toast({
        title: "Не удалось загрузить обложку",
        description: (error as Error).message,
        variant: "destructive",
      });
    },
  });

  const setField = <K extends keyof CourseFormState>(key: K, value: CourseFormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  if (!canEditCourses) {
    return (
      <Layout>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
          Доступ запрещен. Управление курсами доступно технологу и администратору.
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-secondary">Управление курсами</h1>
            <p className="text-muted-foreground">Создание и редактирование учебных курсов</p>
          </div>
          <Button
            onClick={() => {
              setSelectedCourseId(null);
              setForm(emptyForm);
            }}
          >
            <Plus className="h-4 w-4" />
            Новый курс
          </Button>
        </div>

        <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Курсы</CardTitle>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className="pl-9"
                  placeholder="Поиск по названию"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                />
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              {coursesQuery.isLoading && <div className="text-sm text-muted-foreground">Загрузка...</div>}
              {courses.map((course) => (
                <button
                  key={course.id}
                  className={`w-full rounded-md border p-3 text-left transition hover:bg-muted ${
                    selectedCourseId === course.id ? "border-primary bg-primary/5" : ""
                  }`}
                  onClick={() => {
                    setSelectedCourseId(course.id);
                    setForm(toForm(course));
                  }}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="font-medium">{course.title}</div>
                    <Badge variant={course.status === "ACTIVE" ? "default" : "secondary"}>
                      {statusLabel(course.status)}
                    </Badge>
                  </div>
                  <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                    <BookOpen className="h-3.5 w-3.5" />
                    {difficultyLabel(course.difficulty)}
                    <span>•</span>
                    <span>{formatDate(course.startDate) ?? formatDate(course.endDate) ?? "без даты"}</span>
                  </div>
                </button>
              ))}
              {!coursesQuery.isLoading && courses.length === 0 && (
                <div className="rounded-md border p-4 text-sm text-muted-foreground">Курсы не найдены.</div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Edit className="h-4 w-4" />
                {selectedCourse ? "Редактирование курса" : "Создание курса"}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="grid gap-4 md:grid-cols-2">
                <FieldBlock>
                  <Label>Название</Label>
                  <Input value={form.title} onChange={(event) => setField("title", event.target.value)} />
                </FieldBlock>
                <FieldBlock>
                  <Label>Сложность</Label>
                  <Select value={form.difficulty} onValueChange={(value) => setField("difficulty", value as DifficultyLevel)}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="BEGINNER">Начальный</SelectItem>
                      <SelectItem value="INTERMEDIATE">Средний</SelectItem>
                      <SelectItem value="ADVANCED">Продвинутый</SelectItem>
                    </SelectContent>
                  </Select>
                </FieldBlock>
                <FieldBlock>
                  <Label>Статус</Label>
                  <Select value={form.status} onValueChange={(value) => setField("status", value as CourseStatus)}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="DRAFT">Черновик</SelectItem>
                      <SelectItem value="ACTIVE">Активен</SelectItem>
                      <SelectItem value="ARCHIVED">Архив</SelectItem>
                    </SelectContent>
                  </Select>
                </FieldBlock>
                <FieldBlock>
                  <Label>Длительность, минут</Label>
                  <Input type="number" min={0} value={form.durationMinutes} onChange={(event) => setField("durationMinutes", event.target.value)} />
                </FieldBlock>
                <FieldBlock>
                  <Label>Стоимость для компании</Label>
                  <Input type="number" min={0} value={form.companyCost} onChange={(event) => setField("companyCost", event.target.value)} />
                </FieldBlock>
                <FieldBlock>
                  <Label>Дата старта</Label>
                  <Input type="date" value={form.startDate} onChange={(event) => setField("startDate", event.target.value)} />
                </FieldBlock>
                <FieldBlock>
                  <Label>Дата окончания</Label>
                  <Input type="date" value={form.endDate} onChange={(event) => setField("endDate", event.target.value)} />
                </FieldBlock>

                <div className="md:col-span-2">
                  <MultiCombobox
                    label="Роли доступа"
                    options={ROLE_OPTIONS}
                    selected={form.allowedRoles}
                    placeholder="Выберите роли"
                    emptyText="Роли не найдены"
                    onChange={(value) => setField("allowedRoles", value)}
                  />
                </div>
                <div className="md:col-span-2">
                  <MultiCombobox
                    label="Целевые должности"
                    options={positionOptions}
                    selected={form.specializations}
                    placeholder="Выберите должности"
                    emptyText="Должности не найдены"
                    onChange={(value) => setField("specializations", value)}
                  />
                </div>
                <div className="md:col-span-2">
                  <MultiCombobox
                    label="Подразделения доступа"
                    options={departmentOptions}
                    selected={form.allowedDepartmentIds}
                    placeholder="Все подразделения"
                    emptyText="Подразделения не найдены"
                    onChange={(value) => setField("allowedDepartmentIds", value)}
                  />
                </div>

                <FieldBlock>
                  <Label>Партнер</Label>
                  <Input value={form.partnerName} onChange={(event) => setField("partnerName", event.target.value)} />
                </FieldBlock>
                <FieldBlock>
                  <Label>Локация партнера</Label>
                  <Input value={form.partnerLocation} onChange={(event) => setField("partnerLocation", event.target.value)} />
                </FieldBlock>
                <div className="space-y-2 md:col-span-2">
                  <Label>Описание</Label>
                  <Textarea rows={4} value={form.description} onChange={(event) => setField("description", event.target.value)} />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <Label>Ссылка на обучение</Label>
                  <Input value={form.aggregatorUrl} onChange={(event) => setField("aggregatorUrl", event.target.value)} />
                </div>
                <div className="space-y-3 md:col-span-2">
                  <Label>Обложка курса</Label>
                  <CoverPreview coverUrl={form.coverUrl} />
                  <div className="flex flex-col gap-2 md:flex-row md:items-center">
                    <Input
                      type="file"
                      accept="image/png,image/jpeg,image/webp"
                      disabled={coverMutation.isPending}
                      onChange={(event) => {
                        const file = event.target.files?.[0];
                        if (file) {
                          coverMutation.mutate(file);
                        }
                        event.target.value = "";
                      }}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      disabled={!form.coverUrl}
                      onClick={() => setField("coverUrl", "")}
                    >
                      <X className="h-4 w-4" />
                      Убрать
                    </Button>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <ImageUp className="h-3.5 w-3.5" />
                    PNG, JPEG или WEBP. Файл сохраняется в MinIO.
                  </div>
                </div>
                <div className="space-y-2 md:col-span-2">
                  <Label>Инструкции</Label>
                  <Textarea rows={4} value={form.instructions} onChange={(event) => setField("instructions", event.target.value)} />
                </div>
              </div>

              <div className="flex justify-end">
                <Button disabled={saveMutation.isPending || coverMutation.isPending} onClick={() => saveMutation.mutate()}>
                  <Save className="h-4 w-4" />
                  Сохранить курс
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </Layout>
  );
}

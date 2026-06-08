import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Building2, ArrowRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { extractApiErrorMessage } from "@/lib/apiError";

interface Department {
  departmentId: string;
  name: string;
}

interface Position {
  positionId: string;
  title: string;
}

export default function AuthPage() {
  const { login, register, isAuthenticated, isLoading: authLoading } = useAuth();
  const { toast } = useToast();
  const [, setLocation] = useLocation();
  const [isLoading, setIsLoading] = useState(false);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [positions, setPositions] = useState<Position[]>([]);
  const [loadingData, setLoadingData] = useState(true);

  // Редирект если уже авторизован
  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      setLocation("/");
    }
  }, [isAuthenticated, authLoading, setLocation]);

  // Форма входа
  const [loginUsername, setLoginUsername] = useState("");
  const [loginPassword, setLoginPassword] = useState("");

  // Форма регистрации
  const [regUsername, setRegUsername] = useState("");
  const [regEmail, setRegEmail] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regFirstName, setRegFirstName] = useState("");
  const [regMiddleName, setRegMiddleName] = useState("");
  const [regLastName, setRegLastName] = useState("");
  const [regPositionId, setRegPositionId] = useState("");
  const [regDepartmentId, setRegDepartmentId] = useState("");
  const [regHireDate, setRegHireDate] = useState("");
  const authBaseUrl = import.meta.env.VITE_AUTH_API_URL ?? `${window.location.origin}/auth`;

  useEffect(() => {
    // Загрузка departments и positions
    const loadData = async () => {
      try {
        const [deptRes, posRes] = await Promise.all([
          fetch(`${authBaseUrl}/departments`),
          fetch(`${authBaseUrl}/positions`),
        ]);

        if (deptRes.ok) {
          const deptData = await deptRes.json();
          setDepartments(deptData);
        }

        if (posRes.ok) {
          const posData = await posRes.json();
          setPositions(posData);
        }
        if (!deptRes.ok || !posRes.ok) {
          const deptMsg = !deptRes.ok ? await extractApiErrorMessage(deptRes, "Не удалось загрузить департаменты") : "";
          const posMsg = !posRes.ok ? await extractApiErrorMessage(posRes, "Не удалось загрузить должности") : "";
          console.error("Ошибка загрузки данных:", [deptMsg, posMsg].filter(Boolean).join("; "));
        }
      } catch (error) {
        console.error("Ошибка загрузки данных:", error);
      } finally {
        setLoadingData(false);
      }
    };

    loadData();
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      await login(loginUsername, loginPassword);
      toast({
        title: "Успешный вход",
        description: "Добро пожаловать в систему обучения",
      });
    } catch (error: any) {
      toast({
        title: "Ошибка входа",
        description: error.message || "Неверный логин или пароль",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      await register({
        username: regUsername,
        email: regEmail,
        password: regPassword,
        firstName: regFirstName,
        middleName: regMiddleName || undefined,
        lastName: regLastName,
        positionId: regPositionId || undefined,
        departmentId: regDepartmentId || undefined,
        hireDate: regHireDate || undefined,
      });
      toast({
        title: "Регистрация успешна",
        description: "Добро пожаловать в систему обучения",
      });
    } catch (error: any) {
      toast({
        title: "Ошибка регистрации",
        description: error.message || "Ошибка при регистрации",
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-muted/20 relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute inset-0 z-0">
        <div className="absolute top-0 left-0 w-full h-1/2 bg-primary/5 -skew-y-6 transform origin-top-left" />
        <div className="absolute bottom-0 right-0 w-full h-1/2 bg-secondary/5 -skew-y-6 transform origin-bottom-right" />
      </div>

      <div className="z-10 w-full max-w-md px-4">
        <div className="flex flex-col items-center mb-8 text-center">
          <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center mb-4">
            <Building2 className="h-8 w-8 text-primary" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight text-secondary">Платформа обучения</h1>
          <p className="text-muted-foreground mt-2">Корпоративная система обучения</p>
        </div>

        <Tabs defaultValue="login" className="w-full">
          <TabsList className="grid w-full grid-cols-2 mb-4">
            <TabsTrigger value="login">Вход</TabsTrigger>
            <TabsTrigger value="register">Регистрация</TabsTrigger>
          </TabsList>
          
          <TabsContent value="login">
            <Card>
              <CardHeader>
                <CardTitle>Вход в систему</CardTitle>
                <CardDescription>
                  Используйте корпоративную учетную запись
                </CardDescription>
              </CardHeader>
              <form onSubmit={handleLogin}>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="login-username">Имя пользователя</Label>
                    <Input
                      id="login-username"
                      placeholder="ivanov.i.i"
                      required
                      value={loginUsername}
                      onChange={(e) => setLoginUsername(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <Label htmlFor="login-password">Пароль</Label>
                      <a href="#" className="text-xs text-primary hover:underline">
                        Забыли пароль?
                      </a>
                    </div>
                    <Input
                      id="login-password"
                      type="password"
                      required
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                    />
                  </div>
                </CardContent>
                <CardFooter>
                  <Button className="w-full" type="submit" disabled={isLoading}>
                    {isLoading ? "Вход..." : "Войти"} <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </CardFooter>
              </form>
            </Card>
          </TabsContent>
          
          <TabsContent value="register">
            <Card>
              <CardHeader>
                <CardTitle>Регистрация сотрудника</CardTitle>
                <CardDescription>
                  Создайте новую учетную запись для доступа к курсам
                </CardDescription>
              </CardHeader>
              <form onSubmit={handleRegister}>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 md:grid-cols-3">
                    <div className="space-y-2">
                      <Label htmlFor="lastName">Фамилия *</Label>
                      <Input
                        id="lastName"
                        placeholder="Иванов"
                        required
                        value={regLastName}
                        onChange={(e) => setRegLastName(e.target.value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="firstName">Имя *</Label>
                      <Input
                        id="firstName"
                        placeholder="Иван"
                        required
                        value={regFirstName}
                        onChange={(e) => setRegFirstName(e.target.value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="middleName">Отчество</Label>
                      <Input
                        id="middleName"
                        placeholder="Иванович"
                        value={regMiddleName}
                        onChange={(e) => setRegMiddleName(e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="username">Имя пользователя *</Label>
                    <Input
                      id="username"
                      placeholder="ivanov.i.i"
                      required
                      value={regUsername}
                      onChange={(e) => setRegUsername(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email">Корпоративный Email *</Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="ivanov.i.i@company.ru"
                      required
                      value={regEmail}
                      onChange={(e) => setRegEmail(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="password">Пароль *</Label>
                    <Input
                      id="password"
                      type="password"
                      required
                      minLength={6}
                      value={regPassword}
                      onChange={(e) => setRegPassword(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="department">Департамент</Label>
                    <Select
                      value={regDepartmentId}
                      onValueChange={setRegDepartmentId}
                      disabled={loadingData}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Выберите департамент" />
                      </SelectTrigger>
                      <SelectContent>
                        {departments.map((dept) => (
                          <SelectItem key={dept.departmentId} value={dept.departmentId}>
                            {dept.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="position">Должность</Label>
                    <Select
                      value={regPositionId}
                      onValueChange={setRegPositionId}
                      disabled={loadingData}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Выберите должность" />
                      </SelectTrigger>
                      <SelectContent>
                        {positions.map((pos) => (
                          <SelectItem key={pos.positionId} value={pos.positionId}>
                            {pos.title}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="hireDate">Дата приема на работу</Label>
                    <Input
                      id="hireDate"
                      type="date"
                      value={regHireDate}
                      onChange={(e) => setRegHireDate(e.target.value)}
                    />
                  </div>
                </CardContent>
                <CardFooter>
                  <Button className="w-full" type="submit" disabled={isLoading || loadingData}>
                    {isLoading ? "Регистрация..." : "Зарегистрироваться"}
                  </Button>
                </CardFooter>
              </form>
            </Card>
          </TabsContent>
        </Tabs>

        <p className="mt-8 text-center text-xs text-muted-foreground">
          &copy; 2026 Корпоративная платформа обучения.
        </p>
      </div>
    </div>
  );
}

import { Link, useLocation } from "wouter";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery } from "@tanstack/react-query";
import { fetchAssignmentRequests } from "@/lib/coursesApi";
import { 
  LayoutDashboard, 
  BookOpen, 
  CheckSquare, 
  Calendar, 
  MessageSquare, 
  FileText, 
  Settings,
  Building2,
  BarChart3,
  ClipboardCheck,
  PencilRuler,
} from "lucide-react";

export function Sidebar() {
  const [location] = useLocation();
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const canModerate = roles.some((role) => ["ADMIN", "HR"].includes(role));
  const canManageCourses = roles.some((role) => ["ADMIN", "TECHNOLOG"].includes(role));
  const canLeadCourses = roles.some((role) => ["ADMIN", "HR", "TECHNOLOG", "EXPERT"].includes(role));
  const canViewAnalytics = (user?.roles ?? []).some((role) =>
    ["ADMIN", "HR", "MANAGER", "TECHNOLOG"].includes(role),
  );

  const { data: pendingAssignmentRequests = [] } = useQuery({
    queryKey: ["assignment-requests-pending-count"],
    queryFn: () => fetchAssignmentRequests("PENDING"),
    enabled: canModerate,
    refetchInterval: 60000,
    refetchOnWindowFocus: true,
  });

  const pendingAssignmentCount = pendingAssignmentRequests.length;

  const renderBadge = (count?: number) => {
    if (!count) return null;
    return (
      <span className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1.5 text-[11px] font-semibold leading-none text-destructive-foreground">
        {count > 9 ? "9+" : count}
      </span>
    );
  };

  const menuItems = [
    { icon: LayoutDashboard, label: "Дашборд", href: "/" },
    { icon: BookOpen, label: "Назначенные курсы", href: "/catalog" },
    { icon: CheckSquare, label: "Мой выбор", href: "/selection" },
    { icon: Calendar, label: "Расписание", href: "/calendar" },
    { icon: MessageSquare, label: "Чаты", href: "/chat" },
    { icon: FileText, label: "Сертификаты", href: "/certificates" },
  ];

  const adminItems = [
    { icon: Settings, label: "Заявки и лимиты", href: "/admin", badge: pendingAssignmentCount },
  ];

  const courseAdminItems = [
    { icon: PencilRuler, label: "Управление курсами", href: "/admin/courses" },
  ];

  const expertItems = [
    { icon: ClipboardCheck, label: "Ведение курсов", href: "/teaching" },
  ];

  const analyticsItems = [
    { icon: BarChart3, label: "Аналитика", href: "/analytics" },
  ];

  return (
    <div className="flex h-full w-64 flex-col border-r bg-sidebar text-sidebar-foreground">
      <div className="flex h-16 items-center border-b px-4">
        <div className="flex min-w-0 items-center gap-2 font-bold text-lg text-primary">
          <Building2 className="h-5 w-5 shrink-0" />
          <span className="truncate">Обучение</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto py-4">
        <nav className="space-y-1 px-2">
          {menuItems.map((item) => (
            <Link key={item.href} href={item.href}>
              <a
                className={cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                  location === item.href
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-muted-foreground"
                )}
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </a>
            </Link>
          ))}

          {(canModerate || canManageCourses) && (
            <>
              <div className="my-4 border-t px-4 pt-4 text-xs font-semibold uppercase text-muted-foreground">
                Управление
              </div>
              {canModerate && adminItems.map((item) => (
                <Link key={item.href} href={item.href}>
                  <a
                    className={cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                      location === item.href
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-muted-foreground"
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    <span className="truncate">{item.label}</span>
                    {renderBadge(item.badge)}
                  </a>
                </Link>
              ))}
              {canManageCourses && courseAdminItems.map((item) => (
                <Link key={item.href} href={item.href}>
                  <a
                    className={cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                      location === item.href
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-muted-foreground"
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </a>
                </Link>
              ))}
            </>
          )}

          {canLeadCourses && (
            <>
              <div className="my-4 border-t px-4 pt-4 text-xs font-semibold uppercase text-muted-foreground">
                Сопровождение
              </div>
              {expertItems.map((item) => (
                <Link key={item.href} href={item.href}>
                  <a
                    className={cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                      location === item.href
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-muted-foreground"
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </a>
                </Link>
              ))}
            </>
          )}

          {canViewAnalytics && (
            <>
              <div className="my-4 border-t px-4 pt-4 text-xs font-semibold uppercase text-muted-foreground">
                Отчетность
              </div>
              {analyticsItems.map((item) => (
                <Link key={item.href} href={item.href}>
                  <a
                    className={cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                      location === item.href
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-muted-foreground"
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </a>
                </Link>
              ))}
            </>
          )}
        </nav>
      </div>
    </div>
  );
}

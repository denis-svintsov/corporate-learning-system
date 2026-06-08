import { Bell, Search, LogOut, User } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useAuth } from "@/contexts/AuthContext";
import { Link, useLocation } from "wouter";
import { useQuery } from "@tanstack/react-query";
import { fetchCourses } from "@/lib/coursesApi";
import { fetchUnreadNotificationsCount } from "@/lib/notificationsApi";
import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

function difficultyLabel(difficulty?: string | null) {
  if (difficulty === "BEGINNER") return "Начальный";
  if (difficulty === "INTERMEDIATE") return "Средний";
  if (difficulty === "ADVANCED") return "Продвинутый";
  return null;
}

export function Header() {
  const { user, logout } = useAuth();
  const [, setLocation] = useLocation();
  const [search, setSearch] = useState("");
  const [searchFocused, setSearchFocused] = useState(false);

  const searchTerm = search.trim();

  const { data: coursesResult, isFetching: isSearchFetching } = useQuery({
    queryKey: ["header-course-search", searchTerm],
    queryFn: () => fetchCourses({ q: searchTerm, status: "ACTIVE", size: 6 }),
    enabled: searchTerm.length >= 2,
  });

  const { data: unreadNotifications } = useQuery({
    queryKey: ["notifications-unread-count"],
    queryFn: fetchUnreadNotificationsCount,
    enabled: !!user?.id,
    refetchInterval: 60000,
    refetchOnWindowFocus: true,
  });

  const searchResults = coursesResult?.content ?? [];
  const showSearchResults = searchFocused && searchTerm.length >= 2;
  const unreadCount = unreadNotifications?.unreadCount ?? 0;

  const searchHint = useMemo(() => {
    if (searchTerm.length < 2) return "Введите минимум 2 символа";
    if (isSearchFetching) return "Ищем курсы...";
    if (searchResults.length === 0) return "Ничего не найдено";
    return null;
  }, [isSearchFetching, searchResults.length, searchTerm.length]);

  const getInitials = (username: string) => {
    return username
      .split(".")
      .map((part) => part[0]?.toUpperCase())
      .join("")
      .slice(0, 2);
  };

  const openCourse = (courseId: string) => {
    setSearch("");
    setSearchFocused(false);
    setLocation(`/course/${courseId}`);
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const firstCourse = searchResults[0];
    if (firstCourse?.id) {
      openCourse(firstCourse.id);
    }
  };

  return (
    <header className="flex h-16 items-center justify-between border-b bg-background px-6">
      <div className="flex w-96 items-center gap-4">
        <form className="relative w-full" onSubmit={submitSearch}>
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Поиск курсов, материалов..."
            className="w-full bg-muted pl-9"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            onFocus={() => setSearchFocused(true)}
            onBlur={() => window.setTimeout(() => setSearchFocused(false), 120)}
          />
          {showSearchResults && (
            <div className="absolute left-0 top-11 z-50 w-full overflow-hidden rounded-md border bg-background shadow-lg">
              {searchHint ? (
                <div className="px-3 py-2 text-sm text-muted-foreground">{searchHint}</div>
              ) : (
                <div className="max-h-80 overflow-y-auto py-1">
                  {searchResults.map((course) => (
                    <button
                      key={course.id}
                      type="button"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => openCourse(course.id)}
                      className="block w-full px-3 py-2 text-left transition-colors hover:bg-muted"
                    >
                      <div className="truncate text-sm font-medium">{course.title}</div>
                      <div className="truncate text-xs text-muted-foreground">
                        {course.description || difficultyLabel(course.difficulty) || "Курс"}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </form>
      </div>

      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" className="relative" asChild>
          <Link href="/notifications" aria-label="Уведомления">
            <Bell className="h-5 w-5 text-muted-foreground" />
            {unreadCount > 0 && (
              <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground">
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            )}
          </Link>
        </Button>
        
        <div className="flex items-center gap-3 border-l pl-4">
          <div className="hidden text-right sm:block">
            <p className="text-sm font-medium">{user?.username || "Пользователь"}</p>
            <p className="text-xs text-muted-foreground">{user?.email || ""}</p>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="relative h-10 w-10 rounded-full">
                <Avatar>
                  <AvatarFallback>{user ? getInitials(user.username) : "U"}</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href="/cabinet/profile">
                  <User className="mr-2 h-4 w-4" />
                  <span>Личный кабинет</span>
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={logout} className="cursor-pointer">
                <LogOut className="mr-2 h-4 w-4" />
                <span>Выйти</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  );
}

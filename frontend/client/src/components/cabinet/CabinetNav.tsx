import { Link, useLocation } from "wouter";
import { cn } from "@/lib/utils";

const items = [
  { href: "/cabinet/profile", label: "Профиль" },
  { href: "/cabinet/progress", label: "Текущий прогресс" },
  { href: "/cabinet/history", label: "История обучения" },
  { href: "/cabinet/settings", label: "Настройки" },
];

export function CabinetNav() {
  const [location] = useLocation();

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <Link key={item.href} href={item.href}>
          <a
            className={cn(
              "rounded-md border px-3 py-2 text-sm transition-colors",
              location === item.href
                ? "border-primary bg-primary text-primary-foreground"
                : "bg-background hover:bg-muted",
            )}
          >
            {item.label}
          </a>
        </Link>
      ))}
    </div>
  );
}

function mapTechnicalMessage(message: string): string {
  const lower = message.toLowerCase();
  if (lower.includes("access denied to course")) return "У вас нет доступа к этому курсу.";
  if (lower.includes("access denied")) return "У вас нет прав для этого действия.";
  if (lower.includes("hr/admin role required")) return "Недостаточно прав. Требуется роль HR или ADMIN.";
  if (lower.includes("you can submit requests only for yourself")) return "Нельзя отправлять заявки за другого пользователя.";
  if (lower.includes("request already reviewed")) return "Заявка уже обработана.";
  if (lower.includes("assignment request for this course is already pending")) return "Заявка на этот курс уже отправлена и ожидает модерации.";
  if (lower.includes("validation failed")) return "Проверьте корректность заполненных полей.";
  if (lower.includes("token is invalid or expired")) return "Сессия истекла. Войдите снова.";
  if (lower.includes("missing or invalid authorization header")) return "Требуется авторизация.";
  if (lower.includes("couldn't connect to server") || lower.includes("failed to fetch") || lower.includes("networkerror")) {
    return "Сервис временно недоступен. Проверьте подключение и повторите попытку.";
  }
  if (lower.includes("course not found")) return "Курс не найден.";
  if (lower.includes("request not found")) return "Заявка не найдена.";
  if (lower.includes("certificate not found")) return "Сертификат не найден.";
  if (lower.includes("missing x-user-id")) return "Требуется авторизация.";
  if (lower.includes("quarter limit reached")) {
    const limitMatch = message.match(/up to\s+(\d+)\s+courses/i);
    if (limitMatch?.[1]) {
      return `Лимит на квартал исчерпан. Можно выбрать не более ${limitMatch[1]} курсов.`;
    }
    return "Лимит на квартал исчерпан. Достигнуто максимальное количество курсов.";
  }
  return message;
}

function defaultMessageByStatus(status: number): string {
  if (status === 401) return "Требуется авторизация.";
  if (status === 403) return "У вас нет прав для этого действия.";
  if (status === 404) return "Запрошенные данные не найдены.";
  if (status === 409) return "Конфликт данных. Проверьте текущее состояние и повторите попытку.";
  if (status >= 500) return "Внутренняя ошибка сервиса. Попробуйте позже.";
  return "Не удалось выполнить запрос.";
}

export async function extractApiErrorMessage(res: Response, fallback?: string): Promise<string> {
  if (res.status === 401 && typeof window !== "undefined") {
    window.dispatchEvent(new Event("auth:unauthorized"));
  }

  let raw = "";
  try {
    raw = await res.text();
  } catch {
    return fallback ?? defaultMessageByStatus(res.status);
  }

  if (!raw) {
    return fallback ?? defaultMessageByStatus(res.status);
  }

  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const candidate =
      (typeof parsed.userMessage === "string" && parsed.userMessage) ||
      (typeof parsed.message === "string" && parsed.message) ||
      (typeof parsed.error === "string" && parsed.error) ||
      "";

    if (candidate) return mapTechnicalMessage(candidate);

    if (typeof parsed === "object") {
      const values = Object.values(parsed)
        .filter((v): v is string => typeof v === "string" && !!v.trim());
      if (values.length > 0) return mapTechnicalMessage(values.join(", "));
    }
  } catch {
    return mapTechnicalMessage(raw);
  }

  return fallback ?? defaultMessageByStatus(res.status);
}

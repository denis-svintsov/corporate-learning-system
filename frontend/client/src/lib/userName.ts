export interface NameParts {
  firstName?: string | null;
  middleName?: string | null;
  lastName?: string | null;
  email?: string | null;
  id?: string | null;
}

export function formatFullName(user: NameParts): string {
  const fullName = [user.lastName, user.firstName, user.middleName]
    .map((part) => part?.trim())
    .filter(Boolean)
    .join(" ");

  return fullName || user.email || user.id || "";
}

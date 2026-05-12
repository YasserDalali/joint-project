export function formatApiError(error: unknown): string {
  if (error == null) return "Request failed";
  if (typeof error === "string") return error;
  if (typeof error === "object" && error !== null) {
    const o = error as Record<string, unknown>;
    if (typeof o.code === "string" && typeof o.message === "string") {
      return `${o.code}: ${o.message}`;
    }
  }
  try {
    return JSON.stringify(error);
  } catch {
    return String(error);
  }
}

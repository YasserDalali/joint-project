/** Parses API date/time payloads (ISO strings, SQL-style strings, epoch ms, or Jackson numeric arrays). */
export function parseApiDateTime(raw: unknown): Date | null {
  if (raw == null) return null;
  if (typeof raw === "number" && Number.isFinite(raw)) return new Date(raw);
  if (typeof raw === "string") {
    const s = raw.trim();
    if (!s) return null;
    let d = new Date(s);
    if (!Number.isNaN(d.getTime())) return d;
    if (s.includes(" ") && !s.includes("T")) {
      d = new Date(s.replace(" ", "T"));
      if (!Number.isNaN(d.getTime())) return d;
    }
    return null;
  }
  if (Array.isArray(raw)) {
    const y = raw[0];
    const mo = raw[1];
    const day = raw[2];
    if (typeof y !== "number" || typeof mo !== "number" || typeof day !== "number") return null;
    const h = typeof raw[3] === "number" ? raw[3] : 0;
    const mi = typeof raw[4] === "number" ? raw[4] : 0;
    const sec = typeof raw[5] === "number" ? raw[5] : 0;
    const nano = typeof raw[6] === "number" ? raw[6] : 0;
    return new Date(y, mo - 1, day, h, mi, sec, Math.floor(nano / 1_000_000));
  }
  return null;
}

export function formatApiDateTime(
  raw: unknown,
  options?: Intl.DateTimeFormatOptions,
): string {
  const d = parseApiDateTime(raw);
  if (!d) return "—";
  return d.toLocaleString(undefined, options ?? { dateStyle: "medium", timeStyle: "short" });
}

export function formatApiDate(raw: unknown, options?: Intl.DateTimeFormatOptions): string {
  const d = parseApiDateTime(raw);
  if (!d) return "—";
  return d.toLocaleDateString(undefined, options ?? { dateStyle: "medium" });
}

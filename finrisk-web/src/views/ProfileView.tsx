import { useCallback, useEffect, useState } from "react";
import { apiClient } from "../api/client";
import { MaterialIcon } from "../components/MaterialIcon";
import { formatApiError } from "../util/formatApiError";
import { formatApiDate } from "../util/formatApiDateTime";
import type { components } from "../generated/api-schema";

type UserRow = components["schemas"]["UserResponse"];

function initials(fullName: string): string {
  const p = fullName.trim().split(/\s+/).filter(Boolean);
  if (p.length === 0) return "?";
  if (p.length === 1) return p[0]!.slice(0, 2).toUpperCase();
  return (p[0]![0]! + p[p.length - 1]![0]!).toUpperCase();
}

export function ProfileView({
  loading,
  setLoading,
  setError,
  isAdminUi,
}: {
  loading: boolean;
  setLoading: (v: boolean) => void;
  setError: (s: string | null) => void;
  isAdminUi: boolean;
}) {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(12);
  const [filter, setFilter] = useState("");
  const [applied, setApplied] = useState<string | undefined>(undefined);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    if (!isAdminUi) setShowCreate(false);
  }, [isAdminUi]);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/users", {
      params: { query: { page, size, ...(applied ? { email: applied } : {}) } },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (data) setUsers(data.content);
  }, [page, size, applied, setLoading, setError]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadUsers();
    });
  }, [loadUsers]);

  async function handleCreateUser(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    const { data, error: err, response } = await apiClient.POST("/api/v1/users", {
      body: { fullName: fullName.trim(), email: email.trim() },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (response.status === 201 && data) {
      setFullName("");
      setEmail("");
      setShowCreate(false);
      await loadUsers();
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-margin-mobile pb-28 pt-8 md:px-margin-desktop">
      <div className="mb-section-gap">
        <h1 className="mb-unit font-headline-lg text-headline-lg text-on-background">User management</h1>
        <p className="font-body-md text-on-surface-variant">Create users and browse the FinRisk directory.</p>
        {!isAdminUi ? (
          <p className="mt-2 rounded-lg border border-outline-variant bg-surface-container-low px-4 py-3 font-body-sm text-on-surface-variant">
            Admin-only actions (create users) are hidden. Turn on <strong>Admin</strong> in the header to enable them for this browser session.
          </p>
        ) : null}
      </div>

      <div className="mb-gutter flex flex-col items-center gap-gutter md:flex-row">
        <div className="relative w-full">
          <MaterialIcon
            name="search"
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline"
          />
          <input
            className="w-full rounded-lg border border-outline-variant bg-surface-container-lowest py-3 pl-12 pr-4 font-body-md outline-none transition-all focus:border-secondary focus:ring-0"
            placeholder="Filter by email prefix…"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                setPage(0);
                setApplied(filter.trim() || undefined);
              }
            }}
          />
        </div>
        <button
          type="button"
          className="flex w-full items-center justify-center gap-2 whitespace-nowrap rounded-lg bg-primary px-6 py-3 font-label-caps text-label-caps text-on-primary transition-opacity hover:opacity-90 md:w-auto"
          onClick={() => {
            setPage(0);
            setApplied(filter.trim() || undefined);
          }}
          disabled={loading}
        >
          <MaterialIcon name="filter_list" className="text-[20px] text-on-primary" />
          Apply filter
        </button>
      </div>

      {isAdminUi && showCreate ? (
        <form
          className="mb-gutter rounded-lg border border-outline-variant bg-surface-container-lowest p-card-padding vestox-shadow"
          onSubmit={handleCreateUser}
        >
          <h2 className="mb-4 font-headline-md text-headline-md text-on-surface">New user</h2>
          <div className="flex flex-col gap-4 md:flex-row md:flex-wrap md:items-end">
            <label className="flex flex-col gap-1 font-body-sm text-on-surface-variant">
              Full name
              <input
                required
                className="min-w-[12rem] rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2 font-body-md text-on-surface outline-none focus:border-secondary"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1 font-body-sm text-on-surface-variant">
              Email
              <input
                required
                type="email"
                className="min-w-[12rem] rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2 font-body-md text-on-surface outline-none focus:border-secondary"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-primary px-5 py-2 font-label-caps text-label-caps text-on-primary"
            >
              Create
            </button>
            <button
              type="button"
              className="rounded-lg border border-outline px-5 py-2 font-label-caps text-label-caps text-on-surface-variant"
              onClick={() => setShowCreate(false)}
            >
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      <div className="grid grid-cols-1 gap-gutter md:grid-cols-2 lg:grid-cols-3">
        {users.map((u) => (
          <div
            key={u.id}
            className="group cursor-pointer border border-outline-variant bg-surface-container-lowest p-card-padding transition-colors hover:border-secondary vestox-shadow"
          >
            <div className="mb-4 flex items-start justify-between">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-fixed font-headline-md font-bold text-primary">
                {initials(u.fullName)}
              </div>
              <span className="rounded-full bg-surface-variant px-3 py-1 font-label-caps text-[10px] text-on-surface-variant">
                USER
              </span>
            </div>
            <h3 className="mb-1 font-headline-md text-headline-md text-on-surface">{u.fullName}</h3>
            <p className="mb-6 font-body-sm text-on-surface-variant">{u.email}</p>
            <div className="flex items-center justify-between border-t border-outline-variant pt-4">
              <div className="flex flex-col">
                <span className="font-label-caps text-[10px] text-outline">JOINED</span>
                <span className="font-data-mono text-data-mono">{formatApiDate(u.createdAt)}</span>
              </div>
              <span className="font-data-mono text-on-surface-variant">#{u.id}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-gutter flex justify-center gap-2">
        <button
          type="button"
          disabled={loading || page === 0}
          className="rounded-lg border border-outline px-4 py-2 font-label-caps text-label-caps text-on-surface-variant disabled:opacity-40"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          Previous
        </button>
        <button
          type="button"
          disabled={loading || users.length < size}
          className="rounded-lg border border-outline px-4 py-2 font-label-caps text-label-caps text-on-surface-variant disabled:opacity-40"
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </button>
      </div>

      {isAdminUi ? (
        <button
          type="button"
          className="fixed bottom-24 right-6 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-on-primary shadow-lg transition-transform active:scale-95"
          onClick={() => setShowCreate(true)}
          aria-label="Add user"
        >
          <MaterialIcon name="add" style={{ fontVariationSettings: "'wght' 600" }} />
        </button>
      ) : null}
    </main>
  );
}

import { useCallback, useEffect, useState } from "react";
import { apiClient } from "./api/client";
import { AppShell, type AppTab } from "./components/AppShell";
import { DashboardView } from "./views/DashboardView";
import { AssetsView } from "./views/AssetsView";
import { TradingView } from "./views/TradingView";
import { ProfileView } from "./views/ProfileView";
import { formatApiError } from "./util/formatApiError";
import type { components } from "./generated/api-schema";

type AccountRow = components["schemas"]["AccountResponse"];

const ADMIN_UI_STORAGE_KEY = "finrisk-admin-ui";

function readAdminUiFromSession(): boolean {
  try {
    return sessionStorage.getItem(ADMIN_UI_STORAGE_KEY) === "1";
  } catch {
    return false;
  }
}

export default function App() {
  const [tab, setTab] = useState<AppTab>("dashboard");
  const [accountId, setAccountId] = useState("1");
  const [ownerUserId, setOwnerUserId] = useState("1");
  const [accounts, setAccounts] = useState<AccountRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isAdminUi, setIsAdminUi] = useState(readAdminUiFromSession);

  const persistAdminUi = useCallback((v: boolean) => {
    try {
      if (v) sessionStorage.setItem(ADMIN_UI_STORAGE_KEY, "1");
      else sessionStorage.removeItem(ADMIN_UI_STORAGE_KEY);
    } catch {
      /* ignore */
    }
    setIsAdminUi(v);
  }, []);

  const reloadAccounts = useCallback(async () => {
    const uid = Number(ownerUserId);
    if (!Number.isFinite(uid) || uid <= 0) {
      setAccounts([]);
      return;
    }
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/users/{userId}/accounts", {
      params: { path: { userId: uid }, query: { page: 0, size: 50 } },
    });
    if (err) {
      setError(formatApiError(err));
      setAccounts([]);
      return;
    }
    const list = data?.content ?? [];
    setAccounts(list);
    setAccountId((prev) => {
      if (list.length === 0) return prev;
      const cur = prev.trim();
      if (list.some((a) => String(a.id) === cur)) return prev;
      return String(list[0]!.id);
    });
  }, [ownerUserId]);

  useEffect(() => {
    queueMicrotask(() => {
      void reloadAccounts();
    });
  }, [reloadAccounts]);

  return (
    <AppShell active={tab} onTab={setTab} isAdminUi={isAdminUi} onAdminUiChange={persistAdminUi}>
      {error ? (
        <div
          className="mx-auto mb-2 max-w-7xl px-margin-mobile pt-2 md:px-margin-desktop"
          role="alert"
        >
          <div className="rounded-lg border border-error bg-error-container px-4 py-3 font-body-sm text-on-error-container">
            {error}
          </div>
        </div>
      ) : null}

      {tab === "dashboard" ? (
        <DashboardView
          accountId={accountId}
          setAccountId={setAccountId}
          ownerUserId={ownerUserId}
          setOwnerUserId={setOwnerUserId}
          accounts={accounts}
          reloadAccounts={reloadAccounts}
          loading={loading}
          setLoading={setLoading}
          setError={setError}
        />
      ) : null}
      {tab === "assets" ? (
        <AssetsView loading={loading} setLoading={setLoading} setError={setError} isAdminUi={isAdminUi} />
      ) : null}
      {tab === "trading" ? (
        <TradingView
          accountId={accountId}
          setAccountId={setAccountId}
          ownerUserId={ownerUserId}
          setOwnerUserId={setOwnerUserId}
          accounts={accounts}
          reloadAccounts={reloadAccounts}
          isAdminUi={isAdminUi}
          loading={loading}
          setLoading={setLoading}
          setError={setError}
        />
      ) : null}
      {tab === "profile" ? (
        <ProfileView loading={loading} setLoading={setLoading} setError={setError} isAdminUi={isAdminUi} />
      ) : null}

      {loading ? (
        <div className="pointer-events-none fixed bottom-24 left-1/2 z-40 -translate-x-1/2 rounded-full bg-primary px-4 py-2 font-label-caps text-label-caps text-on-primary opacity-90 shadow-lg">
          Loading…
        </div>
      ) : null}
    </AppShell>
  );
}

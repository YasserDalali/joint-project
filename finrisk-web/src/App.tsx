import { useState } from "react";
import { AppShell, type AppTab } from "./components/AppShell";
import { DashboardView } from "./views/DashboardView";
import { AssetsView } from "./views/AssetsView";
import { TradingView } from "./views/TradingView";
import { ProfileView } from "./views/ProfileView";

export default function App() {
  const [tab, setTab] = useState<AppTab>("dashboard");
  const [accountId, setAccountId] = useState("1");
  const [ownerUserId, setOwnerUserId] = useState("1");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  return (
    <AppShell active={tab} onTab={setTab}>
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
          loading={loading}
          setLoading={setLoading}
          setError={setError}
        />
      ) : null}
      {tab === "assets" ? (
        <AssetsView loading={loading} setLoading={setLoading} setError={setError} />
      ) : null}
      {tab === "trading" ? (
        <TradingView
          accountId={accountId}
          setAccountId={setAccountId}
          loading={loading}
          setLoading={setLoading}
          setError={setError}
        />
      ) : null}
      {tab === "profile" ? (
        <ProfileView loading={loading} setLoading={setLoading} setError={setError} />
      ) : null}

      {loading ? (
        <div className="pointer-events-none fixed bottom-24 left-1/2 z-40 -translate-x-1/2 rounded-full bg-primary px-4 py-2 font-label-caps text-label-caps text-on-primary opacity-90 shadow-lg">
          Loading…
        </div>
      ) : null}
    </AppShell>
  );
}

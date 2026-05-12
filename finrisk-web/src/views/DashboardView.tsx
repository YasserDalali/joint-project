import { useCallback, useEffect, useId, useState } from "react";
import { apiClient } from "../api/client";
import { MaterialIcon } from "../components/MaterialIcon";
import { RiskCalculationAnimator } from "../components/RiskCalculationAnimator";
import { formatApiError } from "../util/formatApiError";
import type { components } from "../generated/api-schema";

type PortfolioData = components["schemas"]["PortfolioResponse"];
type RiskData = components["schemas"]["RiskScoreResponse"];
type PlData = components["schemas"]["ProfitLossResponse"];
type AccountRow = components["schemas"]["AccountResponse"];
type RiskLevel = components["schemas"]["RiskLevel"];

function riskBadgeClass(level: RiskLevel): string {
  switch (level) {
    case "LOW":
      return "bg-secondary/10 text-secondary";
    case "MEDIUM":
      return "bg-yellow-500/10 text-yellow-700";
    case "HIGH":
      return "bg-orange-500/10 text-orange-700";
    case "VERY_HIGH":
      return "bg-error/10 text-error";
    default:
      return "bg-surface-variant text-on-surface-variant";
  }
}

function levelLabel(level: RiskLevel): string {
  return level.replaceAll("_", " ");
}

export function DashboardView({
  accountId,
  setAccountId,
  ownerUserId,
  setOwnerUserId,
  accounts,
  reloadAccounts,
  loading,
  setLoading,
  setError,
}: {
  accountId: string;
  setAccountId: (s: string) => void;
  ownerUserId: string;
  setOwnerUserId: (s: string) => void;
  accounts: AccountRow[];
  reloadAccounts: () => Promise<void>;
  loading: boolean;
  setLoading: (v: boolean) => void;
  setError: (s: string | null) => void;
}) {
  const gid = useId().replace(/:/g, "");
  const [portfolio, setPortfolio] = useState<PortfolioData | null>(null);
  const [risk, setRisk] = useState<RiskData | null>(null);
  const [pl, setPl] = useState<PlData | null>(null);
  const [depositAmount, setDepositAmount] = useState("1000");
  const [withdrawAmount, setWithdrawAmount] = useState("100");
  const [newAccountName, setNewAccountName] = useState("Main");
  const [newAccountDeposit, setNewAccountDeposit] = useState("0");
  const [calcDetailsOpen, setCalcDetailsOpen] = useState(false);
  const [calcAnimatorKey, setCalcAnimatorKey] = useState(0);

  const refreshSnapshots = useCallback(async () => {
    const id = Number(accountId);
    if (!Number.isFinite(id) || id <= 0) {
      setPortfolio(null);
      setRisk(null);
      setPl(null);
      return;
    }
    setLoading(true);
    setError(null);
    const [pRes, rRes, lRes] = await Promise.all([
      apiClient.GET("/api/v1/accounts/{accountId}/portfolio", { params: { path: { accountId: id } } }),
      apiClient.GET("/api/v1/accounts/{accountId}/risk", { params: { path: { accountId: id } } }),
      apiClient.GET("/api/v1/accounts/{accountId}/profit-loss", { params: { path: { accountId: id } } }),
    ]);
    setLoading(false);
    if (pRes.error) {
      setError(formatApiError(pRes.error));
      setPortfolio(null);
      setRisk(null);
      setPl(null);
      return;
    }
    setPortfolio(pRes.data ?? null);
    setRisk(rRes.error ? null : (rRes.data ?? null));
    setPl(lRes.error ? null : (lRes.data ?? null));
  }, [accountId, setLoading, setError]);

  useEffect(() => {
    queueMicrotask(() => {
      void refreshSnapshots();
    });
  }, [refreshSnapshots]);

  async function handleDeposit(e: React.FormEvent) {
    e.preventDefault();
    const id = Number(accountId);
    const amount = Number(depositAmount);
    if (!Number.isFinite(id) || id <= 0 || !Number.isFinite(amount) || amount <= 0) {
      setError("Valid account id and positive deposit amount required");
      return;
    }
    setLoading(true);
    setError(null);
    const { error: err } = await apiClient.POST("/api/v1/accounts/{id}/deposit", {
      params: { path: { id } },
      body: { amount },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    await refreshSnapshots();
    await reloadAccounts();
  }

  async function handleWithdraw(e: React.FormEvent) {
    e.preventDefault();
    const id = Number(accountId);
    const amount = Number(withdrawAmount);
    if (!Number.isFinite(id) || id <= 0 || !Number.isFinite(amount) || amount <= 0) {
      setError("Valid account id and positive withdraw amount required");
      return;
    }
    setLoading(true);
    setError(null);
    const { error: err } = await apiClient.POST("/api/v1/accounts/{id}/withdraw", {
      params: { path: { id } },
      body: { amount },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    await refreshSnapshots();
    await reloadAccounts();
  }

  async function handleOpenAccount(e: React.FormEvent) {
    e.preventDefault();
    const uid = Number(ownerUserId);
    const initialDeposit = Number(newAccountDeposit);
    if (!Number.isFinite(uid) || uid <= 0 || !newAccountName.trim()) {
      setError("Owner user id and account name are required");
      return;
    }
    if (!Number.isFinite(initialDeposit) || initialDeposit < 0) {
      setError("Initial deposit must be a non-negative number");
      return;
    }
    setLoading(true);
    setError(null);
    const { data, error: err, response } = await apiClient.POST("/api/v1/accounts", {
      body: { userId: uid, accountName: newAccountName.trim(), initialDeposit },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (response.status === 201 && data) {
      setAccountId(String(data.id));
      await reloadAccounts();
      await refreshSnapshots();
    }
  }

  const netInvested =
    pl?.holdings.reduce((s, h) => s + h.netInvested, 0) ?? null;
  const totalValue = portfolio?.totalAccountValue ?? null;
  const score = risk?.score ?? 0;
  const C = 2 * Math.PI * 80;
  const arcLen = (Math.min(100, Math.max(0, score)) / 100) * C;

  const plByAsset = new Map((pl?.holdings ?? []).map((h) => [h.assetId, h]));

  const holdingsTotalValue =
    portfolio?.holdings.reduce((s, h) => s + (Number.isFinite(h.currentValue) ? h.currentValue : 0), 0) ?? null;

  return (
    <main className="mx-auto max-w-7xl px-margin-mobile pb-28 pt-6 md:px-margin-desktop">
      <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="font-headline-lg text-headline-lg text-primary">Account overview</h1>
          <p className="font-body-sm text-on-surface-variant">Portfolio, P&amp;L, and volatility risk for the selected account.</p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <label className="flex flex-col gap-1 font-label-caps text-label-caps text-on-surface-variant">
            Owner user id
            <input
              className="w-24 rounded-lg border border-outline-variant bg-surface-container-low px-2 py-2 font-data-mono"
              value={ownerUserId}
              onChange={(e) => setOwnerUserId(e.target.value)}
              inputMode="numeric"
            />
          </label>
          <label className="flex min-w-[12rem] flex-col gap-1 font-label-caps text-label-caps text-on-surface-variant">
            Account
            <select
              className="rounded-lg border border-outline-variant bg-surface-container-low px-2 py-2 font-body-md text-on-surface"
              value={accounts.some((a) => String(a.id) === accountId.trim()) ? accountId.trim() : ""}
              onChange={(e) => setAccountId(e.target.value)}
              disabled={accounts.length === 0}
            >
              {accounts.length === 0 ? (
                <option value="">No accounts</option>
              ) : (
                accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    #{a.id} · {a.accountName}
                  </option>
                ))
              )}
            </select>
          </label>
          <button
            type="button"
            className="rounded-lg border border-secondary px-4 py-2 font-label-caps text-secondary"
            disabled={loading}
            onClick={() => {
              void reloadAccounts().then(() => refreshSnapshots());
            }}
          >
            Refresh
          </button>
        </div>
      </div>

      <div className="mb-section-gap grid grid-cols-1 gap-gutter md:grid-cols-3">
        <div className="flex flex-col justify-between border border-outline-variant bg-surface-container-lowest p-card-padding vestox-card-shadow md:col-span-2">
          <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
            <div>
              <span className="font-label-caps text-label-caps text-on-surface-variant">Total net worth</span>
              <h2 className="mt-1 font-display-lg text-display-lg text-primary">
                {totalValue != null ? `$${totalValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : "—"}
              </h2>
            </div>
            {pl && (
              <div
                className={`flex items-center gap-1 rounded-lg px-3 py-1 ${
                  pl.totalProfitLoss >= 0 ? "bg-secondary/10 text-secondary" : "bg-error/10 text-error"
                }`}
              >
                <MaterialIcon name={pl.totalProfitLoss >= 0 ? "trending_up" : "trending_down"} className="text-sm" filled />
                <span className="font-data-mono text-data-mono">
                  {pl.totalProfitLoss >= 0 ? "+" : ""}
                  {pl.totalProfitLoss.toFixed(2)} USD P&amp;L
                </span>
              </div>
            )}
          </div>
          <form className="mb-3 flex flex-wrap gap-2 border-t border-outline-variant pt-4" onSubmit={handleDeposit}>
            <input
              className="min-w-[6rem] flex-1 rounded-lg border border-outline-variant px-3 py-2 font-data-mono"
              value={depositAmount}
              onChange={(e) => setDepositAmount(e.target.value)}
              inputMode="decimal"
            />
            <button type="submit" disabled={loading} className="flex-1 rounded-lg border border-secondary py-3 font-label-caps text-secondary hover:bg-secondary/5">
              Deposit
            </button>
          </form>
          <form className="flex flex-wrap gap-2" onSubmit={handleWithdraw}>
            <input
              className="min-w-[6rem] flex-1 rounded-lg border border-outline-variant px-3 py-2 font-data-mono"
              value={withdrawAmount}
              onChange={(e) => setWithdrawAmount(e.target.value)}
              inputMode="decimal"
            />
            <button type="submit" disabled={loading} className="flex-1 rounded-lg border border-primary py-3 font-label-caps text-primary hover:bg-primary/5">
              Withdraw
            </button>
          </form>
        </div>

        <div className="flex flex-col items-center justify-center border border-outline-variant bg-primary-container p-card-padding text-center vestox-card-shadow">
          <span className="mb-4 font-label-caps text-label-caps text-on-primary-container">Risk profile</span>
          <div className="relative flex h-24 w-24 items-center justify-center rounded-full border-4 border-secondary">
            <span className="font-headline-lg text-headline-lg text-on-primary">{risk ? levelLabel(risk.level).slice(0, 3).toUpperCase() : "—"}</span>
          </div>
          <p className="mt-4 font-body-sm text-on-primary-container">{risk ? `${levelLabel(risk.level)} · ${risk.strategy}` : "Load data with a valid account id"}</p>
        </div>
      </div>

      <section className="mb-section-gap">
        <div className="mb-4 flex items-center justify-between px-2">
          <h3 className="font-headline-md text-headline-md text-primary">Investment accounts</h3>
          <span className="font-label-caps text-label-caps text-on-surface-variant">{accounts.length} listed</span>
        </div>
        <form onSubmit={handleOpenAccount} className="mb-gutter flex flex-wrap items-end gap-3 rounded-lg border border-outline-variant bg-surface-container-lowest p-card-padding">
          <span className="w-full font-label-caps text-label-caps text-on-surface-variant">Open account (owner = user id above)</span>
          <label className="flex flex-col gap-1 text-body-sm text-on-surface-variant">
            Name
            <input className="rounded border px-2 py-1" value={newAccountName} onChange={(e) => setNewAccountName(e.target.value)} required />
          </label>
          <label className="flex flex-col gap-1 text-body-sm text-on-surface-variant">
            Initial deposit
            <input className="rounded border px-2 py-1" value={newAccountDeposit} onChange={(e) => setNewAccountDeposit(e.target.value)} inputMode="decimal" />
          </label>
          <button type="submit" disabled={loading} className="rounded-lg bg-primary px-4 py-2 font-label-caps text-on-primary">
            Open account
          </button>
        </form>
        <div className="space-y-unit">
          {accounts.map((acc) => (
            <button
              key={acc.id}
              type="button"
              onClick={() => setAccountId(String(acc.id))}
              className={`flex w-full cursor-pointer items-center border p-card-padding text-left transition-colors hover:bg-surface-container-low ${
                String(acc.id) === accountId.trim()
                  ? "border-secondary bg-surface-container-low"
                  : "border-outline-variant bg-surface-container-lowest"
              }`}
            >
              <div className="mr-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary">
                <MaterialIcon name="account_balance" className="text-on-primary" />
              </div>
              <div className="flex-1">
                <h4 className="font-body-lg font-semibold text-primary">{acc.accountName}</h4>
                <p className="font-body-sm text-on-surface-variant">Account #{acc.id}</p>
              </div>
              <div className="text-right">
                <p className="font-data-mono text-headline-md text-primary">
                  ${acc.cashBalance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </p>
                <p className="font-data-mono text-body-sm text-secondary">Cash balance</p>
              </div>
              <MaterialIcon name="chevron_right" className="ml-4 text-on-surface-variant opacity-60" />
            </button>
          ))}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-gutter md:grid-cols-12">
        <section className="flex flex-col justify-between rounded-lg border border-outline-variant bg-surface-container-lowest p-card-padding md:col-span-8">
          <div>
            <h2 className="mb-unit font-label-caps text-label-caps text-on-surface-variant">Total portfolio value</h2>
            <div className="flex flex-wrap items-baseline gap-3">
              <span className="font-display-lg text-display-lg font-data-mono">
                {portfolio ? `$${portfolio.totalAccountValue.toLocaleString(undefined, { maximumFractionDigits: 2 })}` : "—"}
              </span>
              {pl ? (
                <div
                  className={`flex items-center rounded px-2 py-1 ${
                    pl.totalProfitLoss >= 0 ? "bg-secondary/10 text-secondary" : "bg-error/10 text-error"
                  }`}
                >
                  <MaterialIcon name={pl.totalProfitLoss >= 0 ? "trending_up" : "trending_down"} className="text-sm" filled />
                  <span className="ml-1 font-data-mono text-sm font-bold">
                    {pl.totalProfitLoss >= 0 ? "+" : ""}
                    {pl.totalProfitLoss.toFixed(2)} P&amp;L
                  </span>
                </div>
              ) : null}
            </div>
          </div>
          <div className="mt-8 grid grid-cols-2 gap-4 border-t border-outline-variant pt-6">
            <div>
              <p className="font-label-caps text-label-caps text-on-surface-variant">Net invested</p>
              <p className="font-data-mono text-body-lg font-bold text-on-surface">
                {netInvested != null
                  ? `$${netInvested.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                  : "—"}
              </p>
            </div>
            <div>
              <p className="mb-1 font-label-caps text-label-caps text-on-surface-variant">Unrealized P&amp;L</p>
              {pl ? (
                <div
                  className={`inline-flex items-center rounded-lg border px-3 py-1 ${
                    pl.totalProfitLoss >= 0 ? "border-secondary/20 bg-secondary/10" : "border-error/20 bg-error/10"
                  }`}
                >
                  <p className={`font-data-mono text-body-lg font-bold ${pl.totalProfitLoss >= 0 ? "text-secondary" : "text-error"}`}>
                    {pl.totalProfitLoss >= 0 ? "+" : ""}
                    {pl.totalProfitLoss.toFixed(2)}
                  </p>
                </div>
              ) : (
                <p className="font-data-mono text-body-lg">—</p>
              )}
            </div>
          </div>
        </section>

        <section className="flex flex-col items-center rounded-lg border border-outline-variant bg-surface-container-lowest p-card-padding md:col-span-4">
          <h2 className="mb-6 self-start font-label-caps text-label-caps text-on-surface-variant">Volatility risk score</h2>
          <div className="relative flex h-48 w-48 items-center justify-center">
            <svg className="absolute h-full w-full -rotate-90" viewBox="0 0 192 192" aria-hidden>
              <defs>
                <linearGradient id={`rg-${gid}`} x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#10B981" />
                  <stop offset="50%" stopColor="#F59E0B" />
                  <stop offset="100%" stopColor="#EF4444" />
                </linearGradient>
              </defs>
              <circle cx="96" cy="96" r="80" fill="none" stroke="#F3F4F6" strokeWidth="12" strokeLinecap="round" />
              <circle
                cx="96"
                cy="96"
                r="80"
                fill="none"
                stroke={`url(#rg-${gid})`}
                strokeWidth="12"
                strokeLinecap="round"
                strokeDasharray={`${arcLen} ${C}`}
              />
            </svg>
            <div className="flex flex-col items-center">
              <span className="font-display-lg text-6xl font-extrabold leading-none text-on-surface">{risk ? Math.round(risk.score) : "—"}</span>
              <span className="mt-1 font-label-caps text-on-surface-variant opacity-60">of 100</span>
            </div>
          </div>
          <div className="mt-6 text-center">
            {risk ? (
              <>
                <div className={`mb-2 inline-flex items-center rounded-full border px-4 py-1 ${riskBadgeClass(risk.level)}`}>
                  <p className="font-headline-md font-extrabold tracking-widest">{levelLabel(risk.level)}</p>
                </div>
                <p className="mx-auto max-w-[200px] text-body-sm leading-relaxed text-on-surface-variant">
                  Weighted volatility score for this account ({risk.strategy}).
                </p>
              </>
            ) : (
              <p className="text-on-surface-variant">No risk data</p>
            )}
          </div>
        </section>

        <section className="overflow-hidden rounded-lg border border-outline-variant bg-surface-container-lowest md:col-span-12">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-outline-variant p-card-padding">
            <h2 className="font-label-caps text-label-caps text-on-surface-variant">Asset weight &amp; risk</h2>
            <button
              type="button"
              disabled={!risk}
              aria-expanded={calcDetailsOpen}
              aria-controls={`dashboard-calc-details-${gid}`}
              className="inline-flex items-center gap-2 rounded-lg border border-primary px-4 py-2 font-label-caps text-primary transition-colors hover:bg-primary/5 disabled:cursor-not-allowed disabled:border-outline-variant disabled:text-on-surface-variant disabled:hover:bg-transparent"
              onClick={() => {
                setCalcDetailsOpen((open) => {
                  const next = !open;
                  if (next) setCalcAnimatorKey((k) => k + 1);
                  return next;
                });
              }}
            >
              <MaterialIcon name={calcDetailsOpen ? "expand_less" : "calculate"} className="text-base" />
              {calcDetailsOpen ? "Hide calculation details" : "Preview calculations"}
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="bg-surface-container-low">
                  <th className="border-b border-outline-variant p-4 font-label-caps text-label-caps text-on-surface-variant">Asset</th>
                  <th className="border-b border-outline-variant p-4 text-right font-label-caps text-label-caps text-on-surface-variant">Weight</th>
                  <th className="border-b border-outline-variant p-4 text-right font-label-caps text-label-caps text-on-surface-variant">Value</th>
                  <th className="border-b border-outline-variant p-4 text-center font-label-caps text-label-caps text-on-surface-variant">Risk</th>
                  <th className="border-b border-outline-variant p-4 text-right font-label-caps text-label-caps text-on-surface-variant">P&amp;L %</th>
                </tr>
              </thead>
              <tbody>
                {(risk?.breakdown ?? []).map((row) => {
                  const holding = portfolio?.holdings.find((h) => h.assetId === row.assetId);
                  const plh = plByAsset.get(row.assetId);
                  const pct = plh?.profitLossPercent;
                  return (
                    <tr key={row.assetId} className="transition-colors hover:bg-surface-container-low">
                      <td className="flex items-center gap-3 p-4">
                        <div className="flex h-8 w-8 items-center justify-center rounded bg-primary-container font-bold text-on-primary">
                          {row.symbol.slice(0, 1)}
                        </div>
                        <div>
                          <p className="font-bold text-on-surface">{row.symbol}</p>
                          <p className="text-xs text-on-surface-variant">#{row.assetId}</p>
                        </div>
                      </td>
                      <td className="p-4 text-right font-data-mono">{(row.weight * 100).toFixed(1)}%</td>
                      <td className="p-4 text-right font-data-mono">
                        {holding ? `$${holding.currentValue.toFixed(2)}` : "—"}
                      </td>
                      <td className="p-4 text-center">
                        <span className={`inline-block rounded px-3 py-1 text-xs font-bold uppercase tracking-tighter ${riskBadgeClass(row.riskLevel)}`}>
                          {levelLabel(row.riskLevel)}
                        </span>
                      </td>
                      <td className="p-4 text-right font-data-mono">
                        {pct != null && Number.isFinite(pct) ? (
                          <span className={pct >= 0 ? "text-secondary" : "text-error"}>
                            {pct >= 0 ? "+" : ""}
                            {pct.toFixed(2)}%
                          </span>
                        ) : (
                          "—"
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {risk && risk.breakdown.length === 0 ? (
              <p className="p-6 text-center text-on-surface-variant">No holdings to analyze.</p>
            ) : null}
          </div>
          {calcDetailsOpen && risk ? (
            <div
              id={`dashboard-calc-details-${gid}`}
              className="motion-safe:animate-dashboard-calc-reveal border-t border-outline-variant bg-surface-container-low"
            >
              <div className="p-card-padding">
                <RiskCalculationAnimator key={calcAnimatorKey} risk={risk} holdingsTotalValue={holdingsTotalValue} />
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </main>
  );
}

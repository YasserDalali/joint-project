import { useCallback, useEffect, useMemo, useState } from "react";
import { apiClient } from "../api/client";
import { MaterialIcon } from "../components/MaterialIcon";
import { formatApiError } from "../util/formatApiError";
import { formatApiDateTime } from "../util/formatApiDateTime";
import type { components } from "../generated/api-schema";

type AssetRow = components["schemas"]["AssetResponse"];
type TxRow = components["schemas"]["TransactionResponse"];
type AccountRow = components["schemas"]["AccountResponse"];

export function TradingView({
  accountId,
  setAccountId,
  ownerUserId,
  setOwnerUserId,
  accounts,
  reloadAccounts,
  isAdminUi,
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
  isAdminUi: boolean;
  loading: boolean;
  setLoading: (v: boolean) => void;
  setError: (s: string | null) => void;
}) {
  const [side, setSide] = useState<"BUY" | "SELL">("BUY");
  const [assets, setAssets] = useState<AssetRow[]>([]);
  const [tradeAssetId, setTradeAssetId] = useState("");
  const [tradeQty, setTradeQty] = useState("1");
  const [tradeUnitPrice, setTradeUnitPrice] = useState("");
  const [cash, setCash] = useState<number | null>(null);
  const [ledger, setLedger] = useState<TxRow[]>([]);

  const loadCash = useCallback(async () => {
    const id = Number(accountId);
    if (!Number.isFinite(id) || id <= 0) return;
    const { data, error: err } = await apiClient.GET("/api/v1/accounts/{accountId}/portfolio", {
      params: { path: { accountId: id } },
    });
    if (!err && data) setCash(data.cashBalance);
    else setCash(null);
  }, [accountId]);

  const loadLedger = useCallback(async () => {
    const id = Number(accountId);
    if (!Number.isFinite(id) || id <= 0) {
      setLedger([]);
      return;
    }
    const { data, error: err } = await apiClient.GET("/api/v1/accounts/{accountId}/transactions", {
      params: { path: { accountId: id }, query: { page: 0, size: 8, sort: ["transactionDate,desc"] } },
    });
    if (!err && data) setLedger(data.content);
    else setLedger([]);
  }, [accountId]);

  useEffect(() => {
    void (async () => {
      const { data, error: err } = await apiClient.GET("/api/v1/assets", {
        params: { query: { page: 0, size: 100 } },
      });
      if (err || !data) return;
      setAssets(data.content);
      if (data.content.length) {
        setTradeAssetId(String(data.content[0]!.id));
        setTradeUnitPrice(String(data.content[0]!.currentPrice));
      }
    })();
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      void loadCash();
      void loadLedger();
    });
  }, [loadCash, loadLedger]);

  const estimated = useMemo(() => {
    const q = Number(tradeQty);
    const p = Number(tradeUnitPrice);
    if (!Number.isFinite(q) || !Number.isFinite(p)) return 0;
    return q * p;
  }, [tradeQty, tradeUnitPrice]);

  function onPickAsset(id: string) {
    setTradeAssetId(id);
    const a = assets.find((x) => String(x.id) === id);
    if (a) setTradeUnitPrice(String(a.currentPrice));
  }

  async function executeTrade() {
    const acc = Number(accountId);
    const assetId = Number(tradeAssetId);
    const quantity = Number(tradeQty);
    const unitPrice = Number(tradeUnitPrice);
    if (
      !Number.isFinite(acc) ||
      acc <= 0 ||
      !Number.isFinite(assetId) ||
      assetId <= 0 ||
      !Number.isFinite(quantity) ||
      quantity < 1 ||
      !Number.isFinite(unitPrice) ||
      unitPrice < 0.0001
    ) {
      setError("Select account, asset, quantity ≥ 1, and unit price ≥ 0.0001");
      return;
    }
    setLoading(true);
    setError(null);
    const path = side === "BUY" ? "/api/v1/transactions/buy" : "/api/v1/transactions/sell";
    const { error: err } = await apiClient.POST(path, {
      body: { accountId: acc, assetId, quantity, unitPrice },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    await loadCash();
    await loadLedger();
  }

  return (
    <main className="mx-auto max-w-7xl px-margin-mobile pb-28 pt-6 md:px-margin-desktop">
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <label className="flex flex-col gap-1 font-label-caps text-label-caps text-on-surface-variant">
          Owner user id
          <input
            className="w-24 rounded border border-outline-variant bg-surface-container-low px-3 py-2 font-data-mono outline-none focus:border-secondary"
            value={ownerUserId}
            onChange={(e) => setOwnerUserId(e.target.value)}
            inputMode="numeric"
          />
        </label>
        <label className="flex min-w-[12rem] flex-col gap-1 font-label-caps text-label-caps text-on-surface-variant">
          Account
          <select
            className="w-full rounded border border-outline-variant bg-surface-container-low px-3 py-2 font-body-md outline-none focus:border-secondary"
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
          className="rounded-lg border border-outline px-3 py-2 font-label-caps text-label-caps text-secondary"
          onClick={() => {
            void reloadAccounts().then(() => {
              void loadCash();
              void loadLedger();
            });
          }}
          disabled={loading}
        >
          Refresh
        </button>
      </div>

      <div className="grid grid-cols-1 gap-gutter lg:grid-cols-12">
        <section className="flex flex-col gap-gutter lg:col-span-5">
          <div className="rounded border border-outline-variant bg-surface-container-lowest p-card-padding shadow-sm">
            <div className="mb-6 flex gap-4">
              <button
                type="button"
                className={`flex-1 rounded-lg py-3 font-label-caps text-label-caps transition-transform active:scale-95 ${
                  side === "BUY" ? "bg-primary text-on-primary" : "border border-outline text-on-surface-variant"
                }`}
                onClick={() => setSide("BUY")}
              >
                Buy
              </button>
              <button
                type="button"
                className={`flex-1 rounded-lg py-3 font-label-caps text-label-caps transition-transform active:scale-95 ${
                  side === "SELL" ? "bg-primary text-on-primary" : "border border-outline text-on-surface-variant"
                }`}
                onClick={() => setSide("SELL")}
              >
                Sell
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="mb-2 block font-label-caps text-label-caps text-on-surface-variant">Asset</label>
                <div className="relative">
                  <select
                    className="w-full appearance-none rounded border border-outline-variant bg-surface-container-low px-4 py-3 pr-10 font-body-md outline-none focus:border-secondary"
                    value={tradeAssetId}
                    onChange={(e) => onPickAsset(e.target.value)}
                  >
                    {assets.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.symbol} — {a.name}
                      </option>
                    ))}
                  </select>
                  <MaterialIcon
                    name="expand_more"
                    className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="mb-2 block font-label-caps text-label-caps text-on-surface-variant">Quantity</label>
                  <input
                    className="w-full rounded border border-outline-variant bg-surface-container-low px-4 py-3 font-data-mono outline-none focus:border-secondary"
                    value={tradeQty}
                    onChange={(e) => setTradeQty(e.target.value)}
                    inputMode="numeric"
                  />
                </div>
                <div>
                  <label className="mb-2 block font-label-caps text-label-caps text-on-surface-variant">
                    Unit price (USD){!isAdminUi ? " · market" : ""}
                  </label>
                  <input
                    className={`w-full rounded border border-outline-variant px-4 py-3 font-data-mono outline-none focus:border-secondary ${
                      isAdminUi
                        ? "bg-surface-container-low"
                        : "cursor-not-allowed bg-surface-container-high text-on-surface"
                    }`}
                    value={tradeUnitPrice}
                    onChange={(e) => isAdminUi && setTradeUnitPrice(e.target.value)}
                    readOnly={!isAdminUi}
                    inputMode="decimal"
                    title={isAdminUi ? "Override unit price (admin)" : "Uses list price for the selected asset"}
                  />
                </div>
              </div>
              <div className="border-t border-outline-variant pt-4">
                <div className="mb-2 flex items-center justify-between">
                  <span className="font-body-sm text-body-sm text-on-surface-variant">Available cash</span>
                  <span className="font-data-mono text-data-mono">
                    {cash != null ? `$${cash.toFixed(2)}` : "—"}
                  </span>
                </div>
                <div className="mb-4 flex items-center justify-between">
                  <span className="font-body-sm text-body-sm text-on-surface-variant">Estimated total</span>
                  <span className="font-headline-md text-headline-md font-bold text-primary">
                    ${estimated.toFixed(2)}
                  </span>
                </div>
              </div>
              <button
                type="button"
                className="w-full rounded-lg bg-primary py-4 font-label-caps text-label-caps text-on-primary transition-opacity hover:opacity-90 active:scale-[0.99]"
                disabled={loading}
                onClick={() => void executeTrade()}
              >
                Execute trade
              </button>
            </div>
          </div>
          <div className="rounded border border-outline-variant bg-surface-container-lowest p-card-padding shadow-sm">
            <h3 className="mb-4 font-label-caps text-label-caps text-on-surface-variant">Market state</h3>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-secondary" />
                <span className="font-body-sm font-semibold text-on-surface">Demo session</span>
              </div>
              <span className="font-data-mono text-body-sm text-on-surface-variant">FinRisk API</span>
            </div>
          </div>
        </section>

        <section className="lg:col-span-7">
          <div className="flex h-full flex-col rounded border border-outline-variant bg-surface-container-lowest shadow-sm">
            <div className="flex items-center justify-between border-b border-outline-variant p-card-padding">
              <h2 className="font-headline-md text-headline-md font-bold text-on-surface">Transaction ledger</h2>
              <button type="button" className="rounded p-2 hover:bg-surface-container-low" onClick={() => void loadLedger()}>
                <MaterialIcon name="filter_list" />
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-surface-container-low">
                    <th className="border-b border-outline-variant p-4 text-left font-label-caps text-label-caps text-on-surface-variant">
                      Date
                    </th>
                    <th className="border-b border-outline-variant p-4 text-left font-label-caps text-label-caps text-on-surface-variant">
                      Type
                    </th>
                    <th className="border-b border-outline-variant p-4 text-left font-label-caps text-label-caps text-on-surface-variant">
                      Asset
                    </th>
                    <th className="border-b border-outline-variant p-4 text-right font-label-caps text-label-caps text-on-surface-variant">
                      Amount
                    </th>
                  </tr>
                </thead>
                <tbody className="font-body-sm">
                  {ledger.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="p-6 text-center text-on-surface-variant">
                        No transactions for this account.
                      </td>
                    </tr>
                  ) : (
                    ledger.map((t) => (
                      <tr key={t.id} className="transition-colors hover:bg-surface-container-low">
                        <td className="border-b border-outline-variant p-4 text-on-surface-variant">
                          {formatApiDateTime(t.transactionDate)}
                        </td>
                        <td
                          className={`border-b border-outline-variant p-4 font-bold ${
                            t.transactionType === "BUY" ? "text-secondary" : "text-error"
                          }`}
                        >
                          {t.transactionType}
                        </td>
                        <td className="border-b border-outline-variant p-4 font-semibold">
                          {t.assetSymbol ?? `#${t.assetId}`}
                        </td>
                        <td className="border-b border-outline-variant p-4 text-right font-data-mono">
                          ${t.totalAmount.toFixed(2)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}

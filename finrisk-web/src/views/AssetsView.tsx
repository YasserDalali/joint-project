import { useCallback, useEffect, useState } from "react";
import { apiClient } from "../api/client";
import { MaterialIcon } from "../components/MaterialIcon";
import { formatApiError } from "../util/formatApiError";
import type { components } from "../generated/api-schema";

type AssetRow = components["schemas"]["AssetResponse"];
type AssetType = components["schemas"]["AssetType"];
type AssetCreateRequest = components["schemas"]["AssetCreateRequest"];

const PAGE_SIZES = [10, 25, 50] as const;

function assetMeta(a: AssetRow): string {
  switch (a.assetType) {
    case "STOCK":
      return a.exchange ?? "STOCK";
    case "ETF":
      return "ETF";
    case "BOND":
      return "BOND";
    case "CRYPTO":
      return a.blockchain ?? "CRYPTO";
  }
}

function buildCreateAssetBody(
  newAssetType: AssetType,
  newSymbol: string,
  newName: string,
  newPrice: string,
  stockSector: string,
  stockExchange: string,
  etfIssuer: string,
  etfExpense: string,
  bondRate: string,
  bondMaturity: string,
  bondIssuer: string,
  cryptoChain: string,
  setError: (s: string | null) => void,
): AssetCreateRequest | null {
  const symbol = newSymbol.trim().toUpperCase();
  const name = newName.trim();
  const currentPrice = Number(newPrice);
  if (!symbol || !name || !Number.isFinite(currentPrice) || currentPrice < 0.0001) {
    setError("Asset symbol, name, and a valid price (≥ 0.0001) are required");
    return null;
  }
  const base = { symbol, name, assetType: newAssetType, currentPrice };
  switch (newAssetType) {
    case "STOCK":
      return { ...base, assetType: "STOCK", sector: stockSector.trim(), exchange: stockExchange.trim() };
    case "ETF": {
      const issuer = etfIssuer.trim();
      if (!issuer) {
        setError("ETF issuer is required");
        return null;
      }
      const er = etfExpense.trim();
      if (er === "") {
        return { ...base, assetType: "ETF", issuer };
      }
      const expenseRatio = Number(er);
      if (!Number.isFinite(expenseRatio) || expenseRatio < 0) {
        setError("Expense ratio must be a non-negative number");
        return null;
      }
      return { ...base, assetType: "ETF", issuer, expenseRatio };
    }
    case "BOND": {
      const interestRate = Number(bondRate);
      if (!Number.isFinite(interestRate) || interestRate < 0 || interestRate > 100) {
        setError("Bond interest rate must be between 0 and 100");
        return null;
      }
      const issuer = bondIssuer.trim();
      if (!issuer) {
        setError("Bond issuer is required");
        return null;
      }
      return {
        ...base,
        assetType: "BOND",
        interestRate,
        maturityDate: bondMaturity,
        issuer,
      };
    }
    case "CRYPTO": {
      const blockchain = cryptoChain.trim();
      if (!blockchain) {
        setError("Blockchain is required for crypto assets");
        return null;
      }
      return { ...base, assetType: "CRYPTO", blockchain };
    }
    default:
      return null;
  }
}

type Segment = "ALL" | AssetType;

export function AssetsView({
  loading,
  setLoading,
  setError,
}: {
  loading: boolean;
  setLoading: (v: boolean) => void;
  setError: (s: string | null) => void;
}) {
  const [assets, setAssets] = useState<AssetRow[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [searchInput, setSearchInput] = useState("");
  const [searchApplied, setSearchApplied] = useState<string | undefined>(undefined);
  const [segment, setSegment] = useState<Segment>("ALL");
  const [priceDrafts, setPriceDrafts] = useState<Record<number, string>>({});
  const [showRegister, setShowRegister] = useState(false);

  const [newAssetType, setNewAssetType] = useState<AssetType>("STOCK");
  const [newSymbol, setNewSymbol] = useState("");
  const [newName, setNewName] = useState("");
  const [newPrice, setNewPrice] = useState("10");
  const [stockSector, setStockSector] = useState("Technology");
  const [stockExchange, setStockExchange] = useState("NASDAQ");
  const [etfIssuer, setEtfIssuer] = useState("");
  const [etfExpense, setEtfExpense] = useState("");
  const [bondRate, setBondRate] = useState("4");
  const [bondMaturity, setBondMaturity] = useState("2030-01-01");
  const [bondIssuer, setBondIssuer] = useState("");
  const [cryptoChain, setCryptoChain] = useState("Ethereum");

  const typeFilter: AssetType | undefined = segment === "ALL" ? undefined : segment;

  const loadAssets = useCallback(async () => {
    setLoading(true);
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/assets", {
      params: {
        query: {
          page,
          size,
          ...(typeFilter ? { type: typeFilter } : {}),
          ...(searchApplied ? { search: searchApplied } : {}),
        },
      },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (data) setAssets(data.content);
  }, [page, size, typeFilter, searchApplied, setLoading, setError]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadAssets();
    });
  }, [loadAssets]);

  async function handleCreateAsset(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const body = buildCreateAssetBody(
      newAssetType,
      newSymbol,
      newName,
      newPrice,
      stockSector,
      stockExchange,
      etfIssuer,
      etfExpense,
      bondRate,
      bondMaturity,
      bondIssuer,
      cryptoChain,
      setError,
    );
    if (!body) return;
    setLoading(true);
    const { error: err, response } = await apiClient.POST("/api/v1/assets", { body });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (response.status === 201) {
      setNewSymbol("");
      setNewName("");
      setShowRegister(false);
      await loadAssets();
    }
  }

  async function handleUpdatePrice(assetId: number) {
    const raw = priceDrafts[assetId]?.trim() ?? "";
    const price = Number(raw);
    if (!Number.isFinite(price) || price < 0.0001) {
      setError("Price must be a number ≥ 0.0001");
      return;
    }
    setLoading(true);
    setError(null);
    const { error: err } = await apiClient.PUT("/api/v1/assets/{id}/price", {
      params: { path: { id: assetId } },
      body: { price },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    setPriceDrafts((d) => {
      const next = { ...d };
      delete next[assetId];
      return next;
    });
    await loadAssets();
  }

  const segments: { id: Segment; label: string }[] = [
    { id: "ALL", label: "All" },
    { id: "STOCK", label: "Stocks" },
    { id: "ETF", label: "ETFs" },
    { id: "CRYPTO", label: "Crypto" },
    { id: "BOND", label: "Bonds" },
  ];

  return (
    <main className="mx-auto max-w-7xl px-margin-mobile pb-32 pt-6 md:px-margin-desktop">
      <section className="mb-section-gap">
        <div className="flex flex-col gap-6">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="mb-2 font-headline-lg text-headline-lg text-on-background">Explore assets</h2>
              <p className="font-body-md text-on-surface-variant">Registered instruments and live list prices.</p>
            </div>
            <button
              type="button"
              onClick={() => setShowRegister((s) => !s)}
              className="rounded-lg bg-primary px-5 py-2 font-label-caps text-label-caps text-on-primary"
            >
              {showRegister ? "Close register" : "Register asset"}
            </button>
          </div>

          {showRegister ? (
            <form
              onSubmit={handleCreateAsset}
              className="rounded-lg border border-outline-variant bg-surface-container-lowest p-card-padding vestox-shadow"
            >
              <h3 className="mb-4 font-headline-md text-headline-md text-on-surface">New asset</h3>
              <div className="mb-4 flex flex-wrap gap-3">
                <label className="flex flex-col gap-1 text-body-sm text-on-surface-variant">
                  Type
                  <select
                    className="rounded-lg border border-outline-variant bg-surface-container-low px-3 py-2"
                    value={newAssetType}
                    onChange={(e) => setNewAssetType(e.target.value as AssetType)}
                  >
                    <option value="STOCK">STOCK</option>
                    <option value="ETF">ETF</option>
                    <option value="BOND">BOND</option>
                    <option value="CRYPTO">CRYPTO</option>
                  </select>
                </label>
                <label className="flex flex-col gap-1 text-body-sm text-on-surface-variant">
                  Symbol
                  <input
                    required
                    maxLength={20}
                    className="rounded-lg border border-outline-variant px-3 py-2"
                    value={newSymbol}
                    onChange={(e) => setNewSymbol(e.target.value)}
                  />
                </label>
                <label className="flex flex-col gap-1 text-body-sm text-on-surface-variant">
                  Name
                  <input
                    required
                    maxLength={150}
                    className="min-w-[10rem] rounded-lg border border-outline-variant px-3 py-2"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                  />
                </label>
                <label className="flex flex-col gap-1 text-body-sm text-on-surface-variant">
                  Price (USD)
                  <input
                    required
                    className="rounded-lg border border-outline-variant px-3 py-2"
                    value={newPrice}
                    onChange={(e) => setNewPrice(e.target.value)}
                    inputMode="decimal"
                  />
                </label>
              </div>
              {newAssetType === "STOCK" ? (
                <div className="mb-4 flex flex-wrap gap-3">
                  <label className="flex flex-col gap-1 text-body-sm">
                    Sector
                    <input
                      required
                      className="rounded-lg border px-3 py-2"
                      value={stockSector}
                      onChange={(e) => setStockSector(e.target.value)}
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-body-sm">
                    Exchange
                    <input
                      required
                      className="rounded-lg border px-3 py-2"
                      value={stockExchange}
                      onChange={(e) => setStockExchange(e.target.value)}
                    />
                  </label>
                </div>
              ) : null}
              {newAssetType === "ETF" ? (
                <div className="mb-4 flex flex-wrap gap-3">
                  <label className="flex flex-col gap-1 text-body-sm">
                    Issuer
                    <input required className="rounded-lg border px-3 py-2" value={etfIssuer} onChange={(e) => setEtfIssuer(e.target.value)} />
                  </label>
                  <label className="flex flex-col gap-1 text-body-sm">
                    Expense ratio (optional)
                    <input className="rounded-lg border px-3 py-2" value={etfExpense} onChange={(e) => setEtfExpense(e.target.value)} />
                  </label>
                </div>
              ) : null}
              {newAssetType === "BOND" ? (
                <div className="mb-4 flex flex-wrap gap-3">
                  <label className="flex flex-col gap-1 text-body-sm">
                    Rate %
                    <input required className="rounded-lg border px-3 py-2" value={bondRate} onChange={(e) => setBondRate(e.target.value)} />
                  </label>
                  <label className="flex flex-col gap-1 text-body-sm">
                    Maturity
                    <input type="date" required className="rounded-lg border px-3 py-2" value={bondMaturity} onChange={(e) => setBondMaturity(e.target.value)} />
                  </label>
                  <label className="flex flex-col gap-1 text-body-sm">
                    Issuer
                    <input required className="rounded-lg border px-3 py-2" value={bondIssuer} onChange={(e) => setBondIssuer(e.target.value)} />
                  </label>
                </div>
              ) : null}
              {newAssetType === "CRYPTO" ? (
                <div className="mb-4">
                  <label className="flex flex-col gap-1 text-body-sm">
                    Blockchain
                    <input required className="max-w-xs rounded-lg border px-3 py-2" value={cryptoChain} onChange={(e) => setCryptoChain(e.target.value)} />
                  </label>
                </div>
              ) : null}
              <button type="submit" disabled={loading} className="rounded-lg bg-primary px-6 py-2 font-label-caps text-on-primary">
                Create asset
              </button>
            </form>
          ) : null}

          <div className="relative group">
            <MaterialIcon
              name="search"
              className="absolute left-4 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-secondary"
            />
            <input
              className="w-full rounded-lg border border-outline-variant bg-surface-container-lowest py-4 pl-12 pr-4 font-body-md transition-all focus:border-secondary focus:outline-none focus:ring-1 focus:ring-secondary"
              placeholder="Search by symbol or asset name…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  setPage(0);
                  setSearchApplied(searchInput.trim() || undefined);
                }
              }}
            />
          </div>

          <div className="flex w-full flex-wrap gap-1 rounded-lg border border-outline-variant bg-surface-container p-1 md:w-max">
            {segments.map((s) => (
              <button
                key={s.id}
                type="button"
                onClick={() => {
                  setPage(0);
                  setSegment(s.id);
                }}
                className={`rounded-[0.125rem] px-6 py-2 font-label-caps text-label-caps ${
                  segment === s.id
                    ? "bg-surface-container-lowest text-primary shadow-sm"
                    : "text-on-surface-variant hover:bg-surface-container-low"
                }`}
              >
                {s.label}
              </button>
            ))}
          </div>
        </div>
      </section>

      <div className="hidden grid-cols-12 gap-2 border-b border-outline-variant px-card-padding py-2 font-label-caps text-label-caps text-on-surface-variant lg:grid">
        <div className="col-span-5">Asset</div>
        <div className="col-span-3 text-right">Last price</div>
        <div className="col-span-2 text-right">Type</div>
        <div className="col-span-2 text-right">Update</div>
      </div>

      <div className="grid grid-cols-1 gap-gutter lg:grid-cols-1">
        {assets.map((a) => (
          <div
            key={a.id}
            className="grid grid-cols-1 items-center rounded-lg border border-outline-variant bg-surface-container-lowest p-card-padding transition-colors hover:bg-surface-container-low vestox-shadow lg:grid-cols-12"
          >
            <div className="mb-4 flex items-center gap-4 lg:col-span-5 lg:mb-0">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg border border-outline-variant bg-surface-container font-bold text-primary">
                {a.symbol.slice(0, 4)}
              </div>
              <div>
                <div className="font-body-lg font-bold text-on-surface">{a.name}</div>
                <div className="font-label-caps text-label-caps text-on-surface-variant">{assetMeta(a)}</div>
              </div>
            </div>
            <div className="mb-2 text-left font-data-mono text-body-md text-on-surface lg:col-span-3 lg:mb-0 lg:text-right">
              ${a.currentPrice.toFixed(4)}
            </div>
            <div className="mb-2 font-label-caps text-on-surface-variant lg:col-span-2 lg:mb-0 lg:text-right">{a.assetType}</div>
            <div className="flex flex-wrap items-center gap-2 lg:col-span-2 lg:justify-end">
              <input
                className="w-28 rounded border border-outline-variant px-2 py-1 font-data-mono"
                placeholder="New price"
                value={priceDrafts[a.id] ?? ""}
                onChange={(e) => setPriceDrafts((d) => ({ ...d, [a.id]: e.target.value }))}
                inputMode="decimal"
              />
              <button
                type="button"
                className="rounded-lg border border-secondary px-3 py-1 font-label-caps text-secondary"
                disabled={loading}
                onClick={() => void handleUpdatePrice(a.id)}
              >
                Save
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-section-gap flex flex-wrap items-center justify-center gap-3">
        <label className="flex items-center gap-2 font-body-sm text-on-surface-variant">
          Page size
          <select
            className="rounded border bg-surface-container-lowest px-2 py-1"
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0);
            }}
          >
            {PAGE_SIZES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className="rounded-lg border border-outline px-4 py-2 font-label-caps"
          disabled={loading || page === 0}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          Previous
        </button>
        <button
          type="button"
          className="rounded-lg border border-outline px-4 py-2 font-label-caps"
          disabled={loading || assets.length < size}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </button>
        <button
          type="button"
          className="rounded-lg bg-primary px-6 py-2 font-label-caps text-on-primary"
          onClick={() => {
            setPage(0);
            setSearchApplied(searchInput.trim() || undefined);
          }}
        >
          Search
        </button>
      </div>
    </main>
  );
}

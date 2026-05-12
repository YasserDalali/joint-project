import { useCallback, useEffect, useState } from "react";
import { apiClient } from "./api/client";
import type { components } from "./generated/api-schema";
import "./App.css";

type Tab = "users" | "assets" | "portfolio";

type UserRow = components["schemas"]["UserResponse"];
type AssetRow = components["schemas"]["AssetResponse"];
type PortfolioData = components["schemas"]["PortfolioResponse"];
type RiskData = components["schemas"]["RiskScoreResponse"];

function formatApiError(error: unknown): string {
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

export default function App() {
  const [tab, setTab] = useState<Tab>("users");

  const [users, setUsers] = useState<UserRow[]>([]);
  const [usersMeta, setUsersMeta] = useState<string>("");
  const [assets, setAssets] = useState<AssetRow[]>([]);
  const [assetsMeta, setAssetsMeta] = useState<string>("");

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");

  const [accountId, setAccountId] = useState("1");
  const [portfolio, setPortfolio] = useState<PortfolioData | null>(null);
  const [risk, setRisk] = useState<RiskData | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/users", {
      params: { query: { page: 0, size: 50 } },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (data) {
      setUsers(data.content);
      setUsersMeta(
        `page ${data.page + 1}/${Math.max(1, data.totalPages)} · ${data.totalElements} users`,
      );
    }
  }, []);

  const loadAssets = useCallback(async () => {
    setLoading(true);
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/assets", {
      params: { query: { page: 0, size: 50 } },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      return;
    }
    if (data) {
      setAssets(data.content);
      setAssetsMeta(
        `page ${data.page + 1}/${Math.max(1, data.totalPages)} · ${data.totalElements} assets`,
      );
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      if (tab === "users") void loadUsers();
      if (tab === "assets") void loadAssets();
    });
  }, [tab, loadUsers, loadAssets]);

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
      await loadUsers();
    }
  }

  async function loadPortfolioSnapshot() {
    const id = Number(accountId);
    if (!Number.isFinite(id) || id <= 0) {
      setError("Enter a positive numeric account id");
      return;
    }
    setLoading(true);
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/accounts/{accountId}/portfolio", {
      params: { path: { accountId: id } },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      setPortfolio(null);
      return;
    }
    setPortfolio(data ?? null);
  }

  async function loadRiskSnapshot() {
    const id = Number(accountId);
    if (!Number.isFinite(id) || id <= 0) {
      setError("Enter a positive numeric account id");
      return;
    }
    setLoading(true);
    setError(null);
    const { data, error: err } = await apiClient.GET("/api/v1/accounts/{accountId}/risk", {
      params: { path: { accountId: id } },
    });
    setLoading(false);
    if (err) {
      setError(formatApiError(err));
      setRisk(null);
      return;
    }
    setRisk(data ?? null);
  }

  return (
    <div className="app">
      <header className="app__header">
        <h1>FinRisk console</h1>
        <p>
          Types from <code>openapi-typescript</code> · HTTP via{" "}
          <code>openapi-fetch</code> (typed client for the generated schema). Regenerate with{" "}
          <code>npm run generate:api</code>.
        </p>
      </header>

      <div className="tabs" role="tablist" aria-label="API areas">
        {(
          [
            ["users", "Users"],
            ["assets", "Assets"],
            ["portfolio", "Portfolio & risk"],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={tab === id}
            onClick={() => setTab(id)}
          >
            {label}
          </button>
        ))}
      </div>

      {error ? (
        <div className="banner banner--error" role="alert">
          {error}
        </div>
      ) : null}

      {tab === "users" ? (
        <section className="panel" aria-labelledby="users-heading">
          <h2 id="users-heading">Users</h2>
          <form className="row" onSubmit={handleCreateUser}>
            <label>
              Full name
              <input
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                autoComplete="name"
              />
            </label>
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </label>
            <button type="submit" disabled={loading}>
              Create user
            </button>
          </form>
          <div className="actions">
            <button type="button" className="secondary" onClick={() => void loadUsers()} disabled={loading}>
              Refresh list
            </button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td>{new Date(u.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="meta">{usersMeta || (loading ? "Loading…" : "No data")}</p>
        </section>
      ) : null}

      {tab === "assets" ? (
        <section className="panel" aria-labelledby="assets-heading">
          <h2 id="assets-heading">Assets</h2>
          <div className="actions">
            <button type="button" className="secondary" onClick={() => void loadAssets()} disabled={loading}>
              Refresh list
            </button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Symbol</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Price (USD)</th>
                </tr>
              </thead>
              <tbody>
                {assets.map((a) => (
                  <tr key={a.id}>
                    <td>{a.id}</td>
                    <td>{a.symbol}</td>
                    <td>{a.name}</td>
                    <td>{a.assetType}</td>
                    <td>{a.currentPrice.toFixed(4)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="meta">{assetsMeta || (loading ? "Loading…" : "No data")}</p>
        </section>
      ) : null}

      {tab === "portfolio" ? (
        <section className="panel" aria-labelledby="portfolio-heading">
          <h2 id="portfolio-heading">Portfolio &amp; risk</h2>
          <div className="row">
            <label>
              Account id
              <input
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                inputMode="numeric"
              />
            </label>
            <div className="actions">
              <button type="button" onClick={() => void loadPortfolioSnapshot()} disabled={loading}>
                Load portfolio
              </button>
              <button type="button" className="secondary" onClick={() => void loadRiskSnapshot()} disabled={loading}>
                Load risk
              </button>
            </div>
          </div>
          <div className="portfolio-grid">
            <div>
              <h2>Holdings</h2>
              {!portfolio ? (
                <p className="banner banner--muted">Load portfolio to see holdings and totals.</p>
              ) : (
                <>
                  <p className="stat">
                    <strong>Cash:</strong> {portfolio.cashBalance.toFixed(2)} {portfolio.currency}
                  </p>
                  <p className="stat">
                    <strong>Holdings value:</strong> {portfolio.totalHoldingsValue.toFixed(2)}
                  </p>
                  <p className="stat">
                    <strong>Total account value:</strong> {portfolio.totalAccountValue.toFixed(2)}
                  </p>
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Asset id</th>
                          <th>Symbol</th>
                          <th>Qty</th>
                          <th>Value</th>
                        </tr>
                      </thead>
                      <tbody>
                        {portfolio.holdings.map((h) => (
                          <tr key={h.assetId}>
                            <td>{h.assetId}</td>
                            <td>{h.symbol}</td>
                            <td>{h.quantity}</td>
                            <td>{h.currentValue.toFixed(2)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
            <div>
              <h2>Risk</h2>
              {!risk ? (
                <p className="banner banner--muted">Load risk to see score and breakdown.</p>
              ) : (
                <>
                  <p className="stat">
                    <strong>Score:</strong> {risk.score.toFixed(2)} ({risk.level}) — {risk.strategy}
                  </p>
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Symbol</th>
                          <th>Weight %</th>
                          <th>Volatility</th>
                          <th>Samples</th>
                        </tr>
                      </thead>
                      <tbody>
                        {risk.breakdown.map((b) => (
                          <tr key={b.assetId}>
                            <td>{b.symbol}</td>
                            <td>{(b.weight * 100).toFixed(1)}</td>
                            <td>{b.volatility != null ? b.volatility.toFixed(4) : "—"}</td>
                            <td>{b.sampleSize ?? "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
          </div>
        </section>
      ) : null}
    </div>
  );
}

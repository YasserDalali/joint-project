import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { components } from "../generated/api-schema";
import {
  effectiveSigmaForRow,
  levelForSigma,
  scoreFromWeightedSigma,
  sigmaFallback,
  weightedPortfolioSigma,
} from "../riskMath";

type RiskData = components["schemas"]["RiskScoreResponse"];

const blockBase =
  "mb-2 rounded-lg border border-dashed border-transparent px-2 py-2 transition-all duration-[450ms] ease-out";
const blockInactive = "translate-y-1 opacity-35";
const blockActive = "translate-y-0 border-outline-variant bg-surface-container-lowest opacity-100";

function useCountUp(target: number, active: boolean, decimals = 4): number {
  const [v, setV] = useState(0);
  const raf = useRef<number>(0);

  useEffect(() => {
    if (!active) {
      const t = window.setTimeout(() => setV(0), 0);
      return () => clearTimeout(t);
    }
    const start = performance.now();
    const duration = 700;
    const from = 0;
    const loop = (time: number) => {
      const p = Math.min(1, (time - start) / duration);
      const eased = 1 - Math.cos((p * Math.PI) / 2);
      setV(from + (target - from) * eased);
      if (p < 1) {
        raf.current = requestAnimationFrame(loop);
      }
    };
    raf.current = requestAnimationFrame(loop);
    return () => {
      cancelAnimationFrame(raf.current);
    };
  }, [target, active]);

  const factor = 10 ** decimals;
  if (!active) return 0;
  return Math.round(v * factor) / factor;
}

type Phase =
  | "intro"
  | "weights"
  | "sigma"
  | "contrib"
  | "sum"
  | "score"
  | "bucket"
  | "done";

const PHASE_ORDER: Phase[] = [
  "intro",
  "weights",
  "sigma",
  "contrib",
  "sum",
  "score",
  "bucket",
  "done",
];

export function RiskCalculationAnimator({
  risk,
  holdingsTotalValue,
}: {
  risk: RiskData | null;
  holdingsTotalValue: number | null;
}) {
  const [phaseIdx, setPhaseIdx] = useState(0);

  const rows = useMemo(() => risk?.breakdown ?? [], [risk?.breakdown]);
  const weighted = useMemo(() => weightedPortfolioSigma(rows), [rows]);
  const computedScore = useMemo(() => scoreFromWeightedSigma(weighted), [weighted]);
  const computedLevel = useMemo(() => levelForSigma(weighted), [weighted]);

  const replay = useCallback(() => {
    setPhaseIdx(0);
  }, []);

  useEffect(() => {
    if (!risk || rows.length === 0) return;
    if (phaseIdx >= PHASE_ORDER.length - 1) return;
    const t = window.setTimeout(() => setPhaseIdx((i) => Math.min(i + 1, PHASE_ORDER.length - 1)), 950);
    return () => clearTimeout(t);
  }, [phaseIdx, risk, rows.length]);

  const showWeights = phaseIdx >= PHASE_ORDER.indexOf("weights");
  const showSigma = phaseIdx >= PHASE_ORDER.indexOf("sigma");
  const showContrib = phaseIdx >= PHASE_ORDER.indexOf("contrib");
  const showSum = phaseIdx >= PHASE_ORDER.indexOf("sum");
  const showScore = phaseIdx >= PHASE_ORDER.indexOf("score");
  const showBucket = phaseIdx >= PHASE_ORDER.indexOf("bucket");

  const animatedWeighted = useCountUp(weighted, showSum, 6);
  const animatedScore = useCountUp(computedScore, showScore, 2);

  if (!risk) return null;

  if (rows.length === 0) {
    return (
      <div className="rounded-lg border border-outline-variant bg-surface-container-low p-card-padding">
        <p className="font-label-caps text-label-caps text-primary">Risk calculation</p>
        <p className="mt-2 font-body-sm text-on-surface-variant">
          No holdings (or zero total holdings value). The API returns score <strong className="text-on-surface">0</strong>{" "}
          and level <strong className="text-on-surface">LOW</strong> with an empty breakdown.
        </p>
      </div>
    );
  }

  const mono = "font-data-mono text-data-mono text-body-sm";

  return (
    <div className="rounded-lg border border-outline-variant bg-surface-container-low p-card-padding">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <p className="font-label-caps text-label-caps text-primary">How this score is built ({risk.strategy})</p>
        <div className="flex gap-1" aria-hidden>
          {PHASE_ORDER.filter((p) => p !== "done").map((p, i) => (
            <span
              key={p}
              className={`h-2 w-2 rounded-full transition-colors duration-300 ${i <= phaseIdx ? "bg-secondary" : "bg-outline-variant"}`}
            />
          ))}
        </div>
      </div>

      <p className="mb-3 font-body-sm text-on-surface-variant">
        Holdings value is split into weights <code className={mono}>wᵢ</code> (share of total). Each position contributes{" "}
        <code className={mono}>wᵢ × σᵢ</code>, where <code className={mono}>σᵢ</code> is daily volatility from log returns when
        enough price history exists; otherwise a level-based fallback constant is used (same rules as the Java{" "}
        <code className={mono}>RiskService</code>).
      </p>

      {holdingsTotalValue != null && holdingsTotalValue > 0 ? (
        <p className="mb-3 font-body-sm text-on-surface-variant">
          Reported total holdings value: <strong className="text-on-surface">{holdingsTotalValue.toFixed(2)}</strong> USD
          (weights in the API match this denominator; cash is excluded).
        </p>
      ) : null}

      <div className={`${blockBase} ${showWeights ? blockActive : blockInactive}`}>
        <h3 className="font-body-sm font-semibold text-on-surface">1 · Position weights</h3>
        <p className="mt-1 font-body-sm text-on-surface-variant">
          <code className={mono}>wᵢ</code> = holding value ÷ Σ holding values (values returned by the API).
        </p>
        <ul className="mt-2 list-disc space-y-1 pl-5 font-body-sm text-on-surface">
          {rows.map((r) => (
            <li key={r.assetId}>
              <span className="font-semibold text-primary">{r.symbol}</span>{" "}
              <code className={mono}>w = {(r.weight * 100).toFixed(2)}%</code>
            </li>
          ))}
        </ul>
      </div>

      <div className={`${blockBase} ${showSigma ? blockActive : blockInactive}`}>
        <h3 className="font-body-sm font-semibold text-on-surface">2 · Effective daily σ per symbol</h3>
        <ul className="mt-2 list-disc space-y-1 pl-5 font-body-sm text-on-surface">
          {rows.map((r) => {
            const eff = effectiveSigmaForRow(r);
            const isMeasured = r.volatility != null && Number.isFinite(r.volatility);
            return (
              <li key={r.assetId}>
                <span className="font-semibold text-primary">{r.symbol}</span>{" "}
                {isMeasured ? (
                  <span>
                    σ from prices ≈ <code className={mono}>{eff.toFixed(6)}</code>
                    {r.sampleSize != null ? (
                      <span className="text-on-surface-variant"> ({r.sampleSize} samples)</span>
                    ) : null}
                  </span>
                ) : (
                  <span>
                    fallback({r.riskLevel}) → <code className={mono}>{sigmaFallback(r.riskLevel).toFixed(4)}</code>
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </div>

      <div className={`${blockBase} ${showContrib ? blockActive : blockInactive}`}>
        <h3 className="font-body-sm font-semibold text-on-surface">3 · Contributions wᵢ × σᵢ</h3>
        <ul className="mt-2 list-disc space-y-1 pl-5 font-body-sm text-on-surface">
          {rows.map((r) => {
            const eff = effectiveSigmaForRow(r);
            const c = r.weight * eff;
            return (
              <li key={r.assetId}>
                <span className="font-semibold text-primary">{r.symbol}</span>{" "}
                <code className={mono}>
                  {(r.weight * 100).toFixed(2)}% × {eff.toFixed(6)} = {c.toFixed(6)}
                </code>
              </li>
            );
          })}
        </ul>
      </div>

      <div className={`${blockBase} ${showSum ? blockActive : blockInactive}`}>
        <h3 className="font-body-sm font-semibold text-on-surface">4 · Portfolio σ (weighted sum)</h3>
        <p className="mt-2 font-body-sm text-on-surface">
          σ<sub>port</sub> = Σ wᵢ σᵢ ≈ <strong className="font-data-mono text-secondary">{animatedWeighted.toFixed(6)}</strong>
        </p>
      </div>

      <div className={`${blockBase} ${showScore ? blockActive : blockInactive}`}>
        <h3 className="font-body-sm font-semibold text-on-surface">5 · Score on 0–100 scale</h3>
        <p className="mt-2 font-body-sm text-on-surface">
          score = min(100, σ<sub>port</sub> / 0.12 × 100) ≈{" "}
          <strong className="font-data-mono text-secondary">{animatedScore.toFixed(2)}</strong>
        </p>
        <p className="mt-1 font-body-sm text-on-surface-variant">
          API score: <strong className="text-on-surface">{risk.score.toFixed(2)}</strong> (should match recomputation up to
          floating-point noise).
        </p>
      </div>

      <div className={`${blockBase} ${showBucket ? blockActive : blockInactive}`}>
        <h3 className="font-body-sm font-semibold text-on-surface">
          6 · Risk bucket from σ<sub>port</sub>
        </h3>
        <p className="mt-1 font-body-sm text-on-surface-variant">
          Thresholds (daily σ): &lt; 0.01 → LOW, &lt; 0.03 → MEDIUM, &lt; 0.06 → HIGH, else VERY_HIGH (same as{" "}
          <code className={mono}>VolatilityRiskStrategy.levelForSigma</code>).
        </p>
        <p className="mt-2 font-body-sm text-on-surface">
          Computed level: <strong className="text-secondary">{computedLevel}</strong> · API level:{" "}
          <strong>{risk.level}</strong>
        </p>
      </div>

      <div className="mt-3">
        <button
          type="button"
          className="rounded-lg border border-secondary px-4 py-2 font-label-caps text-secondary transition-colors hover:bg-secondary/5"
          onClick={replay}
        >
          Replay animation
        </button>
      </div>
    </div>
  );
}

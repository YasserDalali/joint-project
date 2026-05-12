import type { components } from "./generated/api-schema";

type RiskLevel = components["schemas"]["RiskLevel"];
type RiskBreakdownItem = components["schemas"]["RiskBreakdownItem"];

/** Mirrors `RiskService.sigmaFallback`. */
export function sigmaFallback(level: RiskLevel): number {
  switch (level) {
    case "LOW":
      return 0.005;
    case "MEDIUM":
      return 0.02;
    case "HIGH":
      return 0.045;
    case "VERY_HIGH":
      return 0.08;
    default:
      return 0.02;
  }
}

/** Effective daily σ used in the weighted sum (same branch logic as `RiskService`). */
export function effectiveSigmaForRow(row: RiskBreakdownItem): number {
  if (row.volatility != null && Number.isFinite(row.volatility)) {
    return row.volatility;
  }
  return sigmaFallback(row.riskLevel);
}

export function weightedPortfolioSigma(breakdown: RiskBreakdownItem[]): number {
  return breakdown.reduce((acc, row) => acc + row.weight * effectiveSigmaForRow(row), 0);
}

export function scoreFromWeightedSigma(weightedSigma: number): number {
  return Math.min(100, (weightedSigma / 0.12) * 100);
}

/** Mirrors `VolatilityRiskStrategy.levelForSigma` for portfolio σ. */
export function levelForSigma(dailySigma: number): RiskLevel {
  if (!Number.isFinite(dailySigma) || Number.isNaN(dailySigma)) {
    return "MEDIUM";
  }
  if (dailySigma < 0.01) return "LOW";
  if (dailySigma < 0.03) return "MEDIUM";
  if (dailySigma < 0.06) return "HIGH";
  return "VERY_HIGH";
}

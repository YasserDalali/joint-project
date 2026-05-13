#!/usr/bin/env bash
# HTTP happy path used by scripts/e2e.sh.
# Postman Flows (visual): see postman/flows/FINRISK-E2E-HAPPY-PATH.md + postman/FinRisk-E2E-Happy-Path.postman_collection.json
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

EMAIL="e2e-$(date +%s)@example.com"

echo "e2e: creating user..."
curl -fsS "$BASE_URL/api/v1/users" \
  -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"E2E User\",\"email\":\"$EMAIL\"}" \
  >"$tmp"
USER_ID="$(jq -r '.id' "$tmp")"
[[ "$USER_ID" != "null" ]] && [[ -n "$USER_ID" ]]

echo "e2e: creating account..."
curl -fsS "$BASE_URL/api/v1/accounts" \
  -H 'Content-Type: application/json' \
  -d "{\"userId\":$USER_ID,\"accountName\":\"Primary\",\"initialDeposit\":100000}" \
  >"$tmp"
ACCOUNT_ID="$(jq -r '.id' "$tmp")"

echo "e2e: deposit..."
curl -fsS "$BASE_URL/api/v1/accounts/$ACCOUNT_ID/deposit" \
  -H 'Content-Type: application/json' \
  -d '{"amount":50000}' >/dev/null

echo "e2e: create stock..."
curl -fsS "$BASE_URL/api/v1/assets" \
  -H 'Content-Type: application/json' \
  -d '{"assetType":"STOCK","symbol":"E2E","name":"E2E Corp","currentPrice":50,"sector":"Tech","exchange":"NASDAQ"}' \
  >"$tmp"
ASSET_ID="$(jq -r '.id' "$tmp")"

echo "e2e: update price..."
curl -fsS "$BASE_URL/api/v1/assets/$ASSET_ID/price" \
  -X PUT \
  -H 'Content-Type: application/json' \
  -d '{"price":55}' >/dev/null

echo "e2e: buy 10..."
curl -fsS "$BASE_URL/api/v1/transactions/buy" \
  -H 'Content-Type: application/json' \
  -d "{\"accountId\":$ACCOUNT_ID,\"assetId\":$ASSET_ID,\"quantity\":10,\"unitPrice\":55}" >/dev/null

echo "e2e: portfolio..."
curl -fsS "$BASE_URL/api/v1/accounts/$ACCOUNT_ID/portfolio" >"$tmp"
QTY="$(jq -r '.holdings[0].quantity' "$tmp")"
[[ "$QTY" == "10" ]]

echo "e2e: sell 4..."
curl -fsS "$BASE_URL/api/v1/transactions/sell" \
  -H 'Content-Type: application/json' \
  -d "{\"accountId\":$ACCOUNT_ID,\"assetId\":$ASSET_ID,\"quantity\":4,\"unitPrice\":60}" >/dev/null

echo "e2e: profit-loss..."
curl -fsS "$BASE_URL/api/v1/accounts/$ACCOUNT_ID/profit-loss" >/dev/null

echo "e2e: risk..."
curl -fsS "$BASE_URL/api/v1/accounts/$ACCOUNT_ID/risk" >/dev/null

echo "e2e happy path OK"

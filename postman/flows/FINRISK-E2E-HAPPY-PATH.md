# FinRisk E2E happy path in **Postman Flows**

This matches `scripts/e2e-happy-path.sh`: health → create user → account → deposit → create stock → update price → buy → portfolio assertion → sell → P&L → risk.

Postman Flows are **visual workflows** built from **blocks** on a canvas, not the same file format as a **Collection** ([Flows overview](https://learning.postman.com/docs/postman-flows/build-flows/overview/)). The collection in this repo holds the HTTP definitions; your Flow will **run those requests** via **HTTP Request** blocks ([HTTP Request block](https://learning.postman.com/docs/postman-flows/reference/blocks/http-request/)).

## Prerequisites

1. **Postman desktop** (Flows + Native Git are desktop-first; see [Native Git for Flows](https://learning.postman.com/docs/postman-flows/get-started/flows-native-git/)).
2. API reachable at `**http://localhost:18080`** (Docker) or `**http://localhost:8080**` (host JVM).
3. Import the collection: **Import** → `postman/FinRisk-E2E-Happy-Path.postman_collection.json`.

Set the collection variable `**baseUrl`** on **FinRisk — E2E happy path** to your API origin.

## Build the Flow (recommended pattern)

Follow **[Run requests in sequence using Postman Flows](https://learning.postman.com/docs/postman-flows/tutorials/advanced/run-requests-in-sequence/)** and the public example **[Chaining requests](https://www.postman.com/postman/flows-snippets/flow/6267f9315d367a64e7ba06e5)**.

### 1. Create the flow

- Sidebar → **Flows** (or **Items** → **Flow** per [Build your first flow](https://learning.postman.com/docs/postman-flows/get-started/build-your-first-flow/)).
- Rename to e.g. `FinRisk E2E happy path`.

### 2. Add eleven **HTTP Request** blocks in a chain

For **each** step, add an **HTTP Request** block and pick the matching request from **FinRisk — E2E happy path → E2E happy path**:


| Order | Collection request               |
| ----- | -------------------------------- |
| 1     | `01 — Actuator health`           |
| 2     | `02 — Create user`               |
| 3     | `03 — Create account`            |
| 4     | `04 — Deposit cash`              |
| 5     | `05 — Create stock asset`        |
| 6     | `06 — Update asset price`        |
| 7     | `07 — Buy 10 shares`             |
| 8     | `08 — Portfolio (expect qty 10)` |
| 9     | `09 — Sell 4 shares`             |
| 10    | `10 — Profit / loss`             |
| 11    | `11 — Risk score`                |


**Wire execution order** (critical):

- Connect **Start** → first block’s **Send** input (optional but keeps order explicit), **or** rely on chaining only between HTTP blocks.
- Connect each block’s **Success** output → the **next** block’s **Send** input ([sequence tutorial](https://learning.postman.com/docs/postman-flows/tutorials/advanced/run-requests-in-sequence/)).

Only **2xx** responses go out the **Success** port ([HTTP Request block outputs](https://learning.postman.com/docs/postman-flows/reference/blocks/http-request/)); your creates return **201**, which still counts as success.

### 3. Variables and scripts

The collection uses **collection variables** (`userId`, `accountId`, `assetId`, `e2eEmail`, `e2eSymbol`) filled by **Pre-request** (request 01) and **Tests** scripts on each response.

When Flows runs a request from a collection, **collection Tests and Pre-request scripts normally still run**. That should populate `{{userId}}`, `{{accountId}}`, etc. for the next HTTP Request block.

If any step shows empty variables in the Flow:

- Use **[Select](https://learning.postman.com/docs/postman-flows/reference/blocks/select/)** on the previous **Success** payload and paths such as `body.id` (try `id` if your Postman version wraps JSON differently), then connect **Select** output into the next HTTP Request block’s **variable** port for `userId` / `accountId` / `assetId` ([passing data between systems](https://learning.postman.com/docs/postman-flows/tutorials/advanced/send-information-from-one-system-to-another/)).

### 4. Run

- Toolbar → **Run** (local). Inspect each block’s **Success** preview if something fails.
- Optional: **Cloud Run** from the Run menu ([overview](https://learning.postman.com/docs/postman-flows/build-flows/overview/)).

## Store the Flow in Git (optional)

Postman can persist Flows as files under your repo using **Local View** and **[Native Git](https://learning.postman.com/docs/postman-flows/get-started/flows-native-git/)** so teammates get a `.flow` (or flow JSON) next to this markdown. The on-disk format is **written by Postman**, not maintained by hand in this repository.What this shows. The journey of a buy or sell request through the system. Starts as Requested. Gets Validated. Either Rejected if checks fail or Persisting to the DB. Ends Recorded on success or Failed on database error.

## CLI: `postman flows run` (optional)

If your team has **[Postman CLI** `postman flows run](https://learning.postman.com/docs/postman-cli/postman-cli-flows/)` and a **local flow file** exported from Desktop, you can run:

```bash
postman login
postman flows run path/to/your-flow.json --input baseUrl=http://localhost:18080
```

Enterprise/plan limits may apply; see the linked doc.

## Related Postman docs


| Topic               | Link                                                                                                                                                           |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Flows overview      | [Build flows overview](https://learning.postman.com/docs/postman-flows/build-flows/overview/)                                                                  |
| HTTP Request block  | [HTTP Request block](https://learning.postman.com/docs/postman-flows/reference/blocks/http-request/)                                                           |
| Variables in Flows  | [Requests and variables](https://learning.postman.com/docs/postman-flows/build-flows/configure/requests-and-variables/)                                        |
| Sequence            | [Run requests in sequence](https://learning.postman.com/docs/postman-flows/tutorials/advanced/run-requests-in-sequence/)                                       |
| Data between blocks | [Send information from one system to another](https://learning.postman.com/docs/postman-flows/tutorials/advanced/send-information-from-one-system-to-another/) |
| Select block        | [Select](https://learning.postman.com/docs/postman-flows/reference/blocks/select/)                                                                             |
| Troubleshooting     | [Troubleshoot Flows](https://learning.postman.com/docs/postman-flows/build-flows/troubleshoot/)                                                                |



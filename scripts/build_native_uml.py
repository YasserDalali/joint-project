#!/usr/bin/env python3
"""Build native StarUML use-case + activity diagrams into the FinRisk model.

The StarUML MCP server (`generate_diagram`) only ingests Mermaid, which has no
support for UML use-case or activity diagrams. To get real UML diagrams in
StarUML we have to construct them directly in the `.mdj` JSON.

This script:
  1. Reads the current in-memory model from StarUML's crash-recovery file
     `~/.config/StarUML/__backup.mdj`.
  2. Drops the two flowchart approximations of the use-case and activity
     diagrams (the last two `FCFlowchart` entries).
  3. Builds a real `UMLUseCaseDiagram` (Investor actor + 8 use cases + 8
     associations).
  4. Builds a real `UMLActivityDiagram` with five `UMLActivityPartition`
     swimlanes (User / Controller / Service / Strategy / DAO+SQL) tracing the
     risk-score computation, complete with initial node, actions and final
     node connected by control flows.
  5. Writes the result to `staruml/finrisk.mdj` next to the project.

Open the resulting file with File -> Open in StarUML. The two new diagrams
will be the last entries in the Model Explorer.
"""

from __future__ import annotations

import base64
import copy
import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BACKUP = Path.home() / ".config" / "StarUML" / "__backup.mdj"
OUT = ROOT / "staruml" / "finrisk.mdj"


# ---------------------------------------------------------------------------
# ID helper
# ---------------------------------------------------------------------------

def gid() -> str:
    """Generate a unique 20-char base64 ID in StarUML's format."""
    # 14 random bytes -> base64 == 20 chars with 1 '=' suffix
    return base64.b64encode(os.urandom(14)).decode()


# ---------------------------------------------------------------------------
# Generic view builders
# ---------------------------------------------------------------------------

FONT = "Arial;13;0"
FONT_BOLD = "Arial;13;1"
FONT_ITALIC = "Arial;13;2"


def name_compartment_view(parent_id: str, model_id: str, name: str,
                          left: int, top: int, width: int,
                          stereotype: str | None = None) -> dict:
    """Build a UMLNameCompartmentView with stereotype / name / namespace / property labels."""
    cmp_id = gid()
    stereo_id = gid()
    name_id = gid()
    ns_id = gid()
    prop_id = gid()
    sub = [
        {
            "_type": "LabelView",
            "_id": stereo_id,
            "_parent": {"$ref": cmp_id},
            "font": FONT, "parentStyle": True,
            "left": left + 5, "top": top + 5,
            "width": width - 10, "height": 13,
            "text": f"\u00ab{stereotype}\u00bb" if stereotype else "",
            "horizontalAlignment": 1,
            "visible": bool(stereotype),
        },
        {
            "_type": "LabelView",
            "_id": name_id,
            "_parent": {"$ref": cmp_id},
            "font": FONT_BOLD, "parentStyle": True,
            "left": left + 5, "top": top + 20,
            "width": width - 10, "height": 13,
            "text": name,
            "horizontalAlignment": 1,
        },
        {
            "_type": "LabelView",
            "_id": ns_id,
            "_parent": {"$ref": cmp_id},
            "visible": False,
            "font": FONT, "parentStyle": True,
            "width": width - 10, "height": 13,
            "text": "",
        },
        {
            "_type": "LabelView",
            "_id": prop_id,
            "_parent": {"$ref": cmp_id},
            "visible": False,
            "font": FONT, "parentStyle": True,
            "height": 13,
            "horizontalAlignment": 1,
        },
    ]
    return {
        "_type": "UMLNameCompartmentView",
        "_id": cmp_id,
        "_parent": {"$ref": parent_id},
        "model": {"$ref": model_id},
        "subViews": sub,
        "font": FONT, "parentStyle": True,
        "left": left, "top": top,
        "width": width, "height": 40,
        "stereotypeLabel": {"$ref": stereo_id},
        "nameLabel": {"$ref": name_id},
        "namespaceLabel": {"$ref": ns_id},
        "propertyLabel": {"$ref": prop_id},
    }


def edge_label_view(edge_id: str, model_id: str, text: str = "",
                    visible: bool = True, distance: int = 25,
                    alpha: float = -0.523, edge_pos: int = 1) -> dict:
    return {
        "_type": "EdgeLabelView",
        "_id": gid(),
        "_parent": {"$ref": edge_id},
        "model": {"$ref": model_id},
        "visible": visible,
        "font": FONT, "parentStyle": False,
        "height": 13,
        "alpha": alpha, "distance": distance,
        "hostEdge": {"$ref": edge_id},
        "edgePosition": edge_pos,
        "text": text,
    }


# ---------------------------------------------------------------------------
# Use case diagram builder
# ---------------------------------------------------------------------------

USE_CASES = [
    "Register and manage own user",
    "Create or list own accounts",
    "Browse available assets",
    "Buy an asset",
    "Sell an asset",
    "View portfolio holdings and value",
    "View risk score and breakdown",
    "View transaction history",
]


def build_use_case_diagram(model_id: str) -> dict:
    """Return a UMLPackage owning a UMLUseCaseDiagram + actor + 8 use cases."""
    pkg_id = gid()
    diag_id = gid()
    actor_id = gid()

    # Model elements (actor, use cases, associations)
    elements: list[dict] = [
        {
            "_type": "UMLActor",
            "_id": actor_id,
            "_parent": {"$ref": pkg_id},
            "name": "Investor",
        },
    ]
    uc_ids: list[str] = []
    assoc_ids: list[tuple[str, str, str]] = []  # (assoc_id, end1_id, end2_id)
    for name in USE_CASES:
        uc_id = gid()
        uc_ids.append(uc_id)
        elements.append({
            "_type": "UMLUseCase",
            "_id": uc_id,
            "_parent": {"$ref": pkg_id},
            "name": name,
        })

    # Associations between actor and each use case (owned by the actor)
    for uc_id in uc_ids:
        a_id = gid()
        e1_id = gid()
        e2_id = gid()
        assoc_ids.append((a_id, e1_id, e2_id))
        # Attach association to the actor as its ownedElement
        elements[0].setdefault("ownedElements", []).append({
            "_type": "UMLAssociation",
            "_id": a_id,
            "_parent": {"$ref": actor_id},
            "end1": {
                "_type": "UMLAssociationEnd",
                "_id": e1_id,
                "_parent": {"$ref": a_id},
                "reference": {"$ref": actor_id},
            },
            "end2": {
                "_type": "UMLAssociationEnd",
                "_id": e2_id,
                "_parent": {"$ref": a_id},
                "reference": {"$ref": uc_id},
                "navigable": "navigable",
            },
        })

    # Note / description text box (UMLConstraint or UMLNote)
    note_id = gid()
    elements.append({
        "_type": "UMLConstraint",
        "_id": note_id,
        "_parent": {"$ref": pkg_id},
        "name": "Description",
        "specification": (
            "What this shows. An Investor (actor) can trigger any of 8 use "
            "cases (ovals) exposed by the FinRisk API. Each line is one "
            "feature. The investor is the only actor today - no admin or "
            "analyst role exists. Use cases map roughly 1:1 to endpoint "
            "groups in openapi.yaml."
        ),
    })

    # ------------------------------------------------------------------ views
    # Layout: actor at x=80, use cases stacked vertically at x=440
    actor_view_id = gid()
    actor_cmp = name_compartment_view(actor_view_id, actor_id, "Investor",
                                      left=70, top=320, width=80)
    actor_view = {
        "_type": "UMLActorView",
        "_id": actor_view_id,
        "_parent": {"$ref": diag_id},
        "model": {"$ref": actor_id},
        "subViews": [actor_cmp],
        "font": FONT, "fontColor": "#000000",
        "left": 70, "top": 280, "width": 80, "height": 120,
        "nameCompartment": {"$ref": actor_cmp["_id"]},
    }

    uc_views: list[dict] = []
    uc_view_ids: list[str] = []
    Y_TOP = 80
    Y_STEP = 80
    UC_X = 440
    UC_W = 240
    UC_H = 60
    for idx, (uc_id, name) in enumerate(zip(uc_ids, USE_CASES)):
        uc_view_id = gid()
        uc_view_ids.append(uc_view_id)
        cmp = name_compartment_view(uc_view_id, uc_id, name,
                                    left=UC_X, top=Y_TOP + idx * Y_STEP,
                                    width=UC_W)
        uc_views.append({
            "_type": "UMLUseCaseView",
            "_id": uc_view_id,
            "_parent": {"$ref": diag_id},
            "model": {"$ref": uc_id},
            "subViews": [cmp],
            "font": FONT, "fontColor": "#000000",
            "left": UC_X, "top": Y_TOP + idx * Y_STEP,
            "width": UC_W, "height": UC_H,
            "nameCompartment": {"$ref": cmp["_id"]},
        })

    # Association views (lines from actor to each use case)
    assoc_views: list[dict] = []
    for idx, ((a_id, e1_id, e2_id), uc_view_id) in enumerate(zip(assoc_ids,
                                                                  uc_view_ids)):
        edge_id = gid()
        # 6 sub-EdgeLabels: 3 per end (name, stereotype, multiplicity)
        sub = [
            edge_label_view(edge_id, a_id, "", visible=False, distance=15,
                            alpha=1.5707963, edge_pos=1),
            edge_label_view(edge_id, a_id, "", visible=False, distance=30,
                            alpha=1.5707963, edge_pos=1),
            edge_label_view(edge_id, e1_id, "", visible=False, distance=15,
                            alpha=-1.5707963, edge_pos=1),
            edge_label_view(edge_id, e2_id, "", visible=False, distance=30,
                            alpha=0.5236, edge_pos=2),
            edge_label_view(edge_id, e2_id, "", visible=False, distance=40,
                            alpha=0.7854, edge_pos=2),
            edge_label_view(edge_id, e2_id, "", visible=False, distance=25,
                            alpha=-0.5236, edge_pos=2),
        ]
        uc_y = Y_TOP + idx * Y_STEP + UC_H // 2
        actor_anchor_y = 340  # middle of actor view
        assoc_views.append({
            "_type": "UMLAssociationView",
            "_id": edge_id,
            "_parent": {"$ref": diag_id},
            "model": {"$ref": a_id},
            "subViews": sub,
            "font": FONT, "parentStyle": False,
            "head": {"$ref": uc_view_id},
            "tail": {"$ref": actor_view_id},
            "lineStyle": 1,
            "points": f"150:{actor_anchor_y};{UC_X}:{uc_y}",
            "showVisibility": True,
            "nameLabel": {"$ref": sub[0]["_id"]},
            "stereotypeLabel": {"$ref": sub[1]["_id"]},
            "propertyLabel": {"$ref": sub[2]["_id"]},
            "tailRoleNameLabel": {"$ref": sub[3]["_id"]},
            "tailPropertyLabel": {"$ref": sub[4]["_id"]},
            "tailMultiplicityLabel": {"$ref": sub[5]["_id"]},
        })

    # Note view: textual description box
    note_view_id = gid()
    note_view = {
        "_type": "UMLConstraintView",
        "_id": note_view_id,
        "_parent": {"$ref": diag_id},
        "model": {"$ref": note_id},
        "font": FONT, "parentStyle": False,
        "left": 30, "top": 30, "width": 660, "height": 40,
    }

    diagram = {
        "_type": "UMLUseCaseDiagram",
        "_id": diag_id,
        "_parent": {"$ref": pkg_id},
        "name": "Investor use cases",
        "ownedViews": [note_view, actor_view] + uc_views + assoc_views,
    }

    return {
        "_type": "UMLPackage",
        "_id": pkg_id,
        "_parent": {"$ref": model_id},
        "name": "07. Use cases",
        "ownedElements": [diagram] + elements,
    }


# ---------------------------------------------------------------------------
# Activity diagram builder
# ---------------------------------------------------------------------------

SWIMLANES = [
    "User",
    "RiskController",
    "RiskService",
    "VolatilityRiskStrategy",
    "DAOs and SQL Server",
]

# (lane_index, action_name)
ACTIONS = [
    (0, "GET /api/v1/accounts/1/risk-score"),
    (1, "Receive HTTP request"),
    (2, "Load account holdings"),
    (4, "Query vw_portfolio_holdings"),
    (2, "For each holding load price history"),
    (4, "Query asset_price_history"),
    (3, "Compute log returns ri = ln(pi/pi-1)"),
    (3, "Compute standard deviation = volatility"),
    (3, "Return per-holding score"),
    (2, "Weight per-holding score by holding value"),
    (2, "Map portfolio score to RiskLevel"),
    (1, "Build response"),
    (0, "Receive RiskScore response"),
]

# Control flow edges as (source_index_in_ACTIONS, target_index_in_ACTIONS)
# -1 = initial node, len(ACTIONS) = final node
FLOWS = [
    (-1, 0),
    (0, 1),
    (1, 2),
    (2, 3),
    (3, 2),
    (2, 4),
    (4, 5),
    (5, 4),
    (4, 6),
    (6, 7),
    (7, 8),
    (8, 9),
    (9, 10),
    (10, 11),
    (11, 12),
    (12, len(ACTIONS)),  # final
]


LANE_W = 240
LANE_H = 900
ACTION_W = 180
ACTION_H = 40


def lane_x(idx: int) -> int:
    return 30 + idx * LANE_W


def build_activity_diagram(model_id: str) -> dict:
    """Return a UMLPackage holding a UMLActivity + UMLActivityDiagram."""
    pkg_id = gid()
    diag_id = gid()
    activity_id = gid()

    # ---- model elements ----------------------------------------------------
    # 5 partitions
    partitions: list[dict] = []
    partition_ids: list[str] = []
    for name in SWIMLANES:
        pid = gid()
        partition_ids.append(pid)
        partitions.append({
            "_type": "UMLActivityPartition",
            "_id": pid,
            "_parent": {"$ref": activity_id},
            "name": name,
        })

    # Actions
    action_ids: list[str] = []
    actions_model: list[dict] = []
    for lane_idx, name in ACTIONS:
        aid = gid()
        action_ids.append(aid)
        actions_model.append({
            "_type": "UMLAction",
            "_id": aid,
            "_parent": {"$ref": activity_id},
            "name": name,
            "kind": "opaque",
        })

    # Initial + final nodes
    initial_id = gid()
    final_id = gid()
    initial_model = {
        "_type": "UMLInitialNode",
        "_id": initial_id,
        "_parent": {"$ref": activity_id},
        "name": "",
    }
    final_model = {
        "_type": "UMLActivityFinalNode",
        "_id": final_id,
        "_parent": {"$ref": activity_id},
        "name": "",
    }

    # Control flows
    flows_model: list[dict] = []
    flow_ids: list[str] = []
    for s, t in FLOWS:
        fid = gid()
        flow_ids.append(fid)
        src_ref = initial_id if s == -1 else (
            final_id if s == len(ACTIONS) else action_ids[s])
        tgt_ref = final_id if t == len(ACTIONS) else action_ids[t]
        flows_model.append({
            "_type": "UMLControlFlow",
            "_id": fid,
            "_parent": {"$ref": activity_id},
            "source": {"$ref": src_ref},
            "target": {"$ref": tgt_ref},
        })

    activity = {
        "_type": "UMLActivity",
        "_id": activity_id,
        "_parent": {"$ref": pkg_id},
        "name": "Risk score calculation",
        "nodes": [initial_model, final_model] + actions_model,
        "edges": flows_model,
        "groups": partitions,
    }

    # description note
    note_id = gid()
    note_model = {
        "_type": "UMLConstraint",
        "_id": note_id,
        "_parent": {"$ref": pkg_id},
        "name": "Description",
        "specification": (
            "What this shows. Step-by-step computation of a portfolio risk "
            "score, with each swimlane representing a different "
            "participant. HTTP arrives at the controller, the service "
            "gathers holdings and price history via the DAO layer, the "
            "Strategy does the statistical math (log returns + standard "
            "deviation), and the value-weighted result returns to the "
            "user."
        ),
    }

    # ---- views -------------------------------------------------------------
    # Swimlane views (vertical partitions)
    swimlane_views: list[dict] = []
    swimlane_view_ids: list[str] = []
    for idx, pid in enumerate(partition_ids):
        sv_id = gid()
        swimlane_view_ids.append(sv_id)
        swimlane_views.append({
            "_type": "UMLSwimlaneView",
            "_id": sv_id,
            "_parent": {"$ref": diag_id},
            "model": {"$ref": pid},
            "font": FONT_BOLD, "parentStyle": False,
            "left": lane_x(idx), "top": 80,
            "width": LANE_W, "height": LANE_H,
            "isVertical": True,
        })

    # Initial node view (filled circle): top of lane 0
    initial_view_id = gid()
    initial_view = {
        "_type": "UMLControlNodeView",
        "_id": initial_view_id,
        "_parent": {"$ref": diag_id},
        "model": {"$ref": initial_id},
        "font": FONT, "parentStyle": False,
        "left": lane_x(0) + LANE_W // 2 - 8, "top": 105,
        "width": 16, "height": 16,
    }

    # Final node view (bullseye): bottom of lane 0
    final_view_id = gid()
    final_view = {
        "_type": "UMLControlNodeView",
        "_id": final_view_id,
        "_parent": {"$ref": diag_id},
        "model": {"$ref": final_id},
        "font": FONT, "parentStyle": False,
        "left": lane_x(0) + LANE_W // 2 - 10, "top": 880,
        "width": 20, "height": 20,
    }

    # Action views: position each action in its lane, descend Y per row
    action_views: list[dict] = []
    action_view_ids: list[str] = []
    # Assign a row per action based on order they appear in ACTIONS
    row_y = [0] * len(ACTIONS)
    base_y = 150
    step_y = 55
    for i, (lane_idx, _) in enumerate(ACTIONS):
        row_y[i] = base_y + i * step_y

    for i, ((lane_idx, name), aid) in enumerate(zip(ACTIONS, action_ids)):
        x = lane_x(lane_idx) + (LANE_W - ACTION_W) // 2
        y = row_y[i]
        av_id = gid()
        action_view_ids.append(av_id)
        # Action views are simple rectangles with a name label
        cmp = name_compartment_view(av_id, aid, name,
                                    left=x, top=y, width=ACTION_W)
        action_views.append({
            "_type": "UMLActionView",
            "_id": av_id,
            "_parent": {"$ref": diag_id},
            "model": {"$ref": aid},
            "subViews": [cmp],
            "font": FONT, "parentStyle": False,
            "left": x, "top": y, "width": ACTION_W, "height": ACTION_H,
            "nameCompartment": {"$ref": cmp["_id"]},
        })

    # Control flow views
    flow_views: list[dict] = []
    for (s, t), fid in zip(FLOWS, flow_ids):
        head_ref = (final_view_id if t == len(ACTIONS)
                    else action_view_ids[t])
        tail_ref = (initial_view_id if s == -1
                    else action_view_ids[s])
        edge_id = gid()
        sub = [
            edge_label_view(edge_id, fid, "", visible=False, distance=15,
                            alpha=1.5707963, edge_pos=1),
            edge_label_view(edge_id, fid, "", visible=False, distance=30,
                            alpha=1.5707963, edge_pos=1),
            edge_label_view(edge_id, fid, "", visible=False, distance=15,
                            alpha=-1.5707963, edge_pos=1),
        ]
        flow_views.append({
            "_type": "UMLControlFlowView",
            "_id": edge_id,
            "_parent": {"$ref": diag_id},
            "model": {"$ref": fid},
            "subViews": sub,
            "font": FONT, "parentStyle": False,
            "head": {"$ref": head_ref},
            "tail": {"$ref": tail_ref},
            "lineStyle": 1,
            "nameLabel": {"$ref": sub[0]["_id"]},
            "stereotypeLabel": {"$ref": sub[1]["_id"]},
            "propertyLabel": {"$ref": sub[2]["_id"]},
        })

    # description constraint view (top of canvas)
    note_view_id = gid()
    note_view = {
        "_type": "UMLConstraintView",
        "_id": note_view_id,
        "_parent": {"$ref": diag_id},
        "model": {"$ref": note_id},
        "font": FONT, "parentStyle": False,
        "left": 30, "top": 20, "width": 1170, "height": 50,
    }

    diagram = {
        "_type": "UMLActivityDiagram",
        "_id": diag_id,
        "_parent": {"$ref": pkg_id},
        "name": "Risk score calculation",
        "ownedViews": ([note_view]
                       + swimlane_views
                       + [initial_view, final_view]
                       + action_views
                       + flow_views),
    }

    return {
        "_type": "UMLPackage",
        "_id": pkg_id,
        "_parent": {"$ref": model_id},
        "name": "08. Activity - risk score calculation",
        "ownedElements": [diagram, activity, note_model],
    }


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    if not BACKUP.exists():
        raise SystemExit(f"StarUML backup not found at {BACKUP}. "
                         "Open StarUML at least once.")

    project = json.loads(BACKUP.read_text())

    # Find UMLModel root
    model = next(e for e in project["ownedElements"]
                 if e["_type"] == "UMLModel")
    model_id = model["_id"]

    # Drop the last two ownedElements (the use-case + activity flowcharts).
    # Only drop them if they're flowcharts (defensive).
    while project["ownedElements"][-1]["_type"] == "FCFlowchart":
        project["ownedElements"].pop()
        if len(project["ownedElements"]) <= 13:
            break

    # Append native diagrams as the last two packages.
    project["ownedElements"].append(build_use_case_diagram(model_id))
    project["ownedElements"].append(build_activity_diagram(model_id))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(project, indent="\t"))
    print(f"Wrote {OUT} ({OUT.stat().st_size:,} bytes)")
    print("Open File -> Open in StarUML.")


if __name__ == "__main__":
    main()

export const STATUS_ORDER = ["OPEN", "IN_PROGRESS", "IN_REVIEW", "CLOSED"];

export const STATUS_META = {
  OPEN: { label: "Neu", dot: "dot-created", pill: "status-created" },
  IN_PROGRESS: { label: "In Bearbeitung", dot: "dot-processing", pill: "status-processing" },
  IN_REVIEW: { label: "Review", dot: "dot-review", pill: "status-review" },
  CLOSED: { label: "Fertig", dot: "dot-done", pill: "status-done" }
};

export const NEXT_STATUS = { OPEN: "IN_PROGRESS", IN_PROGRESS: "IN_REVIEW", IN_REVIEW: "CLOSED" };

export const ADVANCE_LABEL = {
  OPEN: "Bearbeitung starten",
  IN_PROGRESS: "In Review geben",
  IN_REVIEW: "Freigeben & ausliefern"
};

export const KIND_META = {
  VIDEO: { label: "Video", cls: "kind-video" },
  IMAGE: { label: "Bild", cls: "kind-image" },
  TEXT: { label: "Text", cls: "kind-text" }
};

export const PRIORITY_LABEL = { LOW: "Niedrig", MEDIUM: "Mittel", HIGH: "Hoch" };

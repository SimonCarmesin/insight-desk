import { useEffect, useState } from "react";
import { fetchTicket, fetchComments, updatePriority, postComment } from "../api.js";
import { STATUS_META, NEXT_STATUS, ADVANCE_LABEL, KIND_META, PRIORITY_LABEL } from "../constants.js";

function formatMoney(v) {
  if (v == null || v === "") return "–";
  const n = Number(v);
  if (isNaN(n)) return String(v);
  return n.toLocaleString("de-DE", { style: "currency", currency: "EUR" });
}
function formatTs(iso) {
  if (!iso) return "";
  try {
    return new Date(iso).toLocaleString("de-DE", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
  } catch (e) {
    return iso;
  }
}
function userName(users, id) {
  const u = users.find((x) => x.id === id);
  return u ? u.name : id != null ? "Nutzer #" + id : "–";
}

export default function Drawer({ ticketId, users, onClose, onChanged, onMove, onError }) {
  const [ticket, setTicket] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentAuthor, setCommentAuthor] = useState("Till");
  const [commentText, setCommentText] = useState("");
  const [busy, setBusy] = useState(false);

  const open = ticketId != null;

  async function load() {
    if (ticketId == null) return;
    onError(null);
    try {
      const [t, c] = await Promise.all([fetchTicket(ticketId), fetchComments(ticketId)]);
      setTicket(t);
      setComments(c || []);
    } catch (err) {
      onError("Ticket konnte nicht geladen werden: " + err.message);
    }
  }

  // Nur beim Oeffnen (Wechsel der ticketId) neu laden - nicht bei jedem
  // Board-Refresh im Hintergrund, sonst geht ein angefangener Kommentar
  // beim Tippen verloren.
  useEffect(() => {
    setTicket(null);
    setComments([]);
    setCommentText("");
    setCommentAuthor("Till");
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ticketId]);

  if (!open) {
    return (
      <>
        <div className="scrim" />
        <aside className="drawer" />
      </>
    );
  }

  async function handleAdvance() {
    if (!ticket) return;
    const newStatus = NEXT_STATUS[ticket.status];
    if (!newStatus) return;
    setBusy(true);
    // moveTicket() liegt zentral in App.jsx - dieselbe Funktion, die auch
    // Drag & Drop auf dem Board nutzt (inkl. Bestaetigung bei "Fertig" und
    // Verlaufs-Kommentar). Hier nur zusaetzlich die Drawer-Detailansicht
    // (Kommentare/Ticket) neu laden, wenn es geklappt hat.
    const ok = await onMove(ticket, newStatus);
    if (ok) await load();
    setBusy(false);
  }

  async function handlePriorityChange(e) {
    const priority = e.target.value;
    onError(null);
    try {
      await updatePriority(ticket.id, priority);
      await load();
      await onChanged();
    } catch (err) {
      onError("Prioritaet konnte nicht geaendert werden: " + err.message);
    }
  }

  async function handleCommentSubmit() {
    const author = commentAuthor.trim() || "Till";
    const text = commentText.trim();
    if (!text || !ticket) return;
    onError(null);
    try {
      await postComment(ticket.id, author, text);
      setCommentText("");
      await load();
    } catch (err) {
      onError("Kommentar konnte nicht gespeichert werden: " + err.message);
    }
  }

  if (!ticket) {
    return (
      <>
        <div className="scrim open" onClick={onClose} />
        <aside className="drawer open" />
      </>
    );
  }

  const meta = STATUS_META[ticket.status] || STATUS_META.OPEN;
  const km = KIND_META[ticket.kind] || null;
  const next = NEXT_STATUS[ticket.status];

  return (
    <>
      <div className="scrim open" onClick={onClose} />
      <aside className="drawer open">
        <div className="drawer-head">
          <div className="top-row">
            <div>
              {km ? <span className={"kind-badge " + km.cls}>{km.label}</span> : null}
              <h2 style={{ marginTop: 8 }}>{ticket.title}</h2>
              {ticket.clientName ? <div className="client">{ticket.clientName}</div> : null}
            </div>
            <button className="close-btn" onClick={onClose} aria-label="Schliessen">
              ✕
            </button>
          </div>
          <span className={"status-pill " + meta.pill}>{meta.label}</span>
          <div className="drawer-meta-row">
            <span className="hint" style={{ marginTop: 0 }}>Preis: {formatMoney(ticket.price)}</span>
            <span className="hint" style={{ marginTop: 0 }}>Zugewiesen: {userName(users, ticket.assignedUserId)}</span>
          </div>
        </div>

        <div className="drawer-body">
          <div className="drawer-section">
            <h3>Beschreibung</h3>
            <div className="drawer-desc">{ticket.description || "–"}</div>
          </div>

          <div className="drawer-section">
            <h3>Prioritaet</h3>
            <select value={ticket.priority} onChange={handlePriorityChange}>
              {Object.keys(PRIORITY_LABEL).map((p) => (
                <option value={p} key={p}>
                  {PRIORITY_LABEL[p]}
                </option>
              ))}
            </select>
          </div>

          <div className="drawer-section">
            <h3>Verlauf</h3>
            <div className="timeline">
              {comments.length === 0 ? (
                <div className="tl-empty">Noch keine Eintraege.</div>
              ) : (
                comments.map((c, i) => (
                  <div className="tl-item" key={c.id ?? i}>
                    <div className="tl-dot-wrap">
                      <div className="tl-dot" />
                      {i < comments.length - 1 ? <div className="tl-line" /> : null}
                    </div>
                    <div>
                      <div className="tl-meta">
                        <span className="tl-actor">{c.author}</span>
                        <span className="tl-ts">{formatTs(c.createdAt)}</span>
                      </div>
                      <div className="tl-text">{c.text}</div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="drawer-section">
            <h3>Kommentar hinzufuegen</h3>
            <div className="comment-form">
              <input
                type="text"
                value={commentAuthor}
                maxLength={60}
                placeholder="Dein Name"
                onChange={(e) => setCommentAuthor(e.target.value)}
              />
              <textarea
                value={commentText}
                placeholder="z.B. Rueckfrage an den Kunden, Notiz zur Abnahme ..."
                onChange={(e) => setCommentText(e.target.value)}
              />
              <button className="btn btn-ghost" style={{ alignSelf: "flex-start" }} onClick={handleCommentSubmit}>
                Kommentar speichern
              </button>
            </div>
          </div>
        </div>

        <div className="drawer-actions">
          {next ? (
            <button
              className={"btn btn-block " + (ticket.status === "IN_REVIEW" ? "btn-accent" : "btn-ghost")}
              onClick={handleAdvance}
              disabled={busy}
            >
              {ADVANCE_LABEL[ticket.status]}
            </button>
          ) : (
            <div className="hint" style={{ marginTop: 0 }}>Ausgeliefert — abgeschlossen.</div>
          )}
        </div>
      </aside>
    </>
  );
}

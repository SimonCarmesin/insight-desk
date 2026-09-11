import { KIND_META } from "../constants.js";

function formatMoney(v) {
  if (v == null || v === "") return "–";
  const n = Number(v);
  if (isNaN(n)) return String(v);
  return n.toLocaleString("de-DE", { style: "currency", currency: "EUR" });
}

export default function TicketCard({ ticket, onOpen }) {
  const km = KIND_META[ticket.kind] || null;
  // Abgeschlossene Tickets sind fertig - lassen sich nicht mehr per Drag &
  // Drop verschieben (Backend lehnt einen Status-Wechsel aus CLOSED heraus
  // ohnehin ab, das spiegelt sich hier im UI).
  const draggable = ticket.status !== "CLOSED";

  function handleDragStart(e) {
    e.dataTransfer.setData("text/plain", String(ticket.id));
    e.dataTransfer.effectAllowed = "move";
    e.currentTarget.classList.add("dragging");
  }
  function handleDragEnd(e) {
    e.currentTarget.classList.remove("dragging");
  }

  return (
    <button
      className="ticket-card"
      onClick={() => onOpen(ticket.id)}
      draggable={draggable}
      onDragStart={draggable ? handleDragStart : undefined}
      onDragEnd={draggable ? handleDragEnd : undefined}
    >
      <h4>{ticket.title}</h4>
      {ticket.clientName ? <div className="client">{ticket.clientName}</div> : null}
      <div className="ticket-foot">
        <span>{km ? <span className={"kind-badge " + km.cls}>{km.label}</span> : null}</span>
        <span className="updated">{formatMoney(ticket.price)}</span>
      </div>
    </button>
  );
}

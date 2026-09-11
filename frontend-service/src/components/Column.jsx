import { STATUS_META } from "../constants.js";
import TicketCard from "./TicketCard.jsx";

export default function Column({ status, tickets, onOpen, onDropTicket, dragOverStatus, setDragOverStatus }) {
  const meta = STATUS_META[status];
  const isOver = dragOverStatus === status;

  function handleDragOver(e) {
    // noetig, damit der Browser diesen Bereich ueberhaupt als Drop-Ziel
    // akzeptiert (Standardverhalten von HTML5 Drag & Drop).
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    if (dragOverStatus !== status) setDragOverStatus(status);
  }

  function handleDragLeave(e) {
    // nur zuruecksetzen, wenn wir die Spalte wirklich verlassen (nicht nur
    // zu einem Kind-Element innerhalb der Spalte wechseln)
    if (!e.currentTarget.contains(e.relatedTarget)) {
      setDragOverStatus(null);
    }
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragOverStatus(null);
    const id = Number(e.dataTransfer.getData("text/plain"));
    if (!id) return;
    onDropTicket(id, status);
  }

  return (
    <div className="column">
      <div className="column-head">
        <div className="title-row">
          <span className={"status-dot " + meta.dot}></span>
          <h3>{meta.label}</h3>
          <span className="count-pill">{tickets.length}</span>
        </div>
      </div>
      <div
        className={"column-body" + (isOver ? " drop-target" : "")}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {tickets.length === 0 ? (
          <div className="column-empty">Keine Tickets</div>
        ) : (
          tickets.map((t) => <TicketCard ticket={t} onOpen={onOpen} key={t.id} />)
        )}
      </div>
    </div>
  );
}

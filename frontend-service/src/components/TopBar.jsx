import { STATUS_ORDER, STATUS_META } from "../constants.js";

export default function TopBar({ tickets }) {
  const counts = { OPEN: 0, IN_PROGRESS: 0, IN_REVIEW: 0, CLOSED: 0 };
  tickets.forEach((t) => {
    if (counts[t.status] != null) counts[t.status]++;
  });

  return (
    <div className="topbar">
      <div>
        <span className="eyebrow">Ticket-Pipeline · insight-desk</span>
        <h1>Auftrags-Cockpit</h1>
        <p>
          Eingehende Auftraege als Ticket erfassen und bis zur Freigabe begleiten. Verbunden mit
          ticket-service (Kanban) und user-service (Zuweisung).
        </p>
      </div>
      <div className="stats">
        {STATUS_ORDER.map((s) => (
          <div className="stat" key={s}>
            <div className="n">{counts[s]}</div>
            <div className="l">{STATUS_META[s].label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

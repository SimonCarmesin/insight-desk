import { useEffect, useState, useCallback } from "react";
import { fetchUsers, fetchTickets, updateStatus, postComment } from "./api.js";
import { STATUS_META } from "./constants.js";
import TopBar from "./components/TopBar.jsx";
import ErrorBanner from "./components/ErrorBanner.jsx";
import IntakeForm from "./components/IntakeForm.jsx";
import Board from "./components/Board.jsx";
import Drawer from "./components/Drawer.jsx";

const POLL_INTERVAL_MS = 15000;

export default function App() {
  const [tickets, setTickets] = useState([]);
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [selectedId, setSelectedId] = useState(null);

  const loadAll = useCallback(async () => {
    setError(null);
    try {
      const [u, t] = await Promise.all([fetchUsers(), fetchTickets()]);
      setUsers(u || []);
      setTickets(t || []);
    } catch (err) {
      setError(
        "Daten konnten nicht geladen werden (laeuft ticket-service/user-service? CORS erlaubt?): " + err.message
      );
    }
  }, []);

  const refreshTickets = useCallback(async () => {
    try {
      const t = await fetchTickets();
      setTickets(t || []);
    } catch (err) {
      setError("Tickets konnten nicht aktualisiert werden: " + err.message);
    }
  }, []);

  // Zentrale Stelle fuer jeden Status-Wechsel - genutzt sowohl vom
  // "Weiter"-Button im Drawer als auch vom Drag & Drop auf dem Board, damit
  // beide exakt dasselbe tun (Backend-Call + Verlaufs-Kommentar + Refresh).
  const moveTicket = useCallback(
    async (ticket, newStatus) => {
      if (!ticket || ticket.status === "CLOSED" || ticket.status === newStatus) return false;
      if (newStatus === "CLOSED") {
        const ok = window.confirm(
          `"${ticket.title}" freigeben und ausliefern?\n\nDas setzt das Ticket auf "Fertig" und loest die Benachrichtigung (E-Mail) aus.`
        );
        if (!ok) return false;
      }
      setError(null);
      try {
        await updateStatus(ticket.id, newStatus);
        const note = `Status geaendert: ${STATUS_META[ticket.status].label} → ${STATUS_META[newStatus].label}.`;
        try {
          await postComment(ticket.id, "System", note);
        } catch (e) {
          /* Kommentar ist optional, der Status-Wechsel zaehlt */
        }
        await refreshTickets();
        return true;
      } catch (err) {
        setError("Status konnte nicht geaendert werden: " + err.message);
        return false;
      }
    },
    [refreshTickets]
  );

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  useEffect(() => {
    // Leichtes Live-Update im Hintergrund: nur die Board-Daten, nie das
    // offene Drawer oder das Formular ueberschreiben (sonst gehen unfertige
    // Eingaben - z.B. ein angefangener Kommentar - beim Polling verloren).
    const id = setInterval(() => {
      fetchTickets()
        .then((t) => setTickets(t || []))
        .catch((err) => console.warn("Hintergrund-Update fehlgeschlagen:", err));
    }, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="wrap">
      <TopBar tickets={tickets} />
      <ErrorBanner message={error} onDismiss={() => setError(null)} />

      <div className="layout">
        <div>
          <IntakeForm users={users} onCreated={refreshTickets} onError={setError} />
        </div>
        <div>
          <Board tickets={tickets} onOpen={setSelectedId} onMoveTicket={moveTicket} />
        </div>
      </div>

      <Drawer
        ticketId={selectedId}
        users={users}
        onClose={() => setSelectedId(null)}
        onChanged={refreshTickets}
        onMove={moveTicket}
        onError={setError}
      />
    </div>
  );
}

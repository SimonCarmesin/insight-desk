const CFG = window.APP_CONFIG || {};
export const TICKET_BASE = CFG.TICKET_SERVICE_URL || "http://localhost:8080";
export const USER_BASE = CFG.USER_SERVICE_URL || "http://localhost:8081";
export const INTAKE_BASE = CFG.INTAKE_SERVICE_URL || "http://localhost:8082";

async function apiFetch(url, options) {
  const res = await fetch(url, options);
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (e) {
      data = text;
    }
  }
  if (!res.ok) {
    const msg = typeof data === "string" && data ? data : `${res.status} ${res.statusText}`;
    throw new Error(msg);
  }
  return data;
}

export function fetchUsers() {
  return apiFetch(`${USER_BASE}/users`);
}

export function fetchTickets() {
  return apiFetch(`${TICKET_BASE}/tickets`);
}

export function fetchTicket(id) {
  return apiFetch(`${TICKET_BASE}/tickets/${id}`);
}

export function fetchComments(id) {
  return apiFetch(`${TICKET_BASE}/tickets/${id}/comments`);
}

export function createTicket(payload) {
  return apiFetch(`${TICKET_BASE}/tickets`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export function updateStatus(id, newStatus) {
  return apiFetch(`${TICKET_BASE}/tickets/${id}/status`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ newStatus })
  });
}

export function updatePriority(id, priority) {
  return apiFetch(`${TICKET_BASE}/tickets/${id}/priority?priority=${encodeURIComponent(priority)}`, {
    method: "PATCH"
  });
}

export function postComment(id, author, text) {
  return apiFetch(`${TICKET_BASE}/tickets/${id}/comments`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ author, text })
  });
}

// Intake-Agent: strukturiert den eingefuegten Rohtext einer Anfrage in
// Ticket-Felder. Legt selbst nichts an - das Ergebnis fuellt nur das
// Formular vor, Bestaetigung bleibt beim Menschen.
export function extractFromText(rawText) {
  return apiFetch(`${INTAKE_BASE}/intake/extract`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ rawText })
  });
}

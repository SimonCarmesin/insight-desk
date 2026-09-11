import { useState } from "react";
import { createTicket } from "../api.js";

const EMPTY = {
  title: "",
  description: "",
  clientName: "",
  price: "",
  kind: "",
  priority: "MEDIUM",
  assignedUserId: ""
};

export default function IntakeForm({ users, onCreated, onError }) {
  const [form, setForm] = useState(EMPTY);
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.title.trim() || !form.description.trim() || !form.assignedUserId) return;

    setSubmitting(true);
    onError(null);
    try {
      await createTicket({
        title: form.title.trim(),
        description: form.description.trim(),
        status: "OPEN",
        priority: form.priority,
        assignedUserId: Number(form.assignedUserId),
        clientName: form.clientName.trim() || null,
        price: form.price === "" ? null : Number(form.price),
        kind: form.kind || null
      });
      setForm(EMPTY);
      await onCreated();
    } catch (err) {
      onError("Ticket konnte nicht erstellt werden: " + err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <div className="panel-title">
        <h2>Neues Ticket erfassen</h2>
      </div>
      <form className="intake-form" onSubmit={handleSubmit}>
        <label>
          Titel
          <input
            type="text"
            required
            maxLength={120}
            placeholder="z.B. KI-Roomtour: Reihenhaus Offenbach"
            value={form.title}
            onChange={(e) => update("title", e.target.value)}
          />
        </label>
        <label>
          Beschreibung
          <textarea
            required
            maxLength={2000}
            placeholder="Details zum Auftrag ..."
            value={form.description}
            onChange={(e) => update("description", e.target.value)}
          />
        </label>
        <div className="form-row">
          <label>
            Kunde
            <input
              type="text"
              maxLength={60}
              placeholder="z.B. K. Brenner"
              value={form.clientName}
              onChange={(e) => update("clientName", e.target.value)}
            />
          </label>
          <label>
            Preis (€)
            <input
              type="number"
              min="0"
              step="1"
              placeholder="120"
              value={form.price}
              onChange={(e) => update("price", e.target.value)}
            />
          </label>
        </div>
        <div className="form-row">
          <label>
            Art
            <select value={form.kind} onChange={(e) => update("kind", e.target.value)}>
              <option value="">–</option>
              <option value="VIDEO">Video</option>
              <option value="IMAGE">Bild</option>
              <option value="TEXT">Text</option>
            </select>
          </label>
          <label>
            Prioritaet
            <select value={form.priority} onChange={(e) => update("priority", e.target.value)}>
              <option value="LOW">Niedrig</option>
              <option value="MEDIUM">Mittel</option>
              <option value="HIGH">Hoch</option>
            </select>
          </label>
        </div>
        <label>
          Zugewiesen an
          <select required value={form.assignedUserId} onChange={(e) => update("assignedUserId", e.target.value)}>
            {users.length === 0 ? (
              <option value="">Keine Nutzer in user-service gefunden</option>
            ) : (
              <>
                <option value="">– auswaehlen –</option>
                {users.map((u) => (
                  <option value={u.id} key={u.id}>
                    {u.name}
                  </option>
                ))}
              </>
            )}
          </select>
        </label>
        <button className="btn btn-accent btn-block" type="submit" disabled={submitting}>
          {submitting ? "Wird erstellt …" : "Ticket erstellen"}
        </button>
      </form>
      <p className="hint">
        Lead manuell erfasst (z.B. aus einem Freelance-Marktplatz) — entspricht dem einen Klick im
        Human-in-the-Loop-Workflow. Ein automatischer Intake-Agent kann diesen Schritt spaeter ersetzen.
      </p>
    </>
  );
}

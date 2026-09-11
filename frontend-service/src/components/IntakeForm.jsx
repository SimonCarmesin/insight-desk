import { useState } from "react";
import { createTicket, extractFromText } from "../api.js";

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
  const [rawText, setRawText] = useState("");
  const [extracting, setExtracting] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleExtract() {
    if (!rawText.trim()) return;
    setExtracting(true);
    onError(null);
    try {
      const suggestion = await extractFromText(rawText.trim());
      // Nur Felder uebernehmen, die der Agent tatsaechlich erkannt hat -
      // Rest bleibt wie es ist, damit nichts ueberschrieben wird, das du
      // vielleicht schon selbst eingetragen hattest.
      setForm((f) => ({
        ...f,
        title: suggestion.title ?? f.title,
        description: suggestion.description ?? f.description,
        clientName: suggestion.clientName ?? f.clientName,
        price: suggestion.price != null ? String(suggestion.price) : f.price,
        kind: suggestion.kind ?? f.kind
      }));
    } catch (err) {
      onError("Konnte die Anfrage nicht automatisch strukturieren: " + err.message + " — bitte manuell ausfuellen.");
    } finally {
      setExtracting(false);
    }
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
      setRawText("");
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

      <div className="intake-agent">
        <label>
          Text der Anfrage (optional)
          <textarea
            placeholder="Hier den Text einer gefundenen Anfrage einfuegen (z.B. von einem Freelance-Marktplatz) ..."
            value={rawText}
            onChange={(e) => setRawText(e.target.value)}
          />
        </label>
        <button
          type="button"
          className="btn btn-ghost btn-block"
          onClick={handleExtract}
          disabled={extracting || !rawText.trim()}
        >
          {extracting ? "Agent liest mit …" : "🤖 Mit KI ausfuellen"}
        </button>
        <p className="hint">
          Der Intake-Agent liest nur mit und schlaegt Felder vor - er legt kein Ticket an. Pruefen und
          unten bestaetigen bleibt bei dir.
        </p>
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
        Auftragsquelle bleibt Handarbeit (kein Marktplatz bietet dafuer eine API) - der Intake-Agent
        oben uebernimmt nur das Strukturieren. Anlegen ist weiterhin ein bewusster Klick.
      </p>
    </>
  );
}

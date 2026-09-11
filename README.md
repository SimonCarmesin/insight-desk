# insight-desk — Auftrags-Cockpit

Human-in-the-Loop-System für echte Freelance-Mini-Aufträge (z.B. KI-generierte
Roomtour-Videos für Immobilienanzeigen): Auftragsquellen werden manuell
gesichtet, ein Klick legt ein Ticket an, (künftig) Agenten bearbeiten es,
Freigabe und Auslieferung bleiben beim Menschen. Baut auf einem bestehenden
Kafka-Microservice-Ticketsystem auf.

## Services

| Service | Port | Beschreibung |
|---|---|---|
| frontend-service | 3000 | Auftrags-Cockpit-Dashboard (React + Vite). Kanban-Board (per Drag & Drop bedienbar), Ticket-Drawer mit Aktivitäts-Verlauf, "Neues Ticket erfassen" inkl. Intake-Agent. |
| ticket-service | 8080 | Kernmodell: Tickets (Titel, Beschreibung, Kunde, Preis, Art, Priorität, Status), Kommentare/Aktivitäts-Timeline, Kafka-Producer. |
| user-service | 8081 | Nutzerverwaltung, Zuweisungs-Ziel für Tickets (`assignedUserId`). |
| intake-service | 8082 | Intake-Agent: strukturiert eingefügten Anfrage-Text (Claude API) zu Ticket-Vorschlägen. Legt selbst kein Ticket an. |
| notification-service | – (intern) | Konsumiert Kafka-Events, verschickt eine E-Mail sobald ein Ticket auf `CLOSED` wechselt (Titel/Kunde/Preis). |
| kafka / kafka-ui | 29092 / 8090 | Event-Bus zwischen den Services (Topic `ticket-events`) + Web-UI zum Reinschauen. |
| postgres-ticket / postgres-user | – (intern) | Je eine Postgres-Instanz pro Service. |

**Ablauf:** Eine Auftragsquelle wird manuell gesichtet (kein Marktplatz bietet
dafür eine API) → der Text wird ins Dashboard eingefügt, der Intake-Agent
schlägt Titel/Kunde/Preis/Art vor → nach Prüfung wird das Ticket per Klick
angelegt → durchläuft die Spalten Neu → In Bearbeitung → Review → Fertig
(frei per Drag & Drop verschiebbar, sobald ein Ticket aber `Fertig` ist,
lässt es sich nicht mehr verändern) → beim Setzen auf "Fertig" verschickt
notification-service automatisch die E-Mail.

## Quick Start

Voraussetzung: Docker + Docker Compose.

1. `.env` im Repo-Root prüfen/ausfüllen (liegt lokal, ist gitignored) — siehe
   Tabelle unten. Ohne SMTP-Konfiguration läuft alles normal, es wird nur
   keine E-Mail verschickt (wird geloggt, nicht als Fehler).
2. Alles starten:

   ```
   docker compose up --build
   ```

3. Dashboard: http://localhost:3000
   Kafka-UI (zum Reinschauen in die Topics): http://localhost:8090

### Wichtige Umgebungsvariablen (`.env`)

| Variable | Zweck | Default |
|---|---|---|
| `NOTIFICATION_EMAIL_RECIPIENT` | Empfänger der "Ticket fertig"-Mail | leer (kein Versand) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP-Zugangsdaten | leer / `587` / leer / leer |
| `AI_HUB_BASE_URL` | Basis-URL von adessos internem AI Hub (OpenAI-kompatibel) für den Intake-Agent | `https://adesso-ai-hub.3asabc.de/v1` |
| `AI_HUB_API_KEY` | API-Key für den AI Hub | leer (Agent antwortet mit 503) |
| `AI_HUB_MODEL` | Welches Modell der Intake-Agent über den AI Hub nutzt | `deepseek-v4-flash-sovereign` |
| `FRONTEND_ORIGIN` | CORS: welcher Origin darf ticket-service/user-service/intake-service aus dem Browser ansprechen | `http://localhost:3000` |
| `TICKET_SERVICE_PUBLIC_URL` / `USER_SERVICE_PUBLIC_URL` / `INTAKE_SERVICE_PUBLIC_URL` | Backend-URLs, die der **Browser** aufruft (nicht die internen Docker-Hostnamen) | `http://localhost:8080` / `:8081` / `:8082` |
| `POSTGRES_*` | Zugangsdaten für die beiden Postgres-Instanzen | — |

## Projektstruktur

```
insight-desk/
├── docker-compose.yml
├── .env                     # lokale Zugangsdaten, gitignored
├── ticket-service/          # Spring Boot, Kernmodell + Kafka-Producer
├── user-service/            # Spring Boot, Nutzerverwaltung
├── intake-service/          # Spring Boot, Intake-Agent (Claude API)
├── notification-service/    # Spring Boot, Kafka-Consumer + E-Mail
├── frontend-service/        # React + Vite Dashboard
└── README.md                # diese Datei
```

## Änderungsverlauf

Jede inhaltliche Änderung an den Services wird hier dokumentiert (chronologisch, älteste zuerst) — Datei-für-Datei mit Begründung, damit sich nachvollziehen lässt, was wann warum angepasst wurde.

### Ticket-Objekt & Email-Benachrichtigung: Anpassung an den AI-Gig-Use-Case

Datum: 2026-09-11
Kontext: Auftrags-Cockpit (siehe Cowork-Prototyp) soll an `ticket-service` /
`notification-service` angebunden werden. Das bisherige Ticket-Modell war
ein generisches Helpdesk-Ticket (provisorisch) und kannte weder Kunde/Preis/
Auftragsart noch eine Kommentar-Historie oder ein Ergebnis. Diese Änderung
erweitert es additiv, ohne bestehende generische Nutzung zu brechen.

### ticket-service

**`model/Status.java`** — neuer Enum-Wert `IN_REVIEW`, eingefügt zwischen
`IN_PROGRESS` und `CLOSED`. Grund: im Dashboard muss unterscheidbar sein
"Agent arbeitet noch" vs. "Ergebnis liegt vor, wartet auf deine Freigabe".

**`model/TicketKind.java`** (neu) — Enum `VIDEO`, `IMAGE`, `TEXT`. Steuert im
Frontend, welche Ergebnis-Vorschau gerendert wird.

**`model/Ticket.java`** — drei neue, nullable Felder: `clientName` (String),
`price` (BigDecimal), `kind` (TicketKind). Der bestehende 5-Parameter-
Konstruktor bleibt unverändert erhalten (ruft intern den neuen 8-Parameter-
Konstruktor mit `null` für die drei neuen Felder auf) — bestehender
Aufrufcode und Tests kompilieren unverändert weiter.

**`model/TicketComment.java`** (neu) — eigene Entität statt JSON-Spalte für
die Aktivitäts-Zeitleiste: `id`, `ticket` (ManyToOne-Beziehung), `author`,
`text`, `createdAt`. Sauberer über die Repository-Schicht abfragbar als ein
Freitext-/JSON-Feld.

**`repository/TicketCommentRepo.java`** (neu) — `JpaRepository`, plus
`findByTicketIdOrderByCreatedAtAsc`.

**`model/ticketrequests/CreateTicketRequest.java`** — drei neue, optionale
Felder (`clientName`, `price`, `kind`). Zusätzlicher 5-Parameter-Konstruktor
für den alten, generischen Aufrufweg (ohne Gig-Felder) — bestehende
Testaufrufe wie `new CreateTicketRequest("t","d","OPEN","LOW",1L)`
kompilieren weiter.

**`model/ticketrequests/AddCommentRequest.java`** (neu) — `record(author, text)`.

**`serivce/TicketService.java`** —
- `createTicket`: parst `kind` (falls gesetzt), übergibt die drei neuen
  Felder an `Ticket` und an das `TicketCreatedEvent`.
- `updateTicketStatus`: der "aus CLOSED darf man nicht mehr raus"-Schutz
  greift jetzt auch für `IN_REVIEW` (vorher nur OPEN/IN_PROGRESS geprüft).
  **Weiterhin unverändert**: es wird bewusst nur bei `CLOSED` ein
  `TicketStatusChangedEvent` publiziert (nicht bei jedem Statuswechsel) —
  genau daran hängt jetzt der Email-Versand in notification-service.
- neu: `addComment(ticketId, AddCommentRequest)` und
  `getComments(ticketId)` für die Aktivitäts-Zeitleiste.
- Konstruktor bekommt zusätzlich `TicketCommentRepo` injiziert.

**`controller/TicketController.java`** — zwei neue Endpunkte:
`POST /tickets/{id}/comments` und `GET /tickets/{id}/comments`.

**`event/TicketCreatedEvent.java`**, **`event/TicketStatusChangedEvent.java`**
— je zwei neue Felder `clientName`, `price`, damit notification-service die
Email bauen kann, ohne extra nachzufragen.

### notification-service

**Kernänderung, wie gewünscht:** Email geht jetzt *ausschließlich* bei
Wechsel auf `CLOSED` raus (vorher war es genau umgekehrt program­miert —
Email bei allem *außer* CLOSED, SMS nur bei CLOSED). Die Empfänger-Adresse
kommt aus einer Umgebungsvariable, nicht mehr hardcodiert.

**`event/TicketCreatedEvent.java`**, **`event/TicketStatusChangedEvent.java`**
— gleiche zwei neue Felder wie auf ticket-service-Seite (müssen als eigene
Kopie der Records hier mitgeführt werden, damit Kafka/Jackson sie
deserialisieren kann).

**`service/NotificationSender.java`** — Signatur umgebaut: statt einer
fertigen `(recipient, message)`-Übergabe bekommt jeder Sender jetzt das
volle `TicketEvent` (`handle(event, assignedUserName)`) und baut Empfänger
+ Nachricht selbst (`resolveRecipient`, `buildMessage`). Grund: die Email
braucht jetzt Kunde/Preis aus dem Event, die SMS (bzw. ein künftiger
weiterer Kanal) evtl. andere Daten — das lässt sich so pro Kanal sauber
trennen (Strategy-Pattern).

**`service/EmailNotificationSender.java`** — komplett neu geschrieben:
- `supports()`: nur noch `TicketStatusChangedEvent` mit `newStatus == CLOSED`.
- Empfänger kommt aus `@Value("${notification.email.recipient:}")`,
  gebunden an die Env-Variable `NOTIFICATION_EMAIL_RECIPIENT`
  (siehe `application.properties` und `.env`).
- Verschickt jetzt eine echte Email via `JavaMailSender`
  (SMTP-Konfiguration ebenfalls über Env-Variablen: `MAIL_HOST`,
  `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`), mit Ticket-Titel,
  Kundenname und Preis im Text.
- Ist weder Empfänger noch SMTP konfiguriert, wird **nicht** geworfen/
  gecrasht, sondern nur geloggt (`log.warn`) — der Service bleibt lauffähig,
  auch bevor du echte SMTP-Zugangsdaten eingetragen hast.

**`service/SmsNotificationSender.java`** — inhaltlich unverändert (weiterhin
nur ein Log-Stub), nur an die neue `NotificationSender`-Signatur angepasst.
**Offener Punkt:** er feuert weiterhin ebenfalls bei `CLOSED` — d.h. aktuell
laufen Email und SMS-Stub beide bei Abschluss. Sag Bescheid, ob der
SMS-Kanal raus soll oder bewusst parallel bleiben soll.

**`event/TicketEventConsumer.java`** — ruft jetzt `sender.handle(event, recipientName)`
statt `sender.send(recipient, message)`; Nachrichtenbau ist in die Sender
gewandert (s.o.).

**`build.gradle`** — `spring-boot-starter-mail` als neue Abhängigkeit.

**`application.properties`** — `notification.email.recipient` sowie
`spring.mail.*` (host/port/username/password), alle über Env-Variablen mit
leerem/sicherem Default, sodass der Service auch ohne gesetzte SMTP-Daten
startet.

### docker-compose.yml / .env

`notification-service` bekommt fünf neue Environment-Einträge
(`NOTIFICATION_EMAIL_RECIPIENT`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`,
`MAIL_PASSWORD`), gemappt auf `.env`. In `.env` wurden die Keys ergänzt
(Werte absichtlich leer/Platzhalter gelassen — bitte selbst eintragen):

```
NOTIFICATION_EMAIL_RECIPIENT=   # deine Adresse, an die die "fertig"-Mail geht
MAIL_HOST=                      # z.B. smtp.gmail.com oder dein Mailtrap-Host
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
```

### Tests angepasst

`TicketServiceTest.java` und `TicketControllerTest.java` (ticket-service)
mussten **nicht** angefasst werden — Mockito injiziert `TicketCommentRepo`
per Reflection einfach als `null` mit, und kein Testfall dort nutzt
Kommentare. `TicketServiceIntegrationTest.java` läuft über den echten
Spring-Kontext, der die neue Abhängigkeit automatisch auflöst.

`EmailNotificationSenderTest.java`, `SmsNotificationSenderTest.java`,
`TicketEventConsumerTest.java` (notification-service) wurden überarbeitet:
neue Konstruktor-Signaturen, `send(...)` → `handle(...)`, und
`EmailNotificationSenderTest` testet jetzt das neue (korrekte) Verhalten
statt des alten.

### Nicht gemacht / offen

- Ich habe den Gradle-Build **nicht** selbst ausgeführt (kein Java-
  Toolchain hier verfügbar) — bitte `./gradlew build` in beiden Services
  laufen lassen und mir Fehler zurückmelden, falls welche auftauchen.
- `intake-service` (Lead-Generierung) und `frontend-service` (Dashboard)
  sind noch nicht angelegt — kommen als nächstes.
- SMS-Kanal bei CLOSED: siehe offener Punkt oben.

### Nachtrag: Kafka JsonSerializer/JsonDeserializer-Warnung (unabhängig von obigem)

Beim Build kam eine Deprecation-Warnung für `JsonSerializer`/`JsonDeserializer`
(`org.springframework.kafka.support.serializer`) — vorbestehender Code, nicht
Teil der obigen Änderung. Hintergrund: beide sind seit Spring Kafka 4.0 mit
`@Deprecated(forRemoval=true)` markiert (Jackson-2-basiert). Ersatz:
`JacksonJsonSerializer` / `JacksonJsonDeserializer` (Jackson-3-basiert,
gleiches Package, **gleiche Konstanten-Namen** `TYPE_MAPPINGS`,
`TRUSTED_PACKAGES`, `USE_TYPE_INFO_HEADERS`) — reiner Klassentausch.

**`ticket-service/.../config/KafkaProducerConfig.java`** —
`JsonSerializer` → `JacksonJsonSerializer` (Import + zwei Stellen im Code).

**`notification-service/.../config/KafkaConsumerConfig.java`** —
`JsonDeserializer` → `JacksonJsonDeserializer` (Import + vier Stellen im Code).

Keine neue Gradle-Abhängigkeit nötig: Jackson 3 kommt bei Spring Boot 4
automatisch über `spring-boot-starter-webmvc` mit. Die bestehende
`com.fasterxml.jackson.core:jackson-databind`-Zeile (Jackson 2) in
`ticket-service/build.gradle` kann bleiben, stört nicht - könnte man bei
Gelegenheit aufräumen, falls sie sonst nirgends mehr gebraucht wird.

### Nachtrag: TicketEventConsumer – unnötiger UserService-Call behoben

Gemeldeter Testfehler: `TicketEventConsumerTest.consume_noSenderSupportsEvent_noSenderIsCalled`
schlug fehl mit `NeverWantedButInvoked` auf `userServiceClient.getUserById(...)`.

**Ursache (vorbestehend, nicht durch die Schema-Änderungen eingeführt):**
`TicketEventConsumer.consume()` hat den Empfänger-Namen (und damit den
User-Service-Call) **immer** aufgelöst, bevor überhaupt geprüft wurde, ob
irgendein `NotificationSender` das Event unterstützt. Wenn kein Sender
zuständig war (z. B. ein `TicketCreatedEvent`, für das aktuell nichts
verschickt wird), wurde `userServiceClient.getUserById(...)` trotzdem
aufgerufen — unnötiger HTTP-Call ins Leere.

**`notification-service/.../event/TicketEventConsumer.java`** — `consume(...)`
umgebaut: die Sender werden zuerst gefiltert (`supports(event)`); ist die
Liste leer, wird sofort zurückgekehrt, **ohne** den User-Service zu
kontaktieren. Erst wenn mindestens ein Sender passt, wird der Empfänger-Name
aufgelöst und an die passenden Sender verteilt.

```java
@KafkaListener(topics = "ticket-events", groupId = "notification-service")
public void consume(TicketEvent event) {
    List<NotificationSender> matchingSenders = senders.stream()
            .filter(sender -> sender.supports(event))
            .toList();

    if (matchingSenders.isEmpty()) {
        return;
    }

    Long assignedUserId = resolveAssignedUserId(event);
    String recipientName = resolveRecipientName(assignedUserId);

    matchingSenders.forEach(sender -> sender.handle(event, recipientName));
}
```

Der Testfall `TicketEventConsumerTest` musste **nicht** angepasst werden —
er hat bereits genau dieses Verhalten erwartet (`verify(userServiceClient,
never()).getUserById(any())`), nur die Implementierung war noch nicht
konsistent dazu. Nebeneffekt: spart einen unnötigen User-Service-Call, wenn
für ein Event ohnehin nichts verschickt wird.

### Nachtrag: frontend-service (neues Auftrags-Cockpit-Dashboard)

Neuer Service `frontend-service/` — statisches Dashboard (kein Build-Step,
kein Framework: plain HTML/CSS/JS, wie im Klick-Prototyp), das direkt per
`fetch()` aus dem Browser gegen `ticket-service` (Kanban, Tickets, Kommentare)
und `user-service` (Liste fuer die Zuweisung) spricht. Design/Look 1:1 aus
dem Prototyp uebernommen (IBM Plex Sans/Mono, Light/Dark-Theme-Tokens,
Kanban-Board, Ticket-Drawer mit Verlauf/Timeline).

**Was sich gegenueber dem Prototyp aendert (bewusst, da echte Daten statt
Demo-Daten):**

- Kein `db`/`sample`-Capability mehr (Claude-spezifisch, ausserhalb von
  claude.ai nicht verfuegbar) — stattdessen echte REST-Calls gegen
  ticket-service/user-service.
- Die "Eingehende Anfragen"-Inbox mit KI-generierten Demo-Leads gibt es noch
  nicht als echtes Feature (kein `intake-service` vorhanden). Stattdessen:
  ein Formular "Neues Ticket erfassen" — du traegst einen gefundenen
  Auftrag (z.B. von einem Freelance-Marktplatz) manuell ein, ein Klick legt
  das Ticket an. Das ist exakt der Human-in-the-Loop-Schritt aus eurem
  Konzept; ein Intake-Agent kann das Formular spaeter ersetzen/vorbefuellen.
- Simulierte Ergebnis-Vorschau (Canvas-Platzhalter fuer Video/Bild) aus dem
  Prototyp wurde **nicht** uebernommen — es gibt noch keine echte
  Agenten-Pipeline, die ein Ergebnis erzeugt. Stattdessen zeigt der Drawer
  die echte Ticket-Beschreibung.
- Die Aktivitaets-Timeline nutzt jetzt die echten Kommentare
  (`GET/POST /tickets/{id}/comments`). Bei jedem Status-Wechsel schreibt das
  Frontend zusaetzlich automatisch einen System-Kommentar ("Status
  geaendert: Neu → In Bearbeitung."), damit der Verlauf weiterhin
  nachvollziehbar bleibt wie im Prototyp.
- Spalten-Mapping: `OPEN` → Neu, `IN_PROGRESS` → In Bearbeitung,
  `IN_REVIEW` → Review, `CLOSED` → Fertig. Der Button im Review-Status
  heisst weiterhin "Freigeben & ausliefern" — genau dieser Schritt setzt
  das Ticket auf `CLOSED` und loest damit (unveraendert) das Kafka-Event
  und die E-Mail in notification-service aus.
- Zusaetzlich (nicht im Prototyp): Prioritaet ist im Drawer aenderbar
  (`PATCH /tickets/{id}/priority`), da das Feld im echten Ticket-Modell
  existiert.
- Leichtes Hintergrund-Polling (alle 15s) aktualisiert nur Board/Stats,
  nie das geoeffnete Drawer oder das Formular — damit geht kein unfertiger
  Kommentar-Entwurf o.ae. verloren.

**Neue/geaenderte Dateien:**

- `frontend-service/src/index.html`, `style.css`, `app.js` — das Dashboard.
- `frontend-service/src/config.js.template` — wird beim Container-Start zu
  `config.js` gerendert (siehe unten), enthaelt die Backend-URLs.
- `frontend-service/docker-entrypoint.sh` — laeuft automatisch beim
  nginx-Start (offizielles `/docker-entrypoint.d/`-Feature des
  `nginx`-Images), fuehrt `envsubst` auf `config.js.template` aus.
- `frontend-service/Dockerfile` — `nginx:1.27-alpine`, kopiert die
  statischen Dateien + das Entrypoint-Script.

**CORS (neu, war vorher gar nicht konfiguriert):**

- `ticket-service/.../config/WebConfig.java` (neu) und
  `user-service/.../config/WebConfig.java` (neu) — erlauben Cross-Origin-
  Requests vom Frontend. Origin kommt aus der Env-Variable
  `FRONTEND_ORIGIN` (Property `frontend.origin`, Default
  `http://localhost:3000`), **nicht hardcodiert** — gleiches Muster wie
  beim E-Mail-Empfaenger.
- `ticket-service/src/main/resources/application.properties` und
  `user-service/src/main/resources/application.properties` — neue Zeile
  `frontend.origin=${FRONTEND_ORIGIN:http://localhost:3000}`.

**docker-compose.yml:**

- Neuer Service `frontend-service`, Port `3000:80`, Env
  `TICKET_SERVICE_PUBLIC_URL` / `USER_SERVICE_PUBLIC_URL` (das sind die
  URLs, die der **Browser** aufruft, also `http://localhost:8080` /
  `http://localhost:8081` — bewusst nicht die internen Docker-Hostnamen,
  die sind vom Browser aus nicht erreichbar).
- `ticket-service` und `user-service` bekommen zusaetzlich
  `FRONTEND_ORIGIN=${FRONTEND_ORIGIN}` in ihren environment-Block.

**.env:** drei neue Zeilen ergaenzt (nicht ueberschrieben):
`FRONTEND_ORIGIN=http://localhost:3000`,
`TICKET_SERVICE_PUBLIC_URL=http://localhost:8080`,
`USER_SERVICE_PUBLIC_URL=http://localhost:8081`.

**Starten/Testen:**

```
docker compose up --build frontend-service ticket-service user-service postgres-ticket postgres-user
```

Dashboard dann unter `http://localhost:3000`. Ich konnte `docker compose
build` hier nicht selbst ausfuehren (kein Docker in meiner Umgebung
verfuegbar) — `app.js` habe ich zumindest mit `node -c` auf Syntaxfehler
geprueft (fehlerfrei), bitte den Build/Compose-Start bei dir gegenpruefen
und mir Fehler zurueckmelden.

**Offen / naechste Schritte:**

- `intake-service` (echte Lead-Generierung) — noch nicht gebaut, siehe oben.
- Kein Loeschen/Bearbeiten der Beschreibung ueber das UI (Backend kann das
  via `PUT /tickets/{id}`, im Dashboard aktuell nicht angebunden) — bei
  Bedarf leicht ergaenzbar.
- SMS-Kanal-Frage weiterhin offen (siehe oben).

### Nachtrag: frontend-service auf React + Vite umgestellt

Auf Wunsch von Till: das eben gebaute Vanilla-JS-Dashboard wurde **ersetzt**
durch eine React+Vite-App (gleiches Design/CSS, gleiche Funktionalitaet,
gleiche REST-Anbindung an ticket-service/user-service). Grund: bessere
Skalierbarkeit fuer die naechsten Ausbaustufen (Agenten, Intake, evtl.
Drag&Drop-Kanban), Standard-Stack.

**Neue Struktur in `frontend-service/`:**

- `package.json`, `vite.config.js`, `index.html` (Vite-Root, nicht mehr in
  `src/`) — Standard-Vite-Setup, `@vitejs/plugin-react`.
- `src/main.jsx` — Einstiegspunkt, rendert `<App />`.
- `src/App.jsx` — State (Tickets, Nutzer, Fehler, ausgewaehltes Ticket),
  laedt Daten beim Start, Hintergrund-Polling alle 15s (nur Board/Stats,
  nie das offene Drawer oder Formular ueberschreiben).
- `src/api.js` — alle Fetch-Calls gegen ticket-service/user-service
  (identische Logik wie zuvor in `app.js`, nur als ES-Module-Funktionen).
- `src/constants.js` — Status-/Kind-/Prioritaets-Mappings.
- `src/components/` — `TopBar`, `ErrorBanner`, `IntakeForm`, `Board`,
  `TicketCard`, `Drawer` (Ticket-Detail mit Verlauf, Kommentarformular,
  Status-/Prioritaets-Aenderung).
- `src/styles.css` — 1:1 aus dem vorherigen `style.css` uebernommen
  (Design bleibt gleich, wie gewuenscht).
- `public/config.js.template` — wie zuvor: wird beim Container-Start aus
  `TICKET_SERVICE_PUBLIC_URL`/`USER_SERVICE_PUBLIC_URL` gerendert (siehe
  `docker-entrypoint.sh`), landet unveraendert im Vite-Build-Output
  (`dist/`), damit die Backend-URLs weiterhin **nicht im Image
  hardcodiert** sind, sondern zur Laufzeit aus docker-compose/.env kommen.
- `public/config.js` — Default fuer lokale Entwicklung ohne Docker
  (`npm run dev`), zeigt auf `localhost:8080`/`localhost:8081`.

Alte Vanilla-JS-Dateien (`src/index.html`, `src/style.css`, `src/app.js`,
`src/config.js.template`) wurden geloescht, nicht nur ueberschrieben.

**Dockerfile jetzt Multi-Stage:**

1. `node:20-alpine` — `npm install` + `npm run build` → `dist/`.
2. `nginx:1.27-alpine` — kopiert nur `dist/` (die fertig gebauten
   statischen Dateien), plus das Entrypoint-Script fuer `config.js`.

`docker-compose.yml` und `.env` mussten **nicht** angepasst werden — der
Service heisst weiterhin `frontend-service`, gleicher Build-Context, gleiche
Env-Variablen (`TICKET_SERVICE_PUBLIC_URL`, `USER_SERVICE_PUBLIC_URL`).

**`.github/workflows/ci.yml`** — Job `build-frontend-service` aktualisiert:
`npm install` + `npm run build` (echter Vite-Build, laeuft auf dem
GitHub-Actions-Runner mit normalem Internetzugang — anders als meine
Sandbox hier) sowie `docker build` des Images.

**Wichtiger Hinweis zu dieser Umstellung — ungetestet:**

Ich konnte `npm install` **weder in meiner Cloud-Umgebung noch ueber die
Geraete-Bruecke zu deinem Mac** ausfuehren — in beiden Faellen ist
`registry.npmjs.org` durch eine Netzwerk-Policy geblockt ("Host not in
allowlist"). Das betrifft nur meine Sandbox-Verbindung zu deinem Rechner,
nicht zwangslaeufig dein normales Docker/Terminal — aber ich konnte es
dadurch **nicht selbst verifizieren**. Ich habe den kompletten React-Code
manuell sorgfaeltig durchgesehen (Imports, JSX-Syntax, Prop-Fluss), aber
das ersetzt keinen echten Build. Bitte als erstes ausfuehren und mir
Fehler zurueckmelden:

```
docker compose build frontend-service
docker compose up frontend-service ticket-service user-service postgres-ticket postgres-user
```

Falls der Build durchlaeuft, waere es gut, danach `package-lock.json` zu
committen (z.B. einmal lokal `npm install` in `frontend-service/`
ausfuehren, falls du Node lokal hast) — dann nutzt das Dockerfile
reproduzierbare Versionen statt bei jedem Build neu aufzuloesen. Aktuell
nutzt das Dockerfile bewusst `npm install` statt `npm ci`, weil noch kein
Lockfile existiert.

Der CI-Job macht also (anders als ich hier) einen echten Build — falls
irgendwo ein Syntaxfehler drin waere, wuerde der naechste Push/PR das
zeigen.

### Nachtrag: Preis-Feld-Bug behoben + Drag & Drop im Kanban-Board

Zwei Punkte aus deinem Feedback zum React-Dashboard:

**1) Preis-Feld lief ueber den Formular-Rand hinaus**

Ursache: CSS-Grid-Items haben per Default `min-width:auto`, das heisst sie
werden nie kleiner als ihre intrinsische Mindestbreite - auch wenn die
Spalte (`1fr`) eigentlich weniger Platz haette. Das `<input>`/`<select>`
im "Preis"-Feld hat den Formular-Rand deshalb gesprengt statt sich
einzuordnen.

**`frontend-service/src/styles.css`** — `min-width:0` auf `.intake-form
label` und `.intake-form .form-row` ergaenzt, plus `width:100%;
min-width:0` direkt auf `input`/`select`/`textarea` im Formular. Das ist
der Standard-Fix fuer dieses CSS-Grid-Verhalten.

**2) Drag & Drop zwischen den Spalten**

Tickets lassen sich jetzt frei zwischen den Spalten ziehen (vor und
zurueck, z.B. auch von "In Bearbeitung" zurueck auf "Neu") - **ausser**
ein Ticket ist bereits `CLOSED` ("Fertig"): das laesst sich nicht mehr
anfassen (`draggable` wird dafuer deaktiviert). Das Backend selbst lehnt
einen Status-Wechsel aus `CLOSED` heraus ohnehin ab
(`TicketService.updateTicketStatus`, unveraendert) - das UI spiegelt das
jetzt nur zusaetzlich wider, statt dem Nutzer eine Aktion anzubieten, die
sowieso fehlschlaegt.

Implementiert mit der nativen HTML5-Drag&Drop-API (keine neue
Abhaengigkeit noetig):

- **`src/components/TicketCard.jsx`** — `draggable` (ausser bei `CLOSED`),
  setzt beim Drag-Start die Ticket-ID via `dataTransfer`.
- **`src/components/Column.jsx`** (neu, vorher war das in `Board.jsx`
  inline) — jede Spalte ist ein Drop-Ziel, hebt sich beim Drueberziehen
  visuell hervor (`drop-target`-Klasse), liest beim Drop die Ticket-ID aus.
- **`src/components/Board.jsx`** — vereinfacht, delegiert an `Column`,
  loest beim Drop `onMoveTicket(ticket, neuerStatus)` aus.
- **`src/App.jsx`** — neue zentrale Funktion `moveTicket(ticket,
  newStatus)`: macht den `PATCH /tickets/{id}/status`-Call, schreibt
  danach denselben "Status geaendert: X → Y"-System-Kommentar wie bisher,
  und aktualisiert das Board. Wird jetzt **sowohl** vom Drag&Drop **als
  auch** vom "Weiter"-Button im Drawer genutzt (`Drawer.jsx` ruft nur noch
  `onMove(...)` auf statt die Logik zu duplizieren).
- **Sicherheitsnetz:** Ziehst du ein Ticket auf "Fertig", kommt vorher ein
  Bestaetigungsdialog (`window.confirm`), weil das echt die
  E-Mail-Benachrichtigung ausloest - ein versehentlicher Drop soll nicht
  gleich eine Kunden-Mail verschicken. Bei allen anderen Spaltenwechseln
  keine Rueckfrage, da folgenlos.
- **`src/styles.css`** — `.dragging` (Karte halbtransparent waehrend des
  Ziehens), `.column-body.drop-target` (gestricheltes Highlight beim
  Drueberziehen), `cursor:grab`/`grabbing` auf ziehbaren Karten.

Auch hier gilt der bekannte Vorbehalt: Ich konnte den Build weiterhin nicht
selbst ausfuehren (npm-Registry in meiner Umgebung geblockt), Code wurde
manuell durchgesehen. Da dein letzter Build aber schon erfolgreich lief,
sollte `docker compose build frontend-service` diesmal einfach funktionieren.

### Nachtrag: intake-service (Intake-Agent)

Neuer Service `intake-service/` (Port 8082) — der erste "Agent" im Sinne des
Projekts. Nimmt den Rohtext einer gefundenen Auftragsanfrage entgegen (z.B.
aus einem Freelance-Marktplatz reinkopiert) und lässt ihn per Anthropic-API
(Claude) in Ticket-Felder strukturieren. **Legt selbst kein Ticket an** —
das Ergebnis füllt im Frontend nur das bestehende "Neues Ticket
erfassen"-Formular vor, geprüft und bestätigt wird weiterhin manuell
("Ticket erstellen"-Klick). Bewusst so gebaut, weil es dafür keine
Marktplatz-API gibt (siehe Projekt-Hintergrund) — der Agent übernimmt nur
das Strukturieren, nicht das Finden.

**`intake-service/`** (neuer Spring-Boot-Service, gleiches Grundgerüst wie
die anderen drei — Java 21, Gradle-Wrapper, Multi-Stage-Dockerfile):

- `client/AnthropicClient.java` — dünner Wrapper um
  `POST https://api.anthropic.com/v1/messages` (Spring `RestClient`, wie
  auch schon `UserServiceClient` in ticket-service/notification-service).
  Modell und Token-Limit über Env-Variablen konfigurierbar
  (`ANTHROPIC_MODEL`, Default `claude-haiku-4-5-20251001` — aktuell
  schnellstes/günstigstes Modell für diese Art Extraktion, siehe
  [Anthropic-Modell-Übersicht](https://platform.claude.com/docs/en/about-claude/models/overview)).
- `service/IntakeService.java` — baut den Extraktions-Prompt, parst die
  Antwort defensiv (auch falls in ```json```-Codeblock verpackt), validiert
  `kind` gegen die drei erlaubten Werte, wirft `IntakeNotConfiguredException`
  (→ HTTP 503) wenn kein API-Key gesetzt ist, `ExtractionFailedException`
  (→ HTTP 502) wenn der Call fehlschlägt oder die Antwort sich nicht
  parsen lässt — beides bewusst kein harter Crash, das Frontend fängt das
  ab und fällt auf manuelle Eingabe zurück.
- `controller/IntakeController.java` — ein Endpunkt:
  `POST /intake/extract`.
- `config/WebConfig.java` — CORS, gleiches Muster wie in
  ticket-service/user-service (`FRONTEND_ORIGIN`).
- Tests: `IntakeServiceTest` (Extraktion inkl. Codeblock-Fall,
  ungültiger `kind`, kaputtes JSON), `IntakeControllerTest`
  (200/400/503/502).

**`docker-compose.yml`** — neuer Service `intake-service`, Port
`8082:8082`, Env `FRONTEND_ORIGIN`, `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL`.
`frontend-service` bekommt zusätzlich `INTAKE_SERVICE_PUBLIC_URL` und hängt
jetzt auch von `intake-service` ab.

**`.env`** — drei neue Zeilen: `ANTHROPIC_API_KEY=` (**musst du selbst
eintragen**, sonst antwortet der Service mit 503 statt zu extrahieren),
`ANTHROPIC_MODEL=claude-haiku-4-5-20251001`,
`INTAKE_SERVICE_PUBLIC_URL=http://localhost:8082`.

**`.github/workflows/ci.yml`** — neuer Job `test-intake-service`, gleiches
Muster wie die anderen drei Java-Services (`./gradlew test`).

**Frontend (`frontend-service/`):**

- `src/api.js` — `extractFromText(rawText)`, ruft
  `POST /intake/extract` auf `INTAKE_BASE` (neue Config-URL, siehe unten).
- `src/components/IntakeForm.jsx` — neuer Block oberhalb des
  Ticket-Formulars: Textfeld zum Einfügen des Anfrage-Texts + Button
  "🤖 Mit KI ausfüllen". Übernimmt nur die vom Agent tatsächlich erkannten
  Felder (überschreibt nichts, was du schon selbst eingetragen hattest),
  erstellt aber kein Ticket automatisch. Schlägt die Extraktion fehl (z.B.
  kein API-Key konfiguriert), erscheint eine Fehlermeldung im Banner, das
  Formular bleibt normal per Hand ausfüllbar.
- `public/config.js` / `config.js.template`, `docker-entrypoint.sh` —
  `INTAKE_SERVICE_URL` ergänzt (gleiches Muster wie die beiden anderen
  Backend-URLs).
- `src/styles.css` — Styles für den neuen `.intake-agent`-Block.

**Ungetestet, wie üblich bei mir:** Ich habe keinen echten
`ANTHROPIC_API_KEY`, konnte den Intake-Agent also nicht end-to-end gegen
die echte API testen — nur Code-Review und die Unit-Tests (die den
API-Call selbst mocken). Bitte `ANTHROPIC_API_KEY` in `.env` eintragen,
`docker compose up --build intake-service frontend-service ticket-service user-service`
laufen lassen und im Dashboard einen Beispieltext durchprobieren.

### Nachtrag: Intake-Agent auf adessos AI Hub umgestellt

Auf Wunsch umgestellt: der Intake-Agent nutzt jetzt nicht mehr die
Anthropic-API direkt, sondern adessos internen AI Hub — eine intern
gehostete, OpenAI-kompatible Chat-Completions-API. Grund: kein eigener
Anthropic-Key nötig, stattdessen der ohnehin vorhandene AI-Hub-Zugang.

- `client/AnthropicClient.java` (+ `AnthropicRequest`/`AnthropicMessage`/
  `AnthropicResponse`) entfernt, ersetzt durch `client/AiHubClient.java`
  (+ `AiHubChatRequest`/`AiHubMessage`/`AiHubChatResponse`). Ruft
  `POST {AI_HUB_BASE_URL}/chat/completions` auf (Spring `RestClient`,
  gleiches Muster wie vorher), Auth per `Authorization: Bearer <Key>`
  statt der Anthropic-eigenen Header.
- Env-Variablen umbenannt: `ANTHROPIC_API_KEY`/`ANTHROPIC_MODEL` →
  `AI_HUB_API_KEY`/`AI_HUB_MODEL`, neu dazu `AI_HUB_BASE_URL` (da der AI
  Hub — anders als die öffentliche Anthropic-API — keine feste,
  vorbekannte URL hat). Default-Modell jetzt `deepseek-v4-flash-sovereign`
  (bewusst ein kleines/günstiges Modell — die Aufgabe ist reines
  JSON-Strukturieren, kein aufwändiges Reasoning).
- `ai-hub.temperature` (Default `0.2`) neu als Property ergänzt — niedrig
  gehalten, weil Strukturieren möglichst deterministisch laufen soll,
  nicht kreativ.
- `docker-compose.yml`, `.env`, `application.properties`,
  `IntakeService`/`IntakeServiceTest` entsprechend angepasst. Die
  `AI_HUB_BASE_URL` (`https://adesso-ai-hub.3asabc.de/v1`) steht schon in
  `.env` — **`AI_HUB_API_KEY` musst du weiterhin selbst eintragen**, sonst
  antwortet der Service mit 503.
- Bewusst *kein* Umstieg auf das offizielle `openai-java`-SDK: der Service
  braucht nur den einen `/chat/completions`-Call, ein Roll-my-own per
  `RestClient` (wie schon bei `UserServiceClient`) vermeidet eine neue,
  von mir hier nicht kompilierbar-testbare Abhängigkeit.

Wie immer ungetestet von mir (kein echter AI-Hub-Zugriff aus meiner
Umgebung, Unit-Tests mocken den Client weiterhin) — bitte `AI_HUB_API_KEY`
eintragen und den bekannten Docker-Compose-Befehl mit einem Beispieltext
durchprobieren.

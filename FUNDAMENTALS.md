# Softwarearchitektur & Patterns – Lernnotizen

Sep 29, 2026 · @Till

Laufende Zusammenfassung unserer Lernreihe zu Softwarearchitektur, Design Patterns und den technischen Grundlagen dahinter ("unter der Haube"). Ziel: Patterns im Code erkennen, verstehen wieso etwas performant/skalierbar ist, und selbst wartbare Software entwickeln können.

## Kapselung

Interner Zustand einer Klasse (Felder) wird versteckt; Zugriff läuft nur über Methoden, die dabei Invarianten durchsetzen.

```java
public class BankAccount {
    private double balance;

    public void withdraw(double amount) {
        if (amount > balance) throw new InsufficientFundsException();
        balance -= amount;
    }
    // kein setBalance() – die Regel "nie negativ" lebt nur hier, an einer Stelle
}
```

Öffentliche Setter für Felder mit Invarianten sind ein Code-Smell ("Getter/Setter-Soup", **Anemic Domain Model**), weil die Business-Regel dann außerhalb der Klasse dupliziert werden müsste. Reine Datenhalter (DTOs, Konfigurationsobjekte) ohne Invarianten dürfen ruhig Setter haben. Ziel ist ein **Rich Domain Model**: Verhalten statt Setter.

**JPA-Beispiel:** Hibernate braucht keine öffentlichen Setter – per *Field Access* (Reflection) kann es direkt private Felder befüllen, solange ein No-Arg-Constructor existiert (darf `protected` sein). Daraus folgt die Schichtentrennung:

**Entity (Persistenz) → Domain-Logik → DTO (API)**, mit einem Mapper dazwischen. Ändert sich die interne DB-Struktur, bleiben Controller/DTO unberührt – das ist die praktische Auszahlung von Kapselung.

## Kopplung & Kohäsion

**Kopplung** existiert immer, sobald Klassen zusammenarbeiten. Ziel ist nicht "keine Kopplung", sondern *lose* Kopplung an stabile Abstraktionen (Interfaces, DTO-Contracts) statt enge Kopplung an Implementierungsdetails (konkrete Klassen, DB-Interna).

**Kohäsion** schaut *innerhalb* einer Klasse: gehören die Methoden/Verantwortlichkeiten inhaltlich zusammen? Ein "God Service", der Kontologik, E-Mails, PDF-Reports und Auth mischt, hat niedrige Kohäsion. Aufgeteilt nach Zuständigkeit hat jede Klasse nur einen Grund, sich zu ändern – das ist zugleich das Single Responsibility Principle.

## SOLID

**S – Single Responsibility:** eine Klasse, ein Grund zur Änderung (siehe Kohäsion oben).

**O – Open/Closed:** offen für Erweiterung, geschlossen für Modifikation. Statt if/else-Ketten über einen Typ nutzt man Interface + Polymorphie:

```java
public interface NotificationService { void send(String recipient, String message); }
// EmailNotificationService, SmsNotificationService implementieren das separat
```

*Unter der Haube:* möglich durch **Dynamic Dispatch** – die JVM schaut zur Laufzeit im tatsächlichen Objekttyp nach, welche Methodenimplementierung ausgeführt wird (Methodentabelle im Metaspace), nicht anhand des deklarierten Typs.

**L – Liskov Substitution:** Subklassen dürfen Vor-/Nachbedingungen der Basisklasse nicht verschärfen/abschwächen (klassisches Gegenbeispiel: `Square extends Rectangle`). Der Compiler prüft das **nicht** – rein semantisch, per Disziplin/Tests sicherzustellen.

**I – Interface Segregation:** lieber mehrere kleine, fokussierte Interfaces als ein fettes mit Methoden, die nicht jede Implementierung sinnvoll unterstützt (sonst leere `UnsupportedOperationException`-Methoden).

**D – Dependency Inversion:** High-Level-Code hängt von Abstraktionen ab, nicht von konkreten Implementierungen. Unterschied zu *Dependency Injection* (der Technik): DIP ist das Design-Prinzip, DI die mechanische Verdrahtung.

*Unter der Haube (Spring):* Classpath-Scanning (Reflection) → `BeanDefinition`-Registry (noch keine Instanzen) → Abhängigkeitsgraph topologisch sortiert → Instanziierung per Reflection (`Constructor.newInstance`) → für `@Transactional`/`@Async` wird statt der echten Klasse ein **Proxy** (CGLIB/JDK Dynamic Proxy) injiziert, der Aufrufe abfängt und zusätzliches Verhalten ausführt, bevor er an die echte Methode weiterreicht. Deshalb greift `@Transactional` nicht bei Selbst-Aufrufen (`this.andereMethode()`) – der Aufruf läuft am Proxy vorbei.

Bonus-Pattern aus der Praxis: `List<NotificationService>` injizieren lässt Spring automatisch **alle** Implementierungen sammeln – neue Implementierung = neue Klasse, aufrufender Code bleibt unverändert (O + D zusammen).

## Runtime & Concurrency

**Thread vs. Heap vs. Stack:** Jeder Thread hat einen eigenen, isolierten **Stack** (lokale Variablen, Methodenaufrufe/Call-Frames). Alle Threads **eines Prozesses** teilen sich **einen** Heap – dort leben alle Objekte (`new Ticket()`, Spring-Beans). Beans bleiben dauerhaft am Leben, weil der `ApplicationContext` sie referenziert (GC-Root); Request-Objekte werden Müll, sobald keine Referenz mehr zeigt. Methoden-Bytecode liegt separat im **Metaspace**, einmal pro Klasse (nicht pro Instanz).

`final` schützt nur die **Referenz** (keine Neuzuweisung), nicht das Objekt selbst – für echte Immutability braucht man keine Setter + `final`-Felder.

**Blocking I/O (klassisch, Spring MVC/Tomcat):** Thread-per-Request – ein Thread ist für die *gesamte* Request-Dauer belegt, inklusive Warten auf die DB. Bei vielen langsamen Requests droht Thread-Pool-Erschöpfung (Standard-Pool ca. 200 Threads).

**Non-Blocking I/O (WebFlux/Netty, Reactor Pattern):** wenige Threads mit einem **Event Loop** (OS-Mechanismus wie `epoll`) bedienen sehr viele gleichzeitige Verbindungen, weil kein Thread in einer Warteschleife hängt. Fallstrick: blockierender Code in einem reaktiven Handler kann die ganze App lahmlegen, weil es nur wenige Event-Loop-Threads gibt.

**Kafka-Bezug:** `KafkaConsumer.poll()` ist selbst blockierend; die Parallelitätseinheit ist die **Partition** – pro Partition genau ein Consumer-Thread innerhalb einer Consumer-Gruppe.

## Container & VM

**Kern (Core)** = Hardware-Recheneinheit auf dem CPU-Chip. **Kernel** = Software, der zentrale Betriebssystem-Teil, der als einziger direkt mit der Hardware spricht. Ein Rechner hat mehrere Kerne, aber (normalerweise) einen Kernel, der sie alle verwaltet.

**VM:** Hypervisor emuliert virtuelle Hardware, darauf läuft ein komplett eigenständiges Gast-Betriebssystem (eigener Kernel) – hoher Overhead, langsamer Start (Minuten).

**Container:** kein eigener Kernel – alle Container teilen sich den Host-Kernel über **Namespaces** (isolierte Sicht: eigenes Netzwerk, Dateisystem, Prozessliste) und **cgroups** (Ressourcenlimits). Deshalb leichtgewichtig: kein OS-Boot nötig, Start in Millisekunden.

**Kubernetes:** ein **Node** ist meist eine VM mit einem Kernel; darauf laufen **viele Pods** (Container), die sich diesen einen Kernel teilen. *Nicht* jeder Pod bekommt eine eigene VM – das wäre der alte, teure Ansatz vor Containern.

**macOS/Windows-Detail:** Docker braucht einen Linux-Kernel; Docker Desktop startet dafür im Hintergrund eine kleine Linux-VM, in der alle Container laufen. Auf echtem Linux fällt dieser Schritt komplett weg.

## Race Conditions & Locking

Entstehen, wenn mehrere Threads/Prozesse ungeschützt denselben Zustand verändern. Klassiker: **Lost Update** (`x = x + 1` ist intern lesen→rechnen→schreiben, drei Schritte, keine atomare Operation).

Innerhalb einer JVM: `synchronized` nutzt den Objekt-Monitor jedes Heap-Objekts – nur einer darf gleichzeitig rein, andere warten. Funktioniert **nicht** zwischen mehreren Pods, weil jeder Pod einen komplett eigenen, getrennten Heap hat (keine gemeinsame Speicherebene, nur die gemeinsame DB verbindet sie).

**Pessimistic Locking** (`SELECT ... FOR UPDATE`): DB sperrt die Zeile, andere Transaktionen warten – wie `synchronized`, nur auf DB-Ebene, damit instanzübergreifend.

**Optimistic Locking** (Versionsspalte): kein Warten – jedes Update prüft `WHERE id=? AND version=?`; verliert eine Transaktion das Rennen, betrifft ihr Update 0 Zeilen → `OptimisticLockException`, kontrolliert erkannt statt still überschrieben. Die DB serialisiert konkurrierende Schreibzugriffe auf dieselbe Zeile intern immer nacheinander (Lock-Manager oder MVCC) – das macht die sichere Auflösung erst möglich.

## Datenbanken

**Indexe:** B-Bäume (breit verzweigt, nicht binär – mehrere Schlüssel pro Knoten, wenige Ebenen = wenige Festplattenzugriffe). Ermöglichen gezielte Lookups statt Full Table Scans. Kosten: jeder Insert/Update/Delete muss den Baum aktualisieren (ggf. Knoten-Splits) → Schreibgeschwindigkeit + Speicherplatz als Trade-off. Faustregel: Spalten indexieren, nach denen häufig gefiltert/sortiert/gejoint wird – nicht wahllos alles.

**ACID:**

- *Atomicity* – Transaktion ganz oder gar nicht.
- *Consistency* – DB erzwingt Constraints (Foreign Keys, Unique, Check) unabhängig von App-Code.
- *Isolation* – vier Levels: Read Uncommitted, Read Committed (Postgres-Default), Repeatable Read (MySQL/InnoDB-Default), Serializable. Verhindern zunehmend: Dirty Read (liest nicht committete Änderung), Non-Repeatable Read (derselbe Wert ändert sich innerhalb einer Transaktion), Phantom Read (neue Zeilen tauchen bei Wiederholung der Abfrage auf). Mehr Isolation = mehr Sicherheit, weniger Parallelität/Performance.
- *Durability* – Write-Ahead Log: Änderung wird erst protokolliert, dann bestätigt; überlebt Absturz nach Commit.

**SQL vs. NoSQL:** relationale DBs (festes Schema, Joins) vs. Document Stores (MongoDB/Elasticsearch – verschachtelte, schemaflexible Dokumente, keine Joins nötig, dafür Datenduplikation). Wahl richtet sich nach Zugriffsmuster: Volltextsuche → Inverted Index (Elasticsearch), stark relationale Transaktionsdaten mit Konsistenzbedarf → SQL. **Polyglot Persistence**: mehrere spezialisierte DBs in einer Architektur kombinieren, statt eine DB für alles zu zwingen. **CAP-Theorem** als Hintergrund: bei einer Netzwerk-Partition muss man zwischen strikter Consistency und Availability wählen – relationale DBs tendieren zu Consistency, viele NoSQL-Systeme zu Availability + Eventual Consistency, was horizontale Skalierung erleichtert.

## CI/CD & Automatisiertes Testing

**CI (Continuous Integration):** jede Code-Änderung wird automatisch gebaut und getestet, sobald sie gepusht wird – typische Pipeline: Checkout → Build (Maven/Gradle) → Unit Tests → statische Analyse (Linting/SonarQube) → Artefakt bauen (Jar/Docker-Image).

**CD – zwei Bedeutungen (häufige Interview-Frage):** *Continuous Delivery* – jede Version, die die Pipeline durchläuft, ist deploybar, ein Mensch triggert den tatsächlichen Produktions-Deploy. *Continuous Deployment* – komplett automatisiert, jede erfolgreiche Version geht ohne manuelles Gate live.

**Testpyramide:** viele schnelle, isolierte **Unit Tests** (mocken Abhängigkeiten) → weniger **Integrationstests** (echte DB via Testcontainers, echte Kafka-Anbindung) → wenige, langsame **End-to-End-Tests** (komplettes System). Trade-off: mehr Realitätsnähe = langsamer/brüchiger, deshalb die Pyramidenform.

**Test Doubles:** *Stub* (liefert feste Antworten), *Mock* (prüft zusätzlich, ob/wie oft eine Methode aufgerufen wurde), *Fake* (funktionierende, aber vereinfachte Implementierung, z.B. In-Memory-Repository). Java/Mockito-Beispiel:

```java
@Test
void withdraw_throwsWhenInsufficientFunds() {
    AccountRepository repo = mock(AccountRepository.class);
    when(repo.findById(1L)).thenReturn(Optional.of(new Account(50)));

    AccountService service = new AccountService(repo);

    assertThrows(InsufficientFundsException.class, () -> service.withdraw(1L, 100));
}
```

**Integrationstests mit Testcontainers:** startet für den Testlauf einen echten Postgres-Container in Docker – testet die tatsächliche SQL-Query/Index-Logik statt nur eines Mocks.

**Contract Testing (Microservices/Kafka):** Consumer-Driven Contracts (z.B. Pact) oder ein Schema Registry (Avro) stellen sicher, dass Producer/Consumer eines Kafka-Topics kompatibel bleiben, ohne einen vollen End-to-End-Testlauf mit allen Services gleichzeitig zu brauchen.

**Pipeline unter der Haube:** ein CI-Runner (ephemere VM/Container) checkt das Repo aus, cached Abhängigkeiten (z.B. `~/.m2`), baut ein Artefakt. Häufig per Multi-Stage-Dockerfile: eine Build-Stage mit vollem JDK+Maven, eine schlanke finale Stage nur mit JRE, die den fertigen Jar kopiert – kleines, produktionsreifes Image, ohne den Build-Toolchain-Ballast.

**Deployment-Strategien:** *Rolling Update* (Pods nach und nach ausgetauscht), *Blue-Green* (komplett neue Version parallel hochziehen, dann Traffic umschalten), *Canary* (neue Version bekommt zuerst nur einen kleinen Traffic-Anteil). **GitOps** (ArgoCD/Flux): ein Tool gleicht den Cluster-Zustand kontinuierlich mit einem Git-Repo ab (pull-basiert), statt dass die Pipeline direkt in den Cluster deployed (push-basiert) – vertiefen wir beim Hosting-Thema.

## Hosting & Deployment

**Die Hosting-Schichten (IaaS → PaaS → CaaS → FaaS):** *IaaS* (rohe VM, z.B. EC2) – du verwaltest OS, Runtime, Skalierung selbst. *PaaS* (z.B. Heroku) – du gibst nur Code, die Plattform kümmert sich um Server/Deploy. *CaaS/Kubernetes* (EKS/GKE/AKS) – du verwaltest Container-Orchestrierung, die Plattform die zugrunde liegenden VMs. *FaaS/Serverless* (Lambda) – du gibst nur eine Funktion, skaliert bis auf null, bezahlt wird pro Aufruf. Mehr Kontrolle = mehr Verantwortung, mehr Abstraktion = weniger Kontrolle, aber weniger Betriebsaufwand.

**Load Balancer:** verteilt eingehende Anfragen auf mehrere Instanzen. *Layer 4* (Transport-Ebene, routet nach IP/Port, kennt HTTP-Inhalt nicht) vs. *Layer 7* (versteht HTTP, kann nach URL-Pfad/Headern routen, z.B. `/api/tickets` → Ticket-Service). Algorithmen: Round Robin, Least Connections, IP Hash (für Session-Stickiness). Erkennt kaputte Instanzen über Health Checks und nimmt sie aus der Rotation.

**Kubernetes Service & Ingress:** ein *Service* ist eine stabile interne DNS-Adresse für eine Gruppe von Pods (Pods selbst kommen und gehen, ihre IPs ändern sich) – der Service lastverteilt intern über alle passenden, gesunden Pods (via `kube-proxy`, iptables/IPVS-Regeln). Ein *Ingress* ist der Eingang von außen in den Cluster – HTTP-Routing (z.B. `api.firma.de/tickets` → Ticket-Service), oft auch TLS-Terminierung.

**Liveness & Readiness Probes:** Kubernetes fragt jeden Pod regelmäßig ab. *Liveness* fehlgeschlagen → Pod wird neugestartet (hängt fest). *Readiness* fehlgeschlagen → Pod bleibt am Leben, bekommt aber temporär keinen Traffic mehr (z.B. während des Hochfahrens oder eines temporären DB-Verbindungsproblems).

**Horizontal Pod Autoscaler (HPA):** skaliert die Pod-Anzahl automatisch basierend auf CPU/Memory oder Custom Metrics (z.B. Kafka-Consumer-Lag) – genau der Mechanismus, der Kubernetes/Container gegenüber "eine VM pro Instanz" so viel praktikabler macht (schneller Start, siehe oben).

**Horizontal vs. Vertical Scaling:** *Vertical* – größere Maschine (mehr CPU/RAM), einfach, aber harte Obergrenze und meist Downtime beim Resize. *Horizontal* – mehr Instanzen (Pods), praktisch unbegrenzt skalierbar, erfordert aber **zustandslose Services**: jede Instanz muss jede Anfrage bedienen können, ohne auf lokalen In-Memory-Zustand einer anderen Instanz angewiesen zu sein (Bezug zu "getrennte Heaps pro Pod" von oben). Session-Daten deshalb nicht im Pod-Heap halten, sondern extern (Redis) oder über Sticky Sessions am Load Balancer.

**Reverse Proxy (z.B. nginx):** sitzt vor den eigentlichen App-Servern, leitet Anfragen weiter, übernimmt oft TLS-Terminierung, Caching, Kompression, Rate Limiting – die App-Server selbst müssen sich darum nicht kümmern.

**API Gateway:** ein zentraler Eintrittspunkt vor mehreren Microservices – übernimmt Auth, Rate Limiting, Routing zum richtigen Service, oft auch Request-Aggregation (mehrere Backend-Calls zu einer Antwort bündeln).

## Noch offen (nächste Themen)

- Architekturstile im Detail: Layered, Hexagonal/Clean Architecture – **als Nächstes**
- Microservice-Patterns: Saga, CQRS, Event-Driven/Kafka-spezifisch
- Klassische GoF Design Patterns mit Codebeispielen
- Danach: Coding gemeinsam üben anhand der Patterns
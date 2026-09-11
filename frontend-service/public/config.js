// Default fuer lokale Entwicklung (npm run dev, ohne Docker). Im
// gebauten Docker-Image wird diese Datei beim Container-Start aus
// config.js.template ueberschrieben (siehe docker-entrypoint.sh).
window.APP_CONFIG = {
  TICKET_SERVICE_URL: "http://localhost:8080",
  USER_SERVICE_URL: "http://localhost:8081"
};

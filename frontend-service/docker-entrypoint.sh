#!/bin/sh
set -eu

# Defaults, falls die Variablen nicht gesetzt sind (z.B. lokaler Start ohne
# docker-compose / .env). So startet der Container nicht kaputt, sondern
# faellt auf sinnvolle lokale URLs zurueck.
: "${TICKET_SERVICE_PUBLIC_URL:=http://localhost:8080}"
: "${USER_SERVICE_PUBLIC_URL:=http://localhost:8081}"
: "${INTAKE_SERVICE_PUBLIC_URL:=http://localhost:8082}"
export TICKET_SERVICE_PUBLIC_URL USER_SERVICE_PUBLIC_URL INTAKE_SERVICE_PUBLIC_URL

envsubst '${TICKET_SERVICE_PUBLIC_URL} ${USER_SERVICE_PUBLIC_URL} ${INTAKE_SERVICE_PUBLIC_URL}' \
  < /usr/share/nginx/html/config.js.template \
  > /usr/share/nginx/html/config.js

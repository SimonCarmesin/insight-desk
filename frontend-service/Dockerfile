# ---- Build-Stage: React/Vite-App zu statischen Dateien bauen ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build

# ---- Serve-Stage: gebaute Dateien per nginx ausliefern ----
FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html

# Wird beim Container-Start automatisch ausgefuehrt (offizielles
# nginx-Feature: alle *.sh unter /docker-entrypoint.d/ laufen vor dem
# eigentlichen nginx-Start). Rendert config.js aus den Env-Variablen,
# damit die Backend-URLs nicht im Image hardcodiert sind.
COPY docker-entrypoint.sh /docker-entrypoint.d/30-render-config.sh
RUN chmod +x /docker-entrypoint.d/30-render-config.sh

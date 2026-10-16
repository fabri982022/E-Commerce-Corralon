# syntax=docker/dockerfile:1
# Supone un proyecto Vite con npm: package-lock.json y salida de "npm run build" en dist/.
# Si el proyecto usa Create React App, cambiar dist por build en el COPY de la etapa final.

# ---------- Etapa 1: compilar ----------
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---------- Etapa 2: servir con nginx ----------
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

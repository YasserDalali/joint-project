# Build from repository root: docker compose build finrisk-web
# Context must be `.` so openapi.yaml is available at ../openapi.yaml during npm run build.
FROM node:22-alpine AS build
WORKDIR /build
COPY openapi.yaml /build/openapi.yaml
WORKDIR /build/finrisk-web
COPY finrisk-web/package*.json ./
RUN npm ci
COPY finrisk-web/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY finrisk-web/nginx.docker.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/finrisk-web/dist /usr/share/nginx/html
EXPOSE 80

# syntax=docker/dockerfile:1
#
# Pokemon frontend — multi-stage image (CLAUDE.md constraint 6).
#   1. build:   type-check and build the static bundle with Node 24 (ADR-009).
#   2. runtime: nginx serves the bundle and proxies /api to the backend, so the browser talks to a single
#               origin (no CORS), exactly like the Vite dev proxy.

# ---- 1. build -------------------------------------------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /workspace

# Playwright is only used by `npm run check:browser` on a developer machine; never download browsers here.
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# The lock file first: `npm ci` is cached until the dependencies change.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---- 2. runtime -----------------------------------------------------------------------------------------
# The unprivileged nginx image runs as a non-root user and listens on 8080.
FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/ || exit 1

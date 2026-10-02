FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

RUN npm run build

FROM nginx:1.27-alpine

# Remove default nginx HTML files
RUN rm -rf /usr/share/nginx/html/*

# Copy built frontend files
COPY --from=build /app/dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Adresse de l API vue depuis le conteneur Nginx (service Compose par defaut).
ENV API_HOST=http://api:8080
# Stockage d'images servi sous /media/ (RustFS de la stack par defaut).
ENV MEDIA_UPSTREAM=http://s3:9000 MEDIA_BUCKET=ecoswitch-media

EXPOSE 80

CMD ["/bin/sh", "-c", "if echo \"${API_HOST:-http://api:8080}\" | grep -q '^https://'; then CLEAN=$(echo \"${API_HOST}\" | sed -e 's|/$||'); TARGET_API=\"${CLEAN}/api/\"; TARGET_UPLOADS=\"${CLEAN}/uploads/\"; elif echo \"${API_HOST:-http://api:8080}\" | grep -q '^http://'; then CLEAN=$(echo \"${API_HOST}\" | sed -e 's|/$||'); TARGET_API=\"${CLEAN}/api/\"; TARGET_UPLOADS=\"${CLEAN}/uploads/\"; else CLEAN=$(echo \"${API_HOST:-http://api:8080}\" | sed -e 's|/$||'); TARGET_API=\"http://${CLEAN}/api/\"; TARGET_UPLOADS=\"http://${CLEAN}/uploads/\"; fi; sed -i \"s|__API_PASS__|${TARGET_API}|g\" /etc/nginx/conf.d/default.conf; sed -i \"s|__UPLOADS_PASS__|${TARGET_UPLOADS}|g\" /etc/nginx/conf.d/default.conf; sed -i \"s|__MEDIA_UPSTREAM__|${MEDIA_UPSTREAM:-http://s3:9000}|g; s|__MEDIA_BUCKET__|${MEDIA_BUCKET:-ecoswitch-media}|g\" /etc/nginx/conf.d/default.conf; nginx -t && nginx -g 'daemon off;'"]

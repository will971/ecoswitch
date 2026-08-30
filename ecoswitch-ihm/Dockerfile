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

ENV API_HOST=https://ecoswitch-api.up.railway.app

EXPOSE 80

CMD ["/bin/sh", "-c", "if echo \"${API_HOST:-https://ecoswitch-api.up.railway.app}\" | grep -q '^https://'; then CLEAN=$(echo \"${API_HOST}\" | sed -e 's|/$||'); TARGET_API=\"${CLEAN}/api/\"; TARGET_UPLOADS=\"${CLEAN}/uploads/\"; elif echo \"${API_HOST:-https://ecoswitch-api.up.railway.app}\" | grep -q '^http://'; then CLEAN=$(echo \"${API_HOST}\" | sed -e 's|/$||'); TARGET_API=\"${CLEAN}/api/\"; TARGET_UPLOADS=\"${CLEAN}/uploads/\"; else CLEAN=$(echo \"${API_HOST:-https://ecoswitch-api.up.railway.app}\" | sed -e 's|/$||'); TARGET_API=\"http://${CLEAN}/api/\"; TARGET_UPLOADS=\"http://${CLEAN}/uploads/\"; fi; sed -i \"s|__API_PASS__|${TARGET_API}|g\" /etc/nginx/conf.d/default.conf; sed -i \"s|__UPLOADS_PASS__|${TARGET_UPLOADS}|g\" /etc/nginx/conf.d/default.conf; nginx -t && nginx -g 'daemon off;'"]

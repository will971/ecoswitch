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

ENV API_HOST=api:8080

EXPOSE 80

CMD ["/bin/sh", "-c", "if echo \"${API_HOST:-api:8080}\" | grep -q '^https://'; then CLEAN=$(echo \"${API_HOST}\" | sed -e 's|/$||'); TARGET_URL=\"${CLEAN}/api/\"; elif echo \"${API_HOST:-api:8080}\" | grep -q '^http://'; then CLEAN=$(echo \"${API_HOST}\" | sed -e 's|/$||'); TARGET_URL=\"${CLEAN}/api/\"; else CLEAN=$(echo \"${API_HOST:-api:8080}\" | sed -e 's|/$||'); TARGET_URL=\"http://${CLEAN}/api/\"; fi; sed -i \"s|__API_PASS__|${TARGET_URL}|g\" /etc/nginx/conf.d/default.conf && nginx -t && nginx -g 'daemon off;'"]

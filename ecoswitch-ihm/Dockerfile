FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

# Remove default nginx HTML files
RUN rm -rf /usr/share/nginx/html/*

# Copy built frontend files
COPY --from=build /app/dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

ENV API_HOST=api:8080

EXPOSE 80

CMD ["/bin/sh", "-c", "CLEAN_API_HOST=$(echo \"${API_HOST:-api:8080}\" | sed -e 's|^https\\?://||' -e 's|/$||'); sed -i \"s|__API_HOST__|${CLEAN_API_HOST}|g\" /etc/nginx/conf.d/default.conf && nginx -t && nginx -g 'daemon off;'"]

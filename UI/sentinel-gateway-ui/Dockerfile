# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Default Nginx config should be fine for basic SPA, but might need adjustment for routing
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

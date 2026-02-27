# Stage 1: Build the React/Vite application
FROM node:20-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy package management files to leverage Docker caching
COPY package.json package-lock.json* ./

# Install dependencies using npm ci for deterministic and fast builds
# (If package-lock.json is out of sync, this might fail, but it's best practice;
# we could use npm install otherwise, but ci is safer for repeatable builds)
RUN npm ci

# Copy the rest of the application files
# Note: The .dockerignore file excludes node_modules from being copied,
# drastically reducing the context size sent to the Docker daemon.
COPY . .

# Build the frontend application
RUN npm run build

# Stage 2: Serve the application using Nginx alpine-slim for a minimal footprint
FROM nginx:alpine-slim

# Copy the built assets from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Custom nginx configuration for Single Page Applications (SPA)
# Required so Vite/React router doesn't return 404 on page refresh or direct links
RUN rm /etc/nginx/conf.d/default.conf && \
    echo 'server {' > /etc/nginx/conf.d/default.conf && \
    echo '    listen 80;' >> /etc/nginx/conf.d/default.conf && \
    echo '    server_name localhost;' >> /etc/nginx/conf.d/default.conf && \
    echo '    root /usr/share/nginx/html;' >> /etc/nginx/conf.d/default.conf && \
    echo '    index index.html;' >> /etc/nginx/conf.d/default.conf && \
    echo '    location / {' >> /etc/nginx/conf.d/default.conf && \
    echo '        try_files $uri $uri/ /index.html;' >> /etc/nginx/conf.d/default.conf && \
    echo '    }' >> /etc/nginx/conf.d/default.conf && \
    echo '}' >> /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Run nginx in the foreground
CMD ["nginx", "-g", "daemon off;"]

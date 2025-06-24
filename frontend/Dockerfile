# Stage 1: Build the Vue.js application
FROM node:20-alpine as build-stage

# Set the working directory inside the container
WORKDIR /app

# Copy package.json and package-lock.json (or yarn.lock) first to leverage Docker cache
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of your frontend code
COPY . .

# Build the Vue.js application for production
RUN npm run build

# Stage 2: Serve the built application using a lightweight web server
FROM nginx:stable-alpine
COPY --from=build-stage /app/dist /usr/share/nginx/html
# --- NEW/UPDATED LINE: Copy your custom Nginx configuration file ---
COPY nginx.conf /etc/nginx/conf.d/default.conf
# --- END NEW/UPDATED LINE ---

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
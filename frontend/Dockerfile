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
FROM nginx:alpine as production-stage

# Copy the built assets from the build-stage to Nginx's web root
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Expose the port Nginx will listen on
EXPOSE 80

# Command to start Nginx when the container runs
CMD ["nginx", "-g", "daemon off;"]
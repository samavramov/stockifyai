import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// 1. Import the 'path' module from Node.js to handle file paths
import path from 'path'

export default defineConfig({
  plugins: [vue()],

  // 2. Add the 'resolve' section to define the '@' alias
  resolve: {
    alias: {
      // This line tells Vite that '@' is a shortcut for the '/src' directory
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    host: '0.0.0.0',
    port: 3000,
    // 3. Add the 'proxy' section for cleaner API calls in development
    proxy: {
      // Any request starting with /api, /auth, /me, or /logout will be forwarded
      '/api': {
        target: 'http://localhost:8001', // Your Java backend
        changeOrigin: true, // Recommended for virtual hosts
      },
      '/auth': {
        target: 'http://localhost:8001',
        changeOrigin: true,
      },
      '/me': {
        target: 'http://localhost:8001',
        changeOrigin: true,
      },
      '/logout': {
        target: 'http://localhost:8001',
        changeOrigin: true,
      }
    },
    hmr: {
      host: 'localhost'
    },
    allowedHosts: [
      'localhost',
      '127.0.0.1'
    ]
  },
  
  preview: {
    host: '0.0.0.0',
    port: 3000
  }
})
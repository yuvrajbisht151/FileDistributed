import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During local development the Vite dev server forwards /api calls to the Spring Boot backend,
// so the browser never has to deal with CORS.
const backend = process.env.BACKEND_URL || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: backend, changeOrigin: true },
    },
  },
});

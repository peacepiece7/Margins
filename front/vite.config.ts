import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

const backendTarget = process.env.MARGINS_BACKEND_URL || 'http://localhost:8080';
const frontendPort = Number(process.env.MARGINS_FRONTEND_PORT || '5173');

export default defineConfig({
  plugins: [react()],
  server: {
    port: frontendPort,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
      },
    },
  },
});

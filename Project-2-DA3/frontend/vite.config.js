import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    port: parseInt(process.env.FRONTEND_PORT || process.env.PORT || '3001', 10),
    proxy: {
      '/api': {
        target: `http://127.0.0.1:${process.env.SERVER_PORT || '8080'}`,
        changeOrigin: true,
      },
      '/actuator': {
        target: `http://127.0.0.1:${process.env.SERVER_PORT || '8080'}`,
        changeOrigin: true,
      },
    },
  },
})


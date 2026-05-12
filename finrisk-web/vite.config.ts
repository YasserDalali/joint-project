import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  // Relative asset URLs so CSS/JS load when the app is served from a subpath,
  // opened from dist/ without a server, or behind a reverse proxy that does
  // not mount `/assets` at the domain root.
  base: "./",
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        // Docker Compose maps the API to host port 18080 (see docker-compose.yml).
        target: "http://localhost:18080",
        changeOrigin: true,
      },
    },
  },
})

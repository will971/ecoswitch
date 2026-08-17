import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import istanbul from 'vite-plugin-istanbul'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    istanbul({
      include: 'src/**/*',
      exclude: ['node_modules', 'test/'],
      extension: ['.js', '.vue'],
      requireEnv: false
    })
  ],
  server: {
    // Vite 8 has two security layers for the dev server:
    // 1. cors: controls Access-Control-Allow-Origin headers (for ES module loading)
    // 2. allowedHosts: validates the HTTP Host header (prevents DNS rebinding attacks)
    // Safari triggers BOTH checks, so both must be explicitly configured for local dev.
    cors: true,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})



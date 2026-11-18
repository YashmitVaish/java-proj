import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Port 3000 because the backend's CORS setting allows http://localhost:3000 by default.
export default defineConfig({
  plugins: [react()],
  server: { port: 3000, strictPort: true },
  preview: { port: 3000, strictPort: true },
})

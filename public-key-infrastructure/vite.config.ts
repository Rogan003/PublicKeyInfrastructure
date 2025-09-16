import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import * as fs from "node:fs";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
    server: {
        https: {
            key: fs.readFileSync('./client.key'),
            cert: fs.readFileSync('./client.crt'),
        },
        port: 3000,
    },
})

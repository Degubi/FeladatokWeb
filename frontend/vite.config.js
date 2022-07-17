import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
    plugins: [
        svelte()
    ],
    server: {
        port: 8081
    },
    build: {
        outDir: '../backend/src/main/resources/public',
        emptyOutDir: true
    }
});
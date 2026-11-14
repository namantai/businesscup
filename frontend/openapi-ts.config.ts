import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: '../docs/openapi.json',
  output: { path: 'src/api/generated' },
  plugins: ['@hey-api/typescript', '@hey-api/sdk', '@hey-api/client-fetch'],
});

import { createClient } from '@hey-api/openapi-ts';
import { readdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import * as prettier from 'prettier';
import configuration from '../openapi-ts.config.ts';
const config = await configuration;

const output = process.env.COMPASS_CLIENT_OUTPUT ?? config.output.path;
await createClient({ ...config, output: { path: output } });
const format = await prettier.resolveConfig(path.resolve('package.json'));
async function formatTree(directory) {
  for (const item of await readdir(directory, { withFileTypes: true })) {
    const file = path.join(directory, item.name);
    if (item.isDirectory()) await formatTree(file);
    else if (item.name.endsWith('.ts'))
      await writeFile(
        file,
        await prettier.format(await readFile(file, 'utf8'), {
          ...format,
          parser: 'typescript',
          filepath: file,
        }),
      );
  }
}
await formatTree(output);

import { cpSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const src = resolve(root, 'src');
const inputDir = resolve(root, '.mp-weixin-src');
const outDir = resolve(root, 'dist-mp/mp-weixin');
const watch = process.argv.includes('--watch');

const copy = (from, to) => {
  cpSync(resolve(src, from), resolve(inputDir, to), { recursive: true, force: true });
};

rmSync(inputDir, { recursive: true, force: true });
mkdirSync(inputDir, { recursive: true });

copy('pages', 'pages');
copy('api', 'api');
copy('store', 'store');
copy('static', 'static');
copy('utils', 'utils');
copy('pages.json', 'pages.json');
copy('manifest.json', 'manifest.json');
copy('App.uni.vue', 'App.vue');
copy('main.uni.js', 'main.js');

const command = process.execPath;
const args = [
  resolve(root, 'node_modules/@dcloudio/vite-plugin-uni/bin/uni.js'),
  'build',
  '-p',
  'mp-weixin',
  '-c',
  'vite.uni.config.js',
  '--outDir',
  outDir
];
if (watch) args.push('--watch');

const result = spawnSync(command, args, {
  cwd: root,
  env: {
    ...process.env,
    UNI_INPUT_DIR: inputDir
  },
  stdio: 'inherit',
  shell: false
});

if (result.error) {
  console.error(result.error);
}

process.exit(result.status ?? 1);

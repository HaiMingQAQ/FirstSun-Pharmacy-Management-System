// Run the locally locked compiler with this existing project as its input root.
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
const result = spawnSync(process.execPath,
  [require.resolve('@dcloudio/vite-plugin-uni/bin/uni.js'), ...process.argv.slice(2)], {
    cwd: root,
    env: { ...process.env, UNI_INPUT_DIR: root },
    stdio: 'inherit',
  });
if (result.error) throw result.error;
process.exit(result.status ?? 1);

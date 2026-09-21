// Configure generated output without rewriting the checked-in manifest.
export default function mpliveMainfestPlugin(isOpen) {
  return {
    name: 'firstsun-mplive-manifest',
    enforce: 'post',
    generateBundle(_, bundle) {
      if (process.env.UNI_PLATFORM !== 'mp-weixin') return;
      const asset = bundle['app.json'];
      if (!asset || asset.type !== 'asset') return;
      const app = JSON.parse(String(asset.source));
      app.plugins = app.plugins || {};
      if (isOpen === '0') delete app.plugins['live-player-plugin'];
      if (isOpen === '1') app.plugins['live-player-plugin'] = {
        version: '1.3.5', provider: 'wx2b03c6e691cd7370',
      };
      asset.source = JSON.stringify(app, null, 2);
    },
  };
}

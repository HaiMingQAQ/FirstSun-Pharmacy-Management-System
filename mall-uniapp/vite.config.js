import { loadEnv } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import path from 'path';
// import viteCompression from 'vite-plugin-compression';
import uniReadPagesV3Plugin from './sheep/router/utils/uni-read-pages-v3';
import mpliveMainfestPlugin from './sheep/libs/mplive-manifest-plugin';


// https://vitejs.dev/config/
export default ({ command, mode }) => {
	const env = loadEnv(mode, __dirname, 'SHOPRO_');
	return {
		envPrefix: "SHOPRO_",
		plugins: [
			uni(),
			// viteCompression({
			// 	verbose: false
			// }),
			uniReadPagesV3Plugin({
				pagesJsonDir: path.resolve(__dirname, './pages.json'),
				includes: ['path', 'aliasPath', 'name', 'meta'],
			}),
			mpliveMainfestPlugin(env.SHOPRO_MPLIVE_ON)
		],
		// 5.07 编译器（uni-app.es.js）从 'vue' 导入 isInSSRComponentSetup，
		// 该导出仅在 uni 平台定制 vue（uni-h5-vue / uni-mp-vue）中提供，官方构建即如此指向。
		resolve: {
			alias: {
				vue:
					process.env.UNI_PLATFORM === 'mp-weixin'
						? '@dcloudio/uni-mp-vue'
						: '@dcloudio/uni-h5-vue',
			},
		},
		server: {
			host: true,
			// open: true,
			port: env.SHOPRO_DEV_PORT,
			hmr: {
				overlay: true,
			},
		},
	};
};

<script setup>
  import { onLaunch, onShow, onError } from '@dcloudio/uni-app';
  import { ShoproInit } from './sheep';
  import { PHARMACY_DEMO } from './sheep/api/pharmacy/config';

  onLaunch((options) => {
    // 隐藏原生导航栏 使用自定义底部导航
    uni.hideTabBar({
      fail: () => {},
    });

    // 加载Shopro底层依赖
    // 药店本地演示不调用商城租户、装修或真实登录接口。
    const pharmacyEntry = !options?.path || options.path.startsWith('pages/pharmacy/');
    if (!(PHARMACY_DEMO && pharmacyEntry)) ShoproInit();
  });

  onShow(() => {
    // #ifdef APP-PLUS
    // 获取urlSchemes参数
    const args = plus.runtime.arguments;
    if (args) {
    }

    // 获取剪贴板
    uni.getClipboardData({
      success: (res) => {},
    });
    // #endif
  });
</script>

<style lang="scss">
  @import '@/sheep/scss/index.scss';
  @import '@/sheep/scss/pharmacy.scss';
</style>

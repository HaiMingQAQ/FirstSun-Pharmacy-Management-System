import { ref } from 'vue';
import api from '@/sheep/api/pharmacy/client';
export const go = (path) => uni.navigateTo({ url: `/pages/pharmacy/${path}` });
export const toast = (title) => uni.showToast({ title, icon: 'none' });
const errorText = (error, fallback) => error?.message || error?.msg || fallback;
export const confirm = (title, content) =>
  new Promise((resolve) =>
    uni.showModal({
      title,
      content,
      confirmColor: '#176b5b',
      success: (r) => resolve(r.confirm),
      fail: () => resolve(false),
    }),
  );
export function requireLogin() {
  if (api.session()) return true;
  go('login');
  return false;
}
export function useRequest() {
  const loading = ref(false),
    error = ref('');
  async function run(fn) {
    if (loading.value) return;
    loading.value = true;
    error.value = '';
    try {
      return await fn();
    } catch (e) {
      error.value = errorText(e, '服务暂时不可用，请重试');
    } finally {
      loading.value = false;
    }
  }
  return { loading, error, run };
}
export function useAction() {
  const busy = ref(false);
  async function act(fn) {
    if (busy.value) return;
    busy.value = true;
    try {
      return await fn();
    } catch (e) {
      toast(errorText(e, '操作失败，请重试'));
    } finally {
      busy.value = false;
    }
  }
  return { busy, act };
}

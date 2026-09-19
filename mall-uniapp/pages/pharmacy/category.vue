<template>
  <s-pharmacy-page :tab="1">
    <view class="fs-pad fs-white">
      <view class="fs-search">
        <uni-icons type="search" size="20" color="#64716c" />
        <input
          v-model="keyword"
          placeholder="药品名称、通用名、条码"
          confirm-type="search"
          @confirm="search"
        />
        <button class="fs-text-btn" @tap="search">搜索</button>
      </view>
      <view v-if="history.length" class="history">
        <text class="fs-muted">最近</text>
        <text
          v-for="word in history.slice(0, 3)"
          :key="word"
          @tap="
            keyword = word;
            search();
          "
        >
          {{ word }}
        </text>
        <button class="fs-text-btn" aria-label="清空搜索历史" @tap="clearHistory">
          <uni-icons type="trash" size="16" color="#758078" />
        </button>
      </view>
    </view>
    <view class="category-layout">
      <view class="category-nav">
        <button
          v-for="cat in api.categories"
          :key="cat.id"
          :class="{ active: category === cat.id }"
          @tap="select(cat.id)"
        >
          {{ cat.name }}
        </button>
      </view>
      <view class="category-results">
        <view class="fs-between result-title">
          <text class="fs-small">{{ keyword ? '搜索结果' : categoryName }}</text>
          <text class="fs-muted">{{ list.length }} 件</text>
        </view>
        <s-pharmacy-state
          v-if="loading || error || !list.length"
          :loading="loading"
          :error="error"
          title="没有找到相关药品"
          description="试试通用名，或切换其他分类"
          action="查看全部"
          @retry="retry"
        />
        <s-pharmacy-product
          v-else
          v-for="product in list"
          :key="product.id"
          :product="product"
          compact
          add
          :busy="busy"
          @add="add"
        />
        <view v-if="list.length && !loading && !error" class="fs-footer">已显示全部药品</view>
      </view>
    </view>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onLoad, onShow } from '@dcloudio/uni-app';
  import api from '@/sheep/api/pharmacy/client';
  import { toast, requireLogin, useRequest, useAction } from './usePharmacy';
  const keyword = ref(''),
    category = ref('all'),
    list = ref([]),
    history = ref(api.history());
  const { loading, error, run } = useRequest();
  const { busy, act } = useAction();
  const categoryName = computed(
    () => api.categories.find((c) => c.id === category.value)?.name || '全部药品',
  );
  const load = () =>
    run(async () => {
      list.value = await api.products({ keyword: keyword.value, category: category.value });
    });
  function search() {
    if (loading.value) return;
    api.remember(keyword.value);
    history.value = api.history();
    category.value = 'all';
    load();
  }
  function select(id) {
    if (loading.value) return;
    category.value = id;
    load();
  }
  function clearHistory() {
    api.clearHistory();
    history.value = [];
  }
  function retry() {
    if (!error.value) {
      keyword.value = '';
      category.value = 'all';
    }
    load();
  }
  const add = (p) => {
    if (requireLogin())
      act(async () => {
        await api.add(p.id);
        toast('已加入购物车');
      });
  };
  onLoad((q) => {
    category.value = q.category || 'all';
  });
  onShow(load);
</script>
<style scoped>
  .history {
    display: flex;
    gap: 12px;
    align-items: center;
    font-size: 12px;
    margin-top: 6px;
  }
  .history > text:not(:first-child) {
    max-width: 76px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .history button {
    margin-left: auto;
  }
  .category-layout {
    display: flex;
    align-items: flex-start;
    min-height: 70vh;
  }
  .category-nav {
    width: 88px;
    flex-shrink: 0;
    position: sticky;
    top: 0;
    padding-top: 8px;
  }
  .category-nav button {
    font-size: 14px;
    min-height: 58px;
    background: transparent;
    border-radius: 0;
    padding: 10px 5px;
    color: #69766e;
  }
  .category-nav button.active {
    background: #fff;
    color: #176b5b;
    font-weight: 600;
    border-left: 3px solid #176b5b;
  }
  .category-results {
    flex: 1;
    min-width: 0;
    background: #fff;
    padding: 0 12px;
    min-height: 70vh;
  }
  .result-title {
    padding-top: 16px;
  }
</style>

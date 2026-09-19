<template>
  <s-pharmacy-page dock>
    <s-pharmacy-state
      v-if="!loggedIn"
      title="登录后管理地址"
      action="去登录"
      @retry="go('login')"
    />
    <template v-else-if="editing">
      <view class="fs-section">
        <view class="fs-title">{{ form.id ? '编辑收货地址' : '新增收货地址' }}</view>
        <view class="fs-field">
          <text class="fs-label">收货人</text>
          <input
            v-model="form.name"
            class="fs-input"
            maxlength="30"
            placeholder="请输入收货人姓名"
          />
        </view>
        <view class="fs-field">
          <text class="fs-label">联系电话</text>
          <input
            v-model="form.mobile"
            type="number"
            maxlength="11"
            class="fs-input"
            placeholder="请输入 11 位手机号"
          />
          <text v-if="form.mobile && !validPhone" class="fs-danger fs-small">手机号格式不正确</text>
        </view>
        <view class="fs-field">
          <text class="fs-label">完整收货地址</text>
          <textarea v-model="form.detail" maxlength="150" placeholder="省、市、区、街道及门牌号" />
        </view>
        <view class="fs-between fs-field">
          <text>设为默认地址</text>
          <switch
            :checked="form.isDefault"
            color="#176b5b"
            @change="form.isDefault = $event.detail.value"
          />
        </view>
      </view>
      <view class="fs-dock">
        <button class="fs-outline" :disabled="busy" @tap="editing = false">取消</button>
        <button
          class="fs-primary fs-grow"
          :disabled="busy || !validPhone || !form.name.trim() || !form.detail.trim()"
          :loading="busy"
          @tap="save"
        >
          保存地址
        </button>
      </view>
    </template>
    <template v-else>
      <s-pharmacy-state
        v-if="loading || error || !list.length"
        :loading="loading"
        :error="error"
        title="还没有收货地址"
        description="添加地址后，即可选择门店配送"
        @retry="load"
      />
      <view v-else v-for="address in list" :key="address.id" class="fs-section">
        <view @tap="choose(address)">
          <view class="fs-title">
            {{ address.name }}
            <text class="fs-small">{{ address.mobile }}</text>
          </view>
          <view class="fs-gap">{{ address.detail }}</view>
          <view v-if="address.isDefault" class="fs-gap"><text class="fs-tag">默认地址</text></view>
          <view v-if="selecting" class="fs-muted fs-gap">点击选择此地址</view>
        </view>
        <view class="fs-between fs-gap">
          <button class="fs-text-btn" @tap="edit(address)">编辑</button>
          <button class="fs-text-btn fs-danger" :disabled="busy" @tap="remove(address)">
            删除
          </button>
        </view>
      </view>
      <view v-if="loggedIn" class="fs-dock">
        <button class="fs-primary fs-grow" @tap="edit()">
          <uni-icons type="plus" size="18" color="#fff" />
          新增收货地址
        </button>
      </view>
    </template>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onLoad, onShow } from '@dcloudio/uni-app';
  import api from '@/sheep/api/pharmacy/client';
  import { go, toast, confirm, useRequest, useAction } from './usePharmacy';
  const list = ref([]),
    editing = ref(false),
    selecting = ref(false),
    loggedIn = ref(false),
    form = ref({ name: '', mobile: '', detail: '', isDefault: false });
  const { loading, error, run } = useRequest();
  const { busy, act } = useAction();
  const validPhone = computed(() => /^1[3-9]\d{9}$/.test(form.value.mobile));
  const load = () =>
    run(async () => {
      list.value = await api.addresses();
    });
  const edit = (address) => {
    form.value = address ? { ...address } : { name: '', mobile: '', detail: '', isDefault: false };
    editing.value = true;
  };
  const choose = (address) => {
    if (selecting.value) {
      api.chooseAddress(address.id);
      uni.navigateBack();
    }
  };
  const save = () =>
    act(async () => {
      const address = await api.saveAddress(form.value);
      toast('地址已保存');
      editing.value = false;
      if (selecting.value) choose(address);
      else await load();
    });
  const remove = (address) =>
    act(async () => {
      if (await confirm('删除地址', '删除后不可恢复，确定删除此收货地址？')) {
        await api.deleteAddress(address.id);
        await load();
        toast('地址已删除');
      }
    });
  onLoad((q) => (selecting.value = q.select === '1'));
  onShow(() => {
    loggedIn.value = !!api.session();
    if (loggedIn.value) load();
    else {
      list.value = [];
      editing.value = false;
    }
  });
</script>

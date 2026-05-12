<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">申请代理</text>
        <text class="brand-sub">升级代理会员并开通对应分润权限</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">代理权益</text>
      <view class="metric">
        <text class="metric-label">适用对象</text>
        <text class="metric-value">店铺会员升级代理</text>
      </view>
      <button class="brand-btn" :loading="submitting" @click="createOrder">创建代理加盟订单</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { joinAPI } from '../../api';

const submitting = ref(false);

const createOrder = async () => {
  submitting.value = true;
  try {
    await joinAPI.createAgentOrder();
    uni.showToast({ title: '订单已创建', icon: 'success' });
  } catch (e) {
    console.error(e);
  } finally {
    submitting.value = false;
  }
};
</script>

<style lang="scss" scoped>
@import "../../static/styles/brand-page.scss";

.brand-btn {
  margin-top: 24rpx;
}
</style>

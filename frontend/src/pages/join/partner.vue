<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">申请合伙人</text>
        <text class="brand-sub">升级合伙人并开通团队管理津贴</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">合伙人权益</text>
      <view class="metric">
        <text class="metric-label">适用对象</text>
        <text class="metric-value">代理会员升级合伙人</text>
      </view>
      <button class="brand-btn" :loading="submitting" @click="createOrder">创建合伙人加盟订单</button>
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
    await joinAPI.createPartnerOrder();
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

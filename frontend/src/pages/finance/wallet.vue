<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">我的钱包</text>
        <text class="brand-sub">账户余额、累计收益与提现状态</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">资产概览</text>
      <view class="metric-grid">
        <view class="metric">
          <text class="metric-label">账户余额</text>
          <text class="metric-value">¥{{ money(wallet.balance) }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">累计收益</text>
          <text class="metric-value">¥{{ money(wallet.totalEarnings) }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">待提现</text>
          <text class="metric-value">¥{{ money(wallet.pendingAmount) }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">已提现</text>
          <text class="metric-value">¥{{ money(wallet.totalWithdrawn) }}</text>
        </view>
      </view>
      <button class="brand-btn" @click="goWithdraw">申请提现</button>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">收益记录</text>
      <view v-if="wallet.recentLogs?.length" class="log-list">
        <view class="log-row" v-for="log in wallet.recentLogs" :key="`${log.orderSn}-${log.type}`">
          <view>
            <text class="log-title">{{ log.remark || log.typeDesc || log.type }}</text>
            <text class="log-time">{{ log.createTime }}</text>
          </view>
          <text class="log-amount">¥{{ money(log.amount) }}</text>
        </view>
      </view>
      <view v-else class="empty-brand">
        <image src="/static/logo.jpg" mode="aspectFill" />
        <text>暂无收益记录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { financeAPI } from '../../api';

const wallet = ref({});
const money = (value) => Number(value || 0).toFixed(2);

const loadWallet = async () => {
  try {
    wallet.value = await financeAPI.getWallet();
  } catch (e) {
    console.error(e);
  }
};

const goWithdraw = () => {
  uni.navigateTo({ url: '/pages/finance/withdraw' });
};

onShow(loadWallet);
</script>

<style lang="scss" scoped>
@import "../../static/styles/brand-page.scss";

.brand-btn {
  margin-top: 24rpx;
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.log-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: #f7f9f3;
}

.log-title,
.log-time {
  display: block;
}

.log-title {
  font-size: 27rpx;
  color: #253322;
  font-weight: 700;
}

.log-time {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8b9484;
}

.log-amount {
  flex-shrink: 0;
  color: #2b6b1f;
  font-weight: 700;
}
</style>

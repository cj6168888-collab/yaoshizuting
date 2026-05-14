<template>
  <view class="dashboard-container">
    <view class="header">
      <view class="brand-strip">
        <image class="brand-logo" src="/static/logo.jpg" mode="aspectFill" />
        <view class="brand-copy">
          <text class="brand-name">药师祖庭</text>
          <text class="brand-subtitle">会员管理工作台</text>
        </view>
      </view>

      <view class="user-info">
        <image class="avatar" :src="userInfo.avatar || '/static/default-avatar.png'" />
        <view class="info">
          <text class="nickname">{{ userInfo.nickname }}</text>
          <text class="role-tag">{{ roleName }}</text>
        </view>
      </view>
    </view>

    <view class="balance-card">
      <view class="balance-heading">
        <image class="balance-logo" src="/static/logo.jpg" mode="aspectFill" />
        <text class="balance-label">可提现余额（元）</text>
      </view>
      <view class="balance-amount">{{ wallet.balance || '0.00' }}</view>
      <view class="balance-actions">
        <button class="action-btn" @click="goToWithdraw">提现</button>
        <button class="action-btn secondary" @click="goToWallet">明细</button>
      </view>
    </view>

    <view class="stats-grid">
      <view class="stat-item">
        <text class="stat-value">{{ wallet.totalEarnings || '0.00' }}</text>
        <text class="stat-label">累计收益</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ teamCount }}</text>
        <text class="stat-label">团队人数</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ storeCount }}</text>
        <text class="stat-label">店铺数量</text>
      </view>
    </view>

    <view class="menu-section">
      <view class="section-head">
        <text class="menu-title">快捷入口</text>
        <view class="section-brand">
          <image class="section-logo" src="/static/logo.jpg" mode="aspectFill" />
          <text>药师祖庭</text>
        </view>
      </view>
      <view class="menu-grid">
        <view class="menu-item" @click="goToScan">
          <view class="menu-icon">扫码</view>
          <text class="menu-text">扫码邀请</text>
        </view>
        <view class="menu-item" @click="goToShop">
          <view class="menu-icon">补货</view>
          <text class="menu-text">产品补货</text>
        </view>
        <view class="menu-item" @click="goToTeam" v-if="isPartner || isAgent">
          <view class="menu-icon">团队</view>
          <text class="menu-text">团队管理</text>
        </view>
        <view class="menu-item" @click="goToUpgrade" v-if="canUpgrade">
          <view class="menu-icon">升级</view>
          <text class="menu-text">升级{{ upgradeTarget }}</text>
        </view>
      </view>
    </view>

    <view class="announcement" v-if="announcements.length > 0">
      <view class="section-head">
        <text class="announcement-title">公告</text>
        <view class="section-brand subtle">
          <image class="section-logo" src="/static/logo.jpg" mode="aspectFill" />
          <text>品牌通知</text>
        </view>
      </view>
      <view class="announcement-list">
        <view class="announcement-item" v-for="item in announcements" :key="item.id">
          <text class="announcement-text">{{ item.title }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '../../store/user';
import { financeAPI } from '../../api';

const userStore = useUserStore();

const wallet = ref({});
const teamCount = ref(0);
const storeCount = ref(0);
const announcements = ref([]);

const userInfo = computed(() => userStore.userInfo || {});
const roleName = computed(() => userStore.roleName);
const isAgent = computed(() => userStore.isAgent);
const isPartner = computed(() => userStore.isPartner);

const canUpgrade = computed(() => userStore.role === 1 || userStore.role === 2);

const upgradeTarget = computed(() => {
  return userStore.role === 1 ? '代理' : '合伙人';
});

const loadWallet = async () => {
  try {
    wallet.value = await financeAPI.getWallet();
  } catch (e) {
    console.error(e);
  }
};

const goToWithdraw = () => {
  uni.navigateTo({ url: '/pages/finance/withdraw' });
};

const goToWallet = () => {
  uni.navigateTo({ url: '/pages/finance/wallet' });
};

const goToScan = () => {
  uni.navigateTo({ url: '/pages/growth/scan' });
};

const goToShop = () => {
  uni.switchTab({ url: '/pages/shop/index' });
};

const goToTeam = () => {
  uni.navigateTo({ url: '/pages/team/list' });
};

const goToUpgrade = () => {
  if (userStore.role === 1) {
    uni.navigateTo({ url: '/pages/join/agent' });
  } else if (userStore.role === 2) {
    uni.navigateTo({ url: '/pages/join/partner' });
  }
};

onShow(() => {
  userStore.loadUserInfo();
  loadWallet();
});
</script>

<style lang="scss" scoped>
.dashboard-container {
  min-height: 100vh;
  background: #f4f6f1;
}

.header {
  background: linear-gradient(160deg, #1d4416 0%, #2e6a21 68%, #3d7d2b 100%);
  padding: 34rpx 30rpx 190rpx;
}

.brand-strip {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 34rpx;
}

.brand-logo {
  width: 92rpx;
  height: 92rpx;
  border-radius: 24rpx;
  box-shadow: 0 14rpx 36rpx rgba(12, 31, 12, 0.24);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.brand-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #f2d77a;
  letter-spacing: 2rpx;
}

.brand-subtitle {
  font-size: 22rpx;
  color: rgba(250, 241, 205, 0.82);
}

.user-info {
  display: flex;
  align-items: center;

  .avatar {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    border: 4rpx solid rgba(255, 255, 255, 0.24);
  }

  .info {
    margin-left: 24rpx;

    .nickname {
      display: block;
      font-size: 32rpx;
      color: #fff;
      font-weight: bold;
    }

    .role-tag {
      display: inline-block;
      margin-top: 8rpx;
      padding: 4rpx 16rpx;
      background: rgba(255, 255, 255, 0.18);
      border-radius: 20rpx;
      font-size: 22rpx;
      color: #fff7d3;
    }
  }
}

.balance-card {
  margin: -136rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 16rpx 42rpx rgba(35, 61, 26, 0.12);

  .balance-heading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 14rpx;
  }

  .balance-logo {
    width: 44rpx;
    height: 44rpx;
    border-radius: 12rpx;
  }

  .balance-label {
    font-size: 26rpx;
    color: #7d836f;
    text-align: center;
  }

  .balance-amount {
    font-size: 64rpx;
    font-weight: bold;
    color: #b89129;
    text-align: center;
    margin: 20rpx 0;
  }

  .balance-actions {
    display: flex;
    justify-content: center;
    gap: 30rpx;

    .action-btn {
      width: 200rpx;
      height: 72rpx;
      line-height: 72rpx;
      background: #2f6a20;
      color: #f4de8f;
      font-size: 28rpx;
      border-radius: 36rpx;
      border: none;

      &.secondary {
        background: #f4f7ef;
        color: #486535;
      }
    }
  }
}

.stats-grid {
  display: flex;
  margin: 0 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx 0;

  .stat-item {
    flex: 1;
    text-align: center;
    border-right: 1rpx solid #edf0e8;

    &:last-child {
      border-right: none;
    }

    .stat-value {
      display: block;
      font-size: 36rpx;
      font-weight: bold;
      color: #253322;
    }

    .stat-label {
      display: block;
      font-size: 24rpx;
      color: #8d9482;
      margin-top: 8rpx;
    }
  }
}

.menu-section,
.announcement {
  margin: 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 26rpx;
}

.menu-title,
.announcement-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #253322;
}

.section-brand {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #eef4e7;
  color: #476833;
  font-size: 22rpx;

  &.subtle {
    background: #f6f7f1;
    color: #7b836f;
  }
}

.section-logo {
  width: 30rpx;
  height: 30rpx;
  border-radius: 8rpx;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;

  .menu-item {
    text-align: center;
  }

  .menu-icon {
    width: 90rpx;
    height: 90rpx;
    line-height: 90rpx;
    background: linear-gradient(180deg, #f3f7ed 0%, #e9f0df 100%);
    color: #355e24;
    border-radius: 24rpx;
    font-size: 24rpx;
    font-weight: 700;
    margin: 0 auto 12rpx;
  }

  .menu-text {
    font-size: 24rpx;
    color: #66705c;
  }
}

.announcement-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f3eb;

  &:last-child {
    border-bottom: none;
  }

  .announcement-text {
    font-size: 26rpx;
    color: #666;
  }
}
</style>

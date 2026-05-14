<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">团队管理</text>
        <text class="brand-sub">查看团队成员与上下级关系</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">团队成员</text>
      <view v-if="team.length" class="team-list">
        <view class="team-row" v-for="member in team" :key="member.userId">
          <image class="member-logo" src="/static/logo.jpg" mode="aspectFill" />
          <view class="member-info">
            <text class="member-name">{{ member.nickname || member.mobile || '团队成员' }}</text>
            <text class="member-mobile">{{ member.mobile }}</text>
          </view>
          <text class="role-pill">{{ roleText(member.role) }}</text>
        </view>
      </view>
      <view v-else class="empty-brand">
        <image src="/static/logo.jpg" mode="aspectFill" />
        <text>暂无团队成员</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { teamAPI } from '../../api';

const team = ref([]);
const roleMap = { 0: '普通会员', 1: '店铺会员', 2: '代理会员', 3: '合伙人', 9: '管理员' };
const roleText = (role) => roleMap[role] || '会员';

const loadTeam = async () => {
  try {
    team.value = await teamAPI.getTree();
  } catch (e) {
    console.error(e);
  }
};

onShow(loadTeam);
</script>

<style lang="scss" scoped>
@import "../../static/styles/brand-page.scss";

.team-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.team-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: #f7f9f3;
}

.member-logo {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name,
.member-mobile {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #253322;
}

.member-mobile {
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #7d8876;
}

.role-pill {
  flex-shrink: 0;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #eef4e7;
  color: #2b6b1f;
  font-size: 22rpx;
}
</style>

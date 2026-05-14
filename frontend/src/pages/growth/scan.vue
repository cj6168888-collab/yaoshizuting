<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">扫码邀请</text>
        <text class="brand-sub">分享邀请链接并锁定上下级关系</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">我的邀请信息</text>
      <view v-if="invite" class="invite-box">
        <image class="invite-logo" src="/static/logo.jpg" mode="aspectFill" />
        <text class="invite-name">{{ invite.parentNickname || invite.parentMobile }}</text>
        <text class="invite-title">扫码加入药师祖庭</text>
        <text class="invite-desc">微信转发给新会员，点击后直接进入注册页并绑定推荐关系</text>
        <text class="invite-url">{{ invite.inviteUrl }}</text>
        <button class="brand-btn" open-type="share">转发邀请海报</button>
        <button class="brand-btn secondary" @click="copyUrl">复制 H5 链接</button>
      </view>
      <view v-else class="empty-brand">
        <image src="/static/logo.jpg" mode="aspectFill" />
        <text>正在获取邀请信息</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">锁定下级</text>
      <view class="field">
        <text class="field-label">新会员手机号</text>
        <input class="field-input" v-model="mobile" type="number" maxlength="11" placeholder="请输入新会员手机号" />
      </view>
      <button class="brand-btn" @click="lockParent">锁定关系</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onShareAppMessage, onShow } from '@dcloudio/uni-app';
import { referralAPI } from '../../api';

const invite = ref(null);
const mobile = ref('');

const sharePath = computed(() => {
  if (!invite.value?.parentId) return '/pages/auth/login';
  const params = new URLSearchParams({
    parentId: String(invite.value.parentId),
    mobile: invite.value.parentMobile || '',
    inviterName: invite.value.parentNickname || invite.value.parentMobile || ''
  });
  return `/pages/auth/login?${params.toString()}`;
});

const loadInvite = async () => {
  try {
    invite.value = await referralAPI.getInviteQr();
  } catch (e) {
    console.error(e);
  }
};

const copyUrl = () => {
  if (!invite.value?.inviteUrl) return;
  uni.setClipboardData({ data: invite.value.inviteUrl });
};

const lockParent = async () => {
  if (!mobile.value || !/^1[3-9]\d{9}$/.test(mobile.value)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
    return;
  }
  try {
    await referralAPI.lockParent({
      mobile: mobile.value,
      parentId: invite.value.parentId
    });
    uni.showToast({ title: '关系已锁定', icon: 'success' });
  } catch (e) {
    console.error(e);
  }
};

onShow(loadInvite);

onShareAppMessage(() => ({
  title: `${invite.value?.parentNickname || '药师祖庭会员'}邀请你加入药师祖庭`,
  path: sharePath.value,
  imageUrl: '/static/share-poster.jpg'
}));
</script>

<style lang="scss" scoped>
@import "../../static/styles/brand-page.scss";

.invite-box {
  text-align: center;
}

.invite-logo {
  width: 116rpx;
  height: 116rpx;
  border-radius: 28rpx;
  margin-bottom: 18rpx;
}

.invite-name {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #253322;
}

.invite-title {
  display: block;
  margin-top: 20rpx;
  color: #2b6b1f;
  font-size: 38rpx;
  font-weight: 800;
}

.invite-desc {
  display: block;
  margin: 14rpx auto 0;
  max-width: 520rpx;
  color: #7d8876;
  font-size: 24rpx;
  line-height: 1.6;
}

.invite-url {
  display: block;
  margin: 18rpx 0;
  padding: 18rpx;
  border-radius: 12rpx;
  background: #f7f9f3;
  color: #7d8876;
  font-size: 22rpx;
  word-break: break-all;
}
</style>

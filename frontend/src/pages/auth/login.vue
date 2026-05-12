<template>
  <view class="login-container">
    <view class="logo-area">
      <image class="brand-logo" src="/static/logo.jpg" mode="aspectFit" />
      <text class="title">药师祖庭</text>
      <text class="subtitle">数字化加盟管理系统</text>
    </view>

    <view class="form-area">
      <view class="input-group">
        <input
          v-model="form.mobile"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
          class="input"
        />
      </view>

      <view class="input-group code-group">
        <input
          v-model="form.code"
          type="number"
          placeholder="请输入验证码"
          maxlength="6"
          class="input"
        />
        <button
          class="code-btn"
          :disabled="countdown > 0"
          @click="sendCode"
        >
          {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
        </button>
      </view>

      <view class="input-group" v-if="showInviteCode">
        <input
          v-model="form.inviteCode"
          type="text"
          placeholder="请输入邀请码（选填）"
          class="input"
        />
      </view>

      <button class="login-btn" @click="handleLogin" :loading="loading">
        {{ loading ? '登录中...' : '立即登录' }}
      </button>

      <view class="tips">
        <text>登录即表示同意</text>
        <text class="link">《用户协议》</text>
        <text>和</text>
        <text class="link">《隐私政策》</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onUnmounted, ref } from 'vue';
import { authAPI } from '../../api';
import { useUserStore } from '../../store/user';

const userStore = useUserStore();

const form = ref({
  mobile: '',
  code: '',
  inviteCode: ''
});

const loading = ref(false);
const countdown = ref(0);
const showInviteCode = ref(true);

let timer = null;

const sendCode = async () => {
  if (!form.value.mobile || form.value.mobile.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
    return;
  }

  try {
    await authAPI.sendCode(form.value.mobile);

    uni.showToast({ title: '验证码已发送', icon: 'success' });
    countdown.value = 60;

    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (e) {
    console.error(e);
  }
};

const handleLogin = async () => {
  if (!form.value.mobile || form.value.mobile.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' });
    return;
  }

  if (!form.value.code || form.value.code.length < 4) {
    uni.showToast({ title: '请输入验证码', icon: 'none' });
    return;
  }

  loading.value = true;

  try {
    await userStore.login(form.value.mobile, form.value.code, form.value.inviteCode);
    uni.switchTab({ url: '/pages/index/index' });
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #1d4416 0%, #2b6b1f 100%);
  padding: 120rpx 60rpx 60rpx;
}

.logo-area {
  text-align: center;
  margin-bottom: 80rpx;

  .brand-logo {
    width: 180rpx;
    height: 180rpx;
    margin: 0 auto 28rpx;
    border-radius: 28rpx;
    box-shadow: 0 16rpx 40rpx rgba(18, 33, 15, 0.24);
    background: rgba(255, 255, 255, 0.12);
  }

  .title {
    display: block;
    font-size: 56rpx;
    font-weight: bold;
    color: #fff4b8;
    letter-spacing: 8rpx;
  }

  .subtitle {
    display: block;
    font-size: 28rpx;
    color: rgba(255, 245, 196, 0.82);
    margin-top: 20rpx;
  }
}

.form-area {
  background: #fff;
  border-radius: 24rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.1);
}

.input-group {
  margin-bottom: 32rpx;

  .input {
    height: 96rpx;
    background: #f5f5f5;
    border-radius: 12rpx;
    padding: 0 30rpx;
    font-size: 28rpx;
  }
}

.code-group {
  display: flex;
  align-items: center;
  gap: 20rpx;

  .input {
    flex: 1;
  }

  .code-btn {
    width: 220rpx;
    height: 96rpx;
    line-height: 96rpx;
    background: #2f6a20;
    color: #f6df8b;
    font-size: 26rpx;
    border-radius: 12rpx;
    padding: 0;

    &[disabled] {
      background: #ccc;
      color: #fff;
    }
  }
}

.login-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #2c6a20 0%, #497f34 100%);
  color: #f6df8b;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 48rpx;
  margin-top: 20rpx;
  border: none;
}

.tips {
  text-align: center;
  font-size: 24rpx;
  color: #999;
  margin-top: 40rpx;

  .link {
    color: #2f6a20;
    margin: 0 4rpx;
  }
}
</style>

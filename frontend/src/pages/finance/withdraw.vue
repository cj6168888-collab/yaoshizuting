<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">提现</text>
        <text class="brand-sub">提交收益提现申请</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">提现信息</text>
      <view class="field">
        <text class="field-label">提现金额</text>
        <input class="field-input" v-model="form.amount" type="digit" placeholder="请输入提现金额" />
      </view>
      <view class="field">
        <text class="field-label">提现方式</text>
        <picker :range="typeNames" @change="changeType">
          <view class="field-input picker-value">{{ typeNames[typeIndex] }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="field-label">收款账号</text>
        <input class="field-input" v-model="form.accountNo" placeholder="请输入收款账号" />
      </view>
      <view class="field">
        <text class="field-label">收款人</text>
        <input class="field-input" v-model="form.accountName" placeholder="请输入收款人姓名" />
      </view>
      <view class="field" v-if="form.withdrawType === 3">
        <text class="field-label">开户行</text>
        <input class="field-input" v-model="form.bankName" placeholder="请输入开户行" />
      </view>
      <button class="brand-btn" :loading="submitting" @click="submit">提交申请</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { withdrawalAPI } from '../../api';

const typeNames = ['微信', '支付宝', '银行卡'];
const typeValues = [1, 2, 3];
const typeIndex = ref(0);
const submitting = ref(false);
const form = ref({
  amount: '',
  withdrawType: 1,
  accountNo: '',
  accountName: '',
  bankName: ''
});

const changeType = (event) => {
  typeIndex.value = Number(event.detail.value);
  form.value.withdrawType = typeValues[typeIndex.value];
};

const submit = async () => {
  if (!form.value.amount || Number(form.value.amount) <= 0) {
    uni.showToast({ title: '请输入提现金额', icon: 'none' });
    return;
  }
  if (!form.value.accountNo || !form.value.accountName) {
    uni.showToast({ title: '请完善收款信息', icon: 'none' });
    return;
  }

  submitting.value = true;
  try {
    await withdrawalAPI.apply({
      ...form.value,
      amount: Number(form.value.amount)
    });
    uni.showToast({ title: '申请已提交', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 600);
  } catch (e) {
    console.error(e);
  } finally {
    submitting.value = false;
  }
};
</script>

<style lang="scss" scoped>
@import "../../static/styles/brand-page.scss";

.picker-value {
  line-height: 88rpx;
}
</style>

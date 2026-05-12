<template>
  <view class="brand-page">
    <view class="brand-hero">
      <image class="brand-logo-sm" src="/static/logo.jpg" mode="aspectFill" />
      <view>
        <text class="brand-title">货品中心</text>
        <text class="brand-sub">药师祖庭补货与产品管理</text>
      </view>
    </view>

    <view class="brand-card">
      <text class="brand-card-title">货品列表</text>
      <view class="type-tabs">
        <button
          v-for="item in typeTabs"
          :key="item.value"
          class="type-tab"
          :class="{ active: activeType === item.value }"
          @click="changeType(item.value)"
        >
          {{ item.label }}
        </button>
      </view>

      <view v-if="products.length" class="product-list">
        <view class="product-card" v-for="product in products" :key="product.id">
          <image class="product-logo" :src="product.image || '/static/logo.jpg'" mode="aspectFill" />
          <view class="product-main">
            <view class="product-head">
              <text class="product-name">{{ product.productName }}</text>
              <text class="product-type">{{ typeName(product.productType) }}</text>
            </view>
            <text class="product-desc">{{ product.description || '药师祖庭品牌货品' }}</text>
            <view class="price-grid">
              <view>
                <text class="price-label">市场价</text>
                <text class="price-value">¥{{ money(product.marketPrice) }}</text>
              </view>
              <view>
                <text class="price-label">加盟价</text>
                <text class="price-value">¥{{ money(product.joinPrice) }}</text>
              </view>
              <view>
                <text class="price-label">库存</text>
                <text class="price-value">{{ product.stock || 0 }}{{ product.unit || '件' }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-brand">
        <image src="/static/logo.jpg" mode="aspectFill" />
        <text>暂无上架货品</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onShow } from 'vue';
import { productAPI } from '../../api';

const products = ref([]);
const activeType = ref(null);
const typeTabs = [
  { label: '全部', value: null },
  { label: '仪器', value: 1 },
  { label: '套盒', value: 2 },
  { label: '单品', value: 3 }
];

const money = (value) => Number(value || 0).toFixed(2);
const typeName = (type) => ({ 1: '仪器', 2: '套盒', 3: '单品' }[type] || '货品');

const loadProducts = async () => {
  try {
    products.value = await productAPI.list(activeType.value);
  } catch (e) {
    console.error(e);
  }
};

const changeType = (type) => {
  activeType.value = type;
  loadProducts();
};

onShow(loadProducts);
</script>

<style lang="scss" scoped>
@import "../../static/styles/brand-page.scss";

.type-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 22rpx;
  overflow-x: auto;
}

.type-tab {
  flex-shrink: 0;
  height: 58rpx;
  line-height: 58rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #eef4e7;
  color: #49683d;
  font-size: 24rpx;
}

.type-tab.active {
  background: #2b6b1f;
  color: #f4da7a;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.product-card {
  display: flex;
  gap: 18rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f7f9f3;
}

.product-logo {
  width: 112rpx;
  height: 112rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}

.product-main {
  flex: 1;
  min-width: 0;
}

.product-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.product-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 28rpx;
  font-weight: 700;
  color: #253322;
}

.product-type {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: #e9f2df;
  color: #2b6b1f;
  font-size: 20rpx;
}

.product-desc {
  display: block;
  margin-top: 8rpx;
  color: #7d8876;
  font-size: 23rpx;
}

.price-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 16rpx;
}

.price-label,
.price-value {
  display: block;
}

.price-label {
  color: #8b9484;
  font-size: 21rpx;
}

.price-value {
  margin-top: 4rpx;
  color: #2b6b1f;
  font-size: 24rpx;
  font-weight: 700;
}
</style>

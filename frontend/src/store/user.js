import { defineStore } from 'pinia';
import { authAPI } from '../api';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: null,
    role: 0,
    balance: 0
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    isStore: (state) => state.role === 1,
    isAgent: (state) => state.role === 2,
    isPartner: (state) => state.role === 3,
    roleName: (state) => {
      const names = { 1: '店主', 2: '代理', 3: '合伙人' };
      return names[state.role] || '游客';
    }
  },
  
  actions: {
    async login(mobile, code, inviteCode) {
      try {
        const data = await authAPI.login({ mobile, code, inviteCode });
        this.token = data.token;
        this.userInfo = {
          userId: data.userId,
          role: data.role,
          nickname: data.nickname,
          mobile: data.mobile,
          avatar: data.avatar,
          parentId: data.parentId,
          treePath: data.treePath
        };
        this.role = data.role;
        uni.setStorageSync('token', data.token);
        uni.setStorageSync('userInfo', this.userInfo);
        return data;
      } catch (e) {
        throw e;
      }
    },
    
    logout() {
      this.token = '';
      this.userInfo = null;
      this.role = 0;
      uni.removeStorageSync('token');
      uni.removeStorageSync('userInfo');
    },
    
    loadUserInfo() {
      const userInfo = uni.getStorageSync('userInfo');
      if (userInfo) {
        this.userInfo = userInfo;
        this.role = userInfo.role;
      }
    }
  }
});

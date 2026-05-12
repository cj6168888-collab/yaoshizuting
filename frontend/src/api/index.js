import { baseURL } from '../utils/config';

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token');
    
    uni.request({
      url: baseURL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        if (res.data.code === 200) {
          resolve(res.data.data);
        } else if (res.data.code === 401) {
          uni.removeStorageSync('token');
          uni.reLaunch({ url: '/pages/auth/login' });
          reject(res.data);
        } else {
          uni.showToast({
            title: res.data.message || '请求失败',
            icon: 'none'
          });
          reject(res.data);
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络请求失败',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
};

export const authAPI = {
  login: (data) => request({ url: '/auth/login', method: 'POST', data }),
  sendCode: (mobile) => request({ url: `/auth/sendCode/${mobile}`, method: 'POST' })
};

export const joinAPI = {
  createStoreOrder: (data) => request({ url: '/join/store', method: 'POST', data }),
  createAgentOrder: () => request({ url: '/join/agent', method: 'POST' }),
  createPartnerOrder: () => request({ url: '/join/partner', method: 'POST' })
};

export const financeAPI = {
  getWallet: () => request({ url: '/finance/wallet', method: 'GET' })
};

export const withdrawalAPI = {
  apply: (data) => request({ url: '/withdrawal/apply', method: 'POST', data }),
  approve: (data) => request({ url: '/withdrawal/approve', method: 'PUT', data }),
  complete: (data) => request({ url: '/withdrawal/complete', method: 'PUT', data })
};

export const teamAPI = {
  getTree: () => request({ url: '/team/tree', method: 'GET' })
};

export const referralAPI = {
  getInviteQr: () => request({ url: '/user/invite-qr', method: 'GET' }),
  bindParent: (parentId) => request({ url: `/user/bind-parent/${parentId}`, method: 'POST' }),
  lockParent: (data) => request({ url: '/user/lock-parent', method: 'POST', data }),
  getLockedParent: (mobile) => request({ url: `/user/get-locked-parent/${mobile}`, method: 'GET' })
};

export const productAPI = {
  list: (productType) => request({
    url: productType ? `/product/list?productType=${productType}` : '/product/list',
    method: 'GET'
  })
};

export const adminAPI = {
  getPolicy: (key) => request({ url: `/admin/policy/${key}`, method: 'GET' }),
  updatePolicy: (data) => request({ url: '/admin/policy', method: 'PUT', data }),
  listProducts: (params = {}) => request({ url: `/admin/product/list?${new URLSearchParams(params)}`, method: 'GET' }),
  createProduct: (data) => request({ url: '/admin/product', method: 'POST', data }),
  updateProduct: (id, data) => request({ url: `/admin/product/${id}`, method: 'PUT', data }),
  updateProductStatus: (id, status) => request({ url: `/admin/product/${id}/status`, method: 'PATCH', data: { status } }),
  listUsers: (params = {}) => request({ url: `/admin/users?${new URLSearchParams(params)}`, method: 'GET' }),
  updateUser: (id, data) => request({ url: `/admin/users/${id}`, method: 'PUT', data }),
  financeSummary: () => request({ url: '/admin/finance/summary', method: 'GET' }),
  listWithdrawals: (params = {}) => request({ url: `/admin/finance/withdrawals?${new URLSearchParams(params)}`, method: 'GET' }),
  listProfitLogs: (params = {}) => request({ url: `/admin/finance/profit-logs?${new URLSearchParams(params)}`, method: 'GET' })
};

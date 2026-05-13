<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import axios from 'axios';
import { baseURL } from './utils/config';
import logoUrl from './static/logo.jpg';

const api = axios.create({ baseURL, timeout: 15000 });

const currentView = ref('login');
const activeMenu = ref('wallet');
const mobile = ref('');
const code = ref('');
const token = ref(localStorage.getItem('token') || '');
const role = ref(parseInt(localStorage.getItem('role') || '0', 10));
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'));
const loading = ref(false);
const countdown = ref(0);
const loginError = ref('');
let codeTimer = null;

const wallet = ref(null);
const team = ref([]);
const publicProducts = ref([]);
const publicProductType = ref('');
const publicProductLoading = ref(false);
const joinLoading = ref('');
const joinMessage = ref('');
const withdrawalSubmitting = ref(false);
const withdrawalMessage = ref('');
const withdrawalForm = ref({
  amount: '',
  withdrawType: 1,
  accountNo: '',
  accountName: '',
  bankName: ''
});
const inviteQrData = ref(null);
const parentId = ref('');
const bindingMobile = ref('');
const bindingLoading = ref(false);
const bindResult = ref(null);

const policies = ref({});
const policySaving = ref(false);
const policyMessage = ref('');

const products = ref([]);
const productTotal = ref(0);
const productLoading = ref(false);
const productSaving = ref(false);
const productUploading = ref(false);
const productMessage = ref('');
const productFilters = ref({ keyword: '', productType: '', status: '' });
const productPage = ref({ page: 1, size: 20 });
const productForm = ref(emptyProductForm());

const adminUsers = ref([]);
const userTotal = ref(0);
const userLoading = ref(false);
const userMessage = ref('');
const userFilters = ref({ keyword: '', role: '', status: '' });
const userPage = ref({ page: 1, size: 20 });

const financeSummary = ref({});
const withdrawals = ref([]);
const profitLogs = ref([]);
const financeLoading = ref(false);
const financeMessage = ref('');
const withdrawalFilters = ref({ status: '', userId: '' });
const profitFilters = ref({ receiverId: '', type: '' });
const withdrawalTotal = ref(0);
const profitTotal = ref(0);
const withdrawalPage = ref({ page: 1, size: 20 });
const profitPage = ref({ page: 1, size: 20 });

const isAdmin = computed(() => Number(role.value) >= 9);
const canJoinStore = computed(() => Number(role.value) < 1);
const canJoinAgent = computed(() => Number(role.value) >= 1 && Number(role.value) < 2);
const canJoinPartner = computed(() => Number(role.value) >= 2 && Number(role.value) < 3);

const roleNameMap = {
  0: '普通会员',
  1: '店铺会员',
  2: '代理会员',
  3: '合伙人',
  9: '管理员',
  10: '超级管理员'
};

const accountStatusMap = {
  0: '禁用',
  1: '正常'
};

const productTypeMap = {
  1: '仪器',
  2: '套盒',
  3: '单品'
};

const productStatusMap = {
  0: '下架',
  1: '上架'
};

const withdrawalStatusMap = {
  0: '待审核',
  1: '已审核',
  2: '已打款',
  3: '已拒绝'
};

const policyGroups = [
  {
    title: '加盟费用',
    items: [
      { key: 'STORE_JOIN_FEE', label: '店铺加盟费', description: '店铺会员加盟费用' },
      { key: 'AGENT_JOIN_FEE', label: '代理商加盟费', description: '代理商加盟费用' },
      { key: 'PARTNER_JOIN_FEE', label: '合伙人加盟费', description: '合伙人加盟费用' }
    ]
  },
  {
    title: '分润政策',
    items: [
      { key: 'STORE_REWARD_DIRECT', label: '店主直推店铺奖励', description: '店主直接推荐店铺奖励' },
      { key: 'AGENT_REWARD_DIRECT', label: '代理直推店铺奖励', description: '代理直接推荐店铺奖励' },
      { key: 'PARTNER_REWARD_DIRECT', label: '合伙人直推店铺奖励', description: '合伙人直接推荐店铺奖励' },
      { key: 'STORE_DIRECT_REWARD_START_COUNT', label: '直推起奖店铺数', description: '直推第几家店铺开始奖励', prefix: '', suffix: '家', step: 1 },
      { key: 'STORE_INDIRECT_REWARD_ENABLED', label: '启用间推奖励', description: '是否启用间推店铺奖励：0否，1是', prefix: '', suffix: '0/1', step: 1 },
      { key: 'REWARD_INDIRECT', label: '间推奖励金额', description: '间接推荐店铺奖励金额' },
      { key: 'AGENT_REWARD_DIRECT_AGENT', label: '直推代理奖励', description: '店主或代理直接推荐代理奖励' },
      { key: 'PARTNER_MANAGE_FEE', label: '代理管理培训费', description: '合伙人招募代理的管理培训费' },
      { key: 'HEADQUARTER_SUPPORT_FEE', label: '总部培训支持费', description: '合伙人第11名起的总部培训支持费' },
      { key: 'PARTNER_TEAM_MANAGEMENT', label: '团队管理津贴', description: '合伙人团队管理津贴' },
      { key: 'PARTNER_TEAM_MANAGEMENT_START_COUNT', label: '管理津贴起始数', description: '团队第几家店铺开始发放管理津贴', prefix: '', suffix: '家', step: 1 },
      { key: 'PARTNER_TEAM_MANAGEMENT_END_COUNT', label: '管理津贴结束数', description: '团队管理津贴发放到第几家店铺', prefix: '', suffix: '家', step: 1 }
    ]
  },
  {
    title: '补货与提现',
    items: [
      { key: 'PRODUCT_DISCOUNT', label: '进货折扣', description: '店铺补货折扣' },
      { key: 'CLOUD_WAREHOUSE_FEE', label: '云仓代发服务费', description: '0元进货模式服务费' },
      { key: 'WITHDRAWAL_FEE_RATE', label: '提现手续费率', description: '提现手续费比例' },
      { key: 'WITHDRAWAL_MIN_AMOUNT', label: '最低提现金额', description: '用户可提交提现的最低金额' }
    ]
  }
];

const policyWarnings = computed(() => evaluatePolicyWarnings(policies.value));

function emptyProductForm() {
  return {
    id: null,
    productName: '',
    productCode: '',
    productType: 1,
    marketPrice: 0,
    joinPrice: 0,
    agentPrice: 0,
    partnerPrice: 0,
    stock: 0,
    unit: '套',
    image: '',
    description: '',
    status: 1
  };
}

function authHeaders() {
  return token.value ? { Authorization: `Bearer ${token.value}` } : {};
}

function unwrap(response) {
  const body = response.data;
  if (body?.code && body.code !== 200) {
    throw new Error(body.message || '请求失败');
  }
  return body?.data ?? body;
}

function errorMessage(error, fallback = '操作失败') {
  const serverMessage = error.response?.data?.message || error.response?.data?.error;
  if (serverMessage) return `${fallback}：${serverMessage}`;

  if (error.code === 'ECONNABORTED') {
    return `${fallback}：请求超时，请稍后重试`;
  }

  if (error.message === 'Network Error') {
    return `${fallback}：网络连接异常，请检查后端服务或本机网络`;
  }

  const status = error.response?.status;
  if (status === 401) return `${fallback}：登录已过期，请重新登录`;
  if (status === 403) return `${fallback}：当前账号无权执行此操作`;
  if (status === 404) return `${fallback}：接口不存在或资源已被删除`;
  if (status >= 500) return `${fallback}：服务器暂时不可用`;

  return error.message ? `${fallback}：${error.message}` : fallback;
}

function isFailureMessage(message) {
  return /失败|错误|异常|超时|无权|过期|不可用|不存在/.test(message || '');
}

function formatMoney(amount) {
  return parseFloat(amount || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

function imageUrl(path) {
  if (!path) return logoUrl;
  if (/^https?:\/\//.test(path)) return path;
  if (path.startsWith('/uploads')) return `${baseURL}${path}`;
  return path;
}

function cleanParams(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function totalPages(total, pageState) {
  return Math.max(1, Math.ceil(Number(total || 0) / Number(pageState.size || 20)));
}

function canPrev(pageState) {
  return Number(pageState.page) > 1;
}

function canNext(total, pageState) {
  return Number(pageState.page) < totalPages(total, pageState);
}

function clearCodeTimer() {
  if (!codeTimer) return;
  clearInterval(codeTimer);
  codeTimer = null;
}

async function downloadFile(url, params, filename) {
  const response = await api.get(url, {
    headers: authHeaders(),
    params: cleanParams(params),
    responseType: 'blob'
  });
  const blob = new Blob([response.data], { type: response.headers['content-type'] || 'text/csv;charset=utf-8;' });
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(objectUrl);
}

function setPage(pageState, page, total, loader) {
  const target = Math.min(Math.max(1, page), totalPages(total, pageState));
  if (target === pageState.page) return;
  pageState.page = target;
  loader();
}

async function sendCode() {
  if (!mobile.value || !/^1[3-9]\d{9}$/.test(mobile.value)) {
    loginError.value = '请输入正确的手机号';
    return;
  }

  try {
    await api.post(`/auth/sendCode/${mobile.value}`);
    clearCodeTimer();
    countdown.value = 60;
    codeTimer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) {
        countdown.value = 0;
        clearCodeTimer();
      }
    }, 1000);
    loginError.value = '';
  } catch (error) {
    loginError.value = errorMessage(error, '发送失败');
  }
}

async function login() {
  if (!mobile.value) {
    loginError.value = '请输入手机号';
    return;
  }
  if (!code.value) {
    loginError.value = '请输入验证码';
    return;
  }

  loading.value = true;
  try {
    const data = unwrap(await api.post('/auth/login', { mobile: mobile.value, code: code.value }));
    token.value = data.token;
    role.value = data.role;
    user.value = data;
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', data.role);
    localStorage.setItem('user', JSON.stringify(data));
    currentView.value = 'dashboard';
    activeMenu.value = 'wallet';
    loginError.value = '';
    await fetchData();
  } catch (error) {
    loginError.value = errorMessage(error, '登录失败');
  } finally {
    loading.value = false;
  }
}

async function fetchData() {
  if (!token.value) return;

  await Promise.allSettled([loadWallet(), loadTeam()]);
  if (isAdmin.value) {
    await Promise.allSettled([loadPolicies(), loadProducts(), loadAdminUsers(), loadFinanceAdmin()]);
  }
}

async function loadWallet() {
  const data = unwrap(await api.get('/finance/wallet', { headers: authHeaders() }));
  wallet.value = data;
}

async function loadTeam() {
  const data = unwrap(await api.get('/team/tree', { headers: authHeaders() }));
  team.value = data || [];
}

async function loadPublicProducts() {
  publicProductLoading.value = true;
  try {
    const params = publicProductType.value ? { productType: publicProductType.value } : {};
    publicProducts.value = unwrap(await api.get('/product/list', { headers: authHeaders(), params })) || [];
  } catch (error) {
    productMessage.value = errorMessage(error, '货品列表加载失败');
  } finally {
    publicProductLoading.value = false;
  }
}

async function createJoinOrder(type) {
  const endpoints = {
    store: { url: '/join/store', payload: { payMethod: 1 }, label: '店铺加盟订单' },
    agent: { url: '/join/agent', payload: {}, label: '代理加盟订单' },
    partner: { url: '/join/partner', payload: {}, label: '合伙人加盟订单' }
  };
  const target = endpoints[type];
  if (!target) return;

  joinLoading.value = type;
  joinMessage.value = '';
  try {
    const data = unwrap(await api.post(target.url, target.payload, { headers: authHeaders() }));
    joinMessage.value = `${target.label}已创建，订单号 ${data.orderSn}，金额 ¥${formatMoney(data.amount)}`;
  } catch (error) {
    joinMessage.value = errorMessage(error, `${target.label}创建失败`);
  } finally {
    joinLoading.value = '';
  }
}

async function submitWithdrawal() {
  if (!withdrawalForm.value.amount || Number(withdrawalForm.value.amount) <= 0) {
    withdrawalMessage.value = '请输入有效提现金额';
    return;
  }
  if (!withdrawalForm.value.accountNo || !withdrawalForm.value.accountName) {
    withdrawalMessage.value = '请完善收款信息';
    return;
  }
  if (Number(withdrawalForm.value.withdrawType) === 3 && !withdrawalForm.value.bankName) {
    withdrawalMessage.value = '银行卡提现需填写开户行';
    return;
  }

  withdrawalSubmitting.value = true;
  withdrawalMessage.value = '';
  try {
    const payload = {
      ...withdrawalForm.value,
      amount: Number(withdrawalForm.value.amount),
      withdrawType: Number(withdrawalForm.value.withdrawType)
    };
    const data = unwrap(await api.post('/withdrawal/apply', payload, { headers: authHeaders() }));
    withdrawalMessage.value = `提现申请已提交，单号 ${data.withdrawSn}，到账 ¥${formatMoney(data.actualAmount)}`;
    withdrawalForm.value.amount = '';
    await loadWallet();
  } catch (error) {
    withdrawalMessage.value = errorMessage(error, '提现申请失败');
  } finally {
    withdrawalSubmitting.value = false;
  }
}

async function loadPolicies() {
  const keys = policyGroups.flatMap((group) => group.items.map((item) => item.key));
  const entries = await Promise.all(
    keys.map(async (key) => {
      const data = unwrap(await api.get(`/admin/policy/${key}`, { headers: authHeaders() }));
      return [key, data.configValue];
    })
  );
  policies.value = Object.fromEntries(entries);
}

async function savePolicy(item) {
  const value = policies.value[item.key];
  if (value === '' || value === null || value === undefined || Number(value) < 0) {
    policyMessage.value = '请输入有效的配置值';
    return;
  }

  policySaving.value = true;
  policyMessage.value = '';
  try {
    const data = unwrap(await api.put(
      '/admin/policy',
      { configKey: item.key, configValue: value, description: item.description },
      { headers: authHeaders() }
    ));
    policyMessage.value = data?.warnings?.length
      ? `${item.label} 已更新，存在 ${data.warnings.length} 条风险提示`
      : `${item.label} 已更新`;
  } catch (error) {
    policyMessage.value = errorMessage(error, '配置更新失败');
  } finally {
    policySaving.value = false;
  }
}

function policyNumber(values, key, fallback = 0) {
  const value = Number(values[key]);
  return Number.isFinite(value) ? value : fallback;
}

function evaluatePolicyWarnings(values) {
  const warnings = [];
  const storeJoinFee = policyNumber(values, 'STORE_JOIN_FEE', 13960);
  const maxDirectStoreReward = Math.max(
    policyNumber(values, 'STORE_REWARD_DIRECT', 9000),
    policyNumber(values, 'AGENT_REWARD_DIRECT', 9000),
    policyNumber(values, 'PARTNER_REWARD_DIRECT', 9000)
  );
  const indirectReward = policyNumber(values, 'STORE_INDIRECT_REWARD_ENABLED', 0) > 0
    ? policyNumber(values, 'REWARD_INDIRECT', 0)
    : 0;
  const teamManagement = policyNumber(values, 'PARTNER_TEAM_MANAGEMENT', 998);
  const storeRewardTotal = maxDirectStoreReward + indirectReward + teamManagement;
  if (storeRewardTotal > storeJoinFee) {
    warnings.push(`店铺加盟单笔最高分润 ¥${formatMoney(storeRewardTotal)} 已超过店铺加盟费 ¥${formatMoney(storeJoinFee)}`);
  }

  const directStart = policyNumber(values, 'STORE_DIRECT_REWARD_START_COUNT', 2);
  if (directStart < 1) {
    warnings.push('直推起奖店铺数小于 1，会导致奖励门槛无效');
  }

  const teamStart = policyNumber(values, 'PARTNER_TEAM_MANAGEMENT_START_COUNT', 2);
  const teamEnd = policyNumber(values, 'PARTNER_TEAM_MANAGEMENT_END_COUNT', 100);
  if (teamStart < 1 || teamEnd < teamStart) {
    warnings.push('团队管理津贴起止店铺数不合理，请确保起始值大于等于 1 且不超过结束值');
  }

  const agentJoinFee = policyNumber(values, 'AGENT_JOIN_FEE', 39800);
  const agentReward = Math.max(
    policyNumber(values, 'AGENT_REWARD_DIRECT_AGENT', 16000),
    policyNumber(values, 'PARTNER_MANAGE_FEE', 39800)
  );
  if (agentReward > agentJoinFee) {
    warnings.push(`代理加盟单笔最高分润 ¥${formatMoney(agentReward)} 已超过代理加盟费 ¥${formatMoney(agentJoinFee)}`);
  }

  const partnerJoinFee = policyNumber(values, 'PARTNER_JOIN_FEE', 99800);
  const partnerReward = policyNumber(values, 'PARTNER_REWARD_DIRECT_PARTNER', 40000);
  if (partnerReward > partnerJoinFee) {
    warnings.push(`合伙人加盟直推奖励 ¥${formatMoney(partnerReward)} 已超过合伙人加盟费 ¥${formatMoney(partnerJoinFee)}`);
  }

  return warnings;
}

async function loadInviteQr() {
  try {
    inviteQrData.value = unwrap(await api.get('/user/invite-qr', { headers: authHeaders() }));
  } catch (error) {
    bindResult.value = { error: errorMessage(error, '获取邀请信息失败') };
  }
}

async function bindParent() {
  if (!parentId.value) {
    bindResult.value = { error: '请输入上级用户 ID' };
    return;
  }

  bindingLoading.value = true;
  try {
    const response = await api.post(`/user/bind-parent/${parentId.value}`, {}, { headers: authHeaders() });
    bindResult.value = response.data;
    await loadTeam();
  } catch (error) {
    bindResult.value = { error: errorMessage(error, '绑定失败') };
  } finally {
    bindingLoading.value = false;
  }
}

async function lockParentBind() {
  if (!bindingMobile.value || !/^1[3-9]\d{9}$/.test(bindingMobile.value)) {
    bindResult.value = { error: '请输入正确的手机号' };
    return;
  }

  bindingLoading.value = true;
  try {
    const response = await api.post(
      '/user/lock-parent',
      { mobile: bindingMobile.value, parentId: parseInt(parentId.value, 10) },
      { headers: authHeaders() }
    );
    bindResult.value = response.data;
  } catch (error) {
    bindResult.value = { error: errorMessage(error, '锁定失败') };
  } finally {
    bindingLoading.value = false;
  }
}

async function loadProducts() {
  if (!isAdmin.value) return;
  productLoading.value = true;
  try {
    const data = unwrap(
      await api.get('/admin/product/list', {
        headers: authHeaders(),
        params: { ...productPage.value, ...cleanParams(productFilters.value) }
      })
    );
    products.value = data.records || [];
    productTotal.value = data.total || 0;
    productPage.value.page = Number(data.page || productPage.value.page);
    productPage.value.size = Number(data.size || productPage.value.size);
  } catch (error) {
    productMessage.value = errorMessage(error, '商品列表加载失败');
  } finally {
    productLoading.value = false;
  }
}

function searchProducts() {
  productPage.value.page = 1;
  loadProducts();
}

function editProduct(product) {
  productForm.value = {
    id: product.id,
    productName: product.productName || '',
    productCode: product.productCode || '',
    productType: product.productType || 1,
    marketPrice: product.marketPrice ?? 0,
    joinPrice: product.joinPrice ?? 0,
    agentPrice: product.agentPrice ?? 0,
    partnerPrice: product.partnerPrice ?? 0,
    stock: product.stock ?? 0,
    unit: product.unit || '套',
    image: product.image || '',
    description: product.description || '',
    status: product.status ?? 1
  };
  productMessage.value = '';
}

function resetProductForm() {
  productForm.value = emptyProductForm();
  productMessage.value = '';
}

async function uploadProductImage(event) {
  const file = event.target.files?.[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);
  productUploading.value = true;
  productMessage.value = '';
  try {
    const data = unwrap(await api.post('/admin/product/upload', formData, { headers: authHeaders() }));
    productForm.value.image = data.url;
    productMessage.value = '商品图片已上传';
  } catch (error) {
    productMessage.value = errorMessage(error, '图片上传失败');
  } finally {
    productUploading.value = false;
    event.target.value = '';
  }
}

async function saveProduct() {
  if (!productForm.value.productName.trim()) {
    productMessage.value = '请输入商品名称';
    return;
  }

  const payload = {
    ...productForm.value,
    marketPrice: Number(productForm.value.marketPrice || 0),
    joinPrice: Number(productForm.value.joinPrice || 0),
    agentPrice: Number(productForm.value.agentPrice || 0),
    partnerPrice: Number(productForm.value.partnerPrice || 0),
    stock: Number(productForm.value.stock || 0),
    productType: Number(productForm.value.productType),
    status: Number(productForm.value.status)
  };
  delete payload.id;

  productSaving.value = true;
  productMessage.value = '';
  try {
    if (productForm.value.id) {
      await api.put(`/admin/product/${productForm.value.id}`, payload, { headers: authHeaders() });
      productMessage.value = '商品已更新';
    } else {
      await api.post('/admin/product', payload, { headers: authHeaders() });
      productMessage.value = '商品已创建';
    }
    resetProductForm();
    await loadProducts();
  } catch (error) {
    productMessage.value = errorMessage(error, '商品保存失败');
  } finally {
    productSaving.value = false;
  }
}

async function toggleProductStatus(product) {
  productMessage.value = '';
  try {
    await api.patch(
      `/admin/product/${product.id}/status`,
      { status: product.status === 1 ? 0 : 1 },
      { headers: authHeaders() }
    );
    await loadProducts();
  } catch (error) {
    productMessage.value = errorMessage(error, '商品状态更新失败');
  }
}

async function loadAdminUsers() {
  if (!isAdmin.value) return;
  userLoading.value = true;
  try {
    const data = unwrap(
      await api.get('/admin/users', {
        headers: authHeaders(),
        params: { ...userPage.value, ...cleanParams(userFilters.value) }
      })
    );
    adminUsers.value = data.records || [];
    userTotal.value = data.total || 0;
    userPage.value.page = Number(data.page || userPage.value.page);
    userPage.value.size = Number(data.size || userPage.value.size);
  } catch (error) {
    userMessage.value = errorMessage(error, '人员列表加载失败');
  } finally {
    userLoading.value = false;
  }
}

function searchAdminUsers() {
  userPage.value.page = 1;
  loadAdminUsers();
}

async function updateUser(userItem, patch) {
  userMessage.value = '';
  try {
    const data = unwrap(await api.put(`/admin/users/${userItem.id}`, patch, { headers: authHeaders() }));
    Object.assign(userItem, data);
    userMessage.value = '人员信息已更新';
  } catch (error) {
    userMessage.value = errorMessage(error, '人员信息更新失败');
    await loadAdminUsers();
  }
}

async function loadFinanceAdmin() {
  if (!isAdmin.value) return;
  financeLoading.value = true;
  try {
    const [summary, withdrawalResponse, profitResponse] = await Promise.all([
      api.get('/admin/finance/summary', { headers: authHeaders() }),
      api.get('/admin/finance/withdrawals', {
        headers: authHeaders(),
        params: { ...withdrawalPage.value, ...cleanParams(withdrawalFilters.value) }
      }),
      api.get('/admin/finance/profit-logs', {
        headers: authHeaders(),
        params: { ...profitPage.value, ...cleanParams(profitFilters.value) }
      })
    ]);
    financeSummary.value = unwrap(summary) || {};
    const withdrawalData = unwrap(withdrawalResponse);
    const profitData = unwrap(profitResponse);
    withdrawals.value = withdrawalData.records || [];
    withdrawalTotal.value = withdrawalData.total || 0;
    withdrawalPage.value.page = Number(withdrawalData.page || withdrawalPage.value.page);
    withdrawalPage.value.size = Number(withdrawalData.size || withdrawalPage.value.size);
    profitLogs.value = profitData.records || [];
    profitTotal.value = profitData.total || 0;
    profitPage.value.page = Number(profitData.page || profitPage.value.page);
    profitPage.value.size = Number(profitData.size || profitPage.value.size);
  } catch (error) {
    financeMessage.value = errorMessage(error, '财务数据加载失败');
  } finally {
    financeLoading.value = false;
  }
}

function searchWithdrawals() {
  withdrawalPage.value.page = 1;
  loadFinanceAdmin();
}

function searchProfitLogs() {
  profitPage.value.page = 1;
  loadFinanceAdmin();
}

function exportProducts() {
  downloadFile('/admin/product/export', productFilters.value, 'products.csv');
}

function exportUsers() {
  downloadFile('/admin/users/export', userFilters.value, 'users.csv');
}

function exportWithdrawals() {
  downloadFile('/admin/finance/withdrawals/export', withdrawalFilters.value, 'withdrawals.csv');
}

function exportProfitLogs() {
  downloadFile('/admin/finance/profit-logs/export', profitFilters.value, 'profit-logs.csv');
}

async function approveWithdrawal(item, approved) {
  const remark = approved ? '审核通过' : window.prompt('请输入拒绝原因', '资料不完整');
  if (remark === null) return;

  financeMessage.value = '';
  try {
    await api.put(
      '/withdrawal/approve',
      { withdrawalId: item.id, approved, remark },
      { headers: authHeaders() }
    );
    financeMessage.value = approved ? '提现已审核通过' : '提现已拒绝';
    await loadFinanceAdmin();
  } catch (error) {
    financeMessage.value = errorMessage(error, '提现审核失败');
  }
}

async function completeWithdrawal(item) {
  const transactionId = window.prompt('请输入打款流水号', item.withdrawSn || '');
  if (!transactionId) return;

  financeMessage.value = '';
  try {
    await api.put(
      '/withdrawal/complete',
      { withdrawalId: item.id, transactionId },
      { headers: authHeaders() }
    );
    financeMessage.value = '提现已标记为打款完成';
    await loadFinanceAdmin();
  } catch (error) {
    financeMessage.value = errorMessage(error, '打款确认失败');
  }
}

function switchMenu(menu) {
  activeMenu.value = menu;
  if (menu === 'shop') loadPublicProducts();
  if (menu === 'invite') loadInviteQr();
  if (menu === 'products') loadProducts();
  if (menu === 'users') loadAdminUsers();
  if (menu === 'financeAdmin') loadFinanceAdmin();
  if (menu === 'admin') loadPolicies();
}

function logout() {
  clearCodeTimer();
  token.value = '';
  role.value = 0;
  user.value = {};
  wallet.value = null;
  team.value = [];
  publicProducts.value = [];
  policies.value = {};
  inviteQrData.value = null;
  joinMessage.value = '';
  withdrawalMessage.value = '';
  localStorage.clear();
  currentView.value = 'login';
  mobile.value = '';
  code.value = '';
  countdown.value = 0;
  loginError.value = '';
  bindResult.value = null;
}

function expireSession(message = '登录已过期，请重新登录') {
  const hadToken = Boolean(token.value);
  logout();
  if (hadToken) {
    loginError.value = message;
  }
}

api.interceptors.response.use(
  (response) => {
    if (response.data?.code === 401 && token.value) {
      const message = response.data.message || '登录已过期，请重新登录';
      expireSession(message);
      return Promise.reject(new Error(message));
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401 && token.value) {
      expireSession(error.response.data?.message || '登录已过期，请重新登录');
    }
    return Promise.reject(error);
  }
);

onMounted(() => {
  if (token.value) {
    currentView.value = 'dashboard';
    fetchData();
  }
});

onUnmounted(clearCodeTimer);
</script>

<template>
  <div id="app">
    <div v-if="currentView === 'login'" class="login-page">
      <div class="login-bg"></div>

      <div class="login-container">
        <div class="login-brand">
          <div class="brand-logo">
            <img :src="logoUrl" alt="药师祖庭" />
          </div>
          <h1 class="brand-name">药师祖庭</h1>
          <p class="brand-sub">会员管理系统</p>
        </div>

        <div class="login-card">
          <div class="card-tabs">
            <span class="tab active">手机登录</span>
          </div>
          <div class="input-group">
            <label>手机号</label>
            <div class="input-box">
              <input v-model="mobile" type="tel" placeholder="请输入手机号" maxlength="11" />
            </div>
          </div>
          <div class="input-group">
            <label>验证码</label>
            <div class="input-box code-box">
              <input v-model="code" type="text" placeholder="验证码" maxlength="6" />
              <button class="code-btn" @click="sendCode" :disabled="countdown > 0">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </button>
            </div>
          </div>
          <div v-if="loginError" class="error-tip">{{ loginError }}</div>
          <button class="submit-btn" @click="login" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <div class="quick-login">
            <span>快速测试</span>
            <button class="quick-tag" @click="mobile = '13800000000'">管理员</button>
            <button class="quick-tag" @click="mobile = '13900139002'">会员</button>
          </div>
        </div>
        <div class="login-footer">药师祖庭 © 2026</div>
      </div>
    </div>

    <div v-else class="app-layout">
      <aside class="sidebar">
        <div class="sidebar-brand">
          <div class="sb-logo">
            <img :src="logoUrl" alt="药师祖庭" />
          </div>
          <div class="sb-info">
            <div class="sb-name">药师祖庭</div>
            <div class="sb-sub">会员管理系统</div>
          </div>
        </div>

        <nav class="sidebar-menu">
          <div class="menu-group">
            <div class="menu-label">功能</div>
            <button class="menu-item" :class="{ active: activeMenu === 'wallet' }" @click="switchMenu('wallet')">
              <span class="mi-icon">¥</span> 我的钱包
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'withdraw' }" @click="switchMenu('withdraw')">
              <span class="mi-icon">提</span> 申请提现
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'shop' }" @click="switchMenu('shop')">
              <span class="mi-icon">货</span> 货品中心
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'join' }" @click="switchMenu('join')">
              <span class="mi-icon">加</span> 加盟升级
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'team' }" @click="switchMenu('team')">
              <span class="mi-icon">队</span> 我的团队
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'invite' }" @click="switchMenu('invite')">
              <span class="mi-icon">码</span> 邀请会员
            </button>
          </div>
          <div v-if="isAdmin" class="menu-group">
            <div class="menu-label">管理</div>
            <button class="menu-item" :class="{ active: activeMenu === 'products' }" @click="switchMenu('products')">
              <span class="mi-icon">品</span> 商品管理
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'users' }" @click="switchMenu('users')">
              <span class="mi-icon">人</span> 人员管理
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'financeAdmin' }" @click="switchMenu('financeAdmin')">
              <span class="mi-icon">账</span> 财务管理
            </button>
            <button class="menu-item" :class="{ active: activeMenu === 'admin' }" @click="switchMenu('admin')">
              <span class="mi-icon">设</span> 系统配置
            </button>
          </div>
        </nav>

        <div class="sidebar-user">
          <div class="user-card">
            <img class="user-avatar-img" :src="user.avatar || logoUrl" alt="" />
            <div class="user-text">
              <div class="ut-name">{{ user.nickname || user.mobile || '会员' }}</div>
              <div class="ut-role">{{ roleNameMap[role] || '会员' }}</div>
            </div>
          </div>
          <button class="exit-btn" @click="logout">退出</button>
        </div>
      </aside>

      <main class="main-panel">
        <section v-if="activeMenu === 'wallet'">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>药师祖庭</span></div>
            <h2>我的钱包</h2>
            <p>实时查看账户资产与收益明细</p>
          </div>

          <div class="asset-cards">
            <div class="asset-card gold">
              <span class="ac-icon">¥</span>
              <div class="ac-body"><span class="ac-label">账户余额</span><span class="ac-value">¥{{ formatMoney(wallet?.balance) }}</span></div>
            </div>
            <div class="asset-card green">
              <span class="ac-icon">益</span>
              <div class="ac-body"><span class="ac-label">累计收益</span><span class="ac-value">¥{{ formatMoney(wallet?.totalEarnings) }}</span></div>
            </div>
            <div class="asset-card blue">
              <span class="ac-icon">提</span>
              <div class="ac-body"><span class="ac-label">待提现</span><span class="ac-value">¥{{ formatMoney(wallet?.pendingAmount) }}</span></div>
            </div>
          </div>

          <div class="panel-card">
            <div class="pc-header"><h3>收益记录</h3><span class="tag-count">{{ wallet?.recentLogs?.length || 0 }} 条</span></div>
            <div class="pc-body">
              <div v-if="wallet?.recentLogs?.length" class="flow-list">
                <div v-for="log in wallet.recentLogs" :key="log.id || `${log.orderSn}-${log.type}`" class="flow-row">
                  <div class="flow-left"><div class="flow-title">{{ log.remark || log.typeDesc || log.type }}</div><div class="flow-time">{{ formatTime(log.createTime) }}</div></div>
                  <div class="flow-amount" :class="{ up: log.amount > 0 }">{{ log.amount > 0 ? '+' : '' }}¥{{ formatMoney(log.amount) }}</div>
                </div>
              </div>
              <div v-else class="empty-box brand-empty">
                <img :src="logoUrl" alt="" />
                <span>暂无收益记录</span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'withdraw'">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>药师祖庭</span></div>
            <h2>申请提现</h2>
            <p>提交收益提现申请并进入后台审核流程</p>
          </div>

          <div v-if="withdrawalMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(withdrawalMessage), fail: isFailureMessage(withdrawalMessage) }">{{ withdrawalMessage }}</div>
          <div class="panel-card">
            <div class="pc-header"><h3>提现信息</h3><span class="tag-count">可提现 ¥{{ formatMoney(wallet?.balance) }}</span></div>
            <div class="form-grid">
              <label><span>提现金额</span><input v-model="withdrawalForm.amount" type="number" min="0" step="0.01" placeholder="最低金额按系统配置校验" /></label>
              <label><span>提现方式</span><select v-model.number="withdrawalForm.withdrawType"><option :value="1">微信</option><option :value="2">支付宝</option><option :value="3">银行卡</option></select></label>
              <label><span>收款账号</span><input v-model="withdrawalForm.accountNo" placeholder="请输入收款账号" /></label>
              <label><span>收款人</span><input v-model="withdrawalForm.accountName" placeholder="请输入收款人姓名" /></label>
              <label v-if="withdrawalForm.withdrawType === 3" class="span-2"><span>开户行</span><input v-model="withdrawalForm.bankName" placeholder="请输入开户行" /></label>
              <button class="btn-main span-2" @click="submitWithdrawal" :disabled="withdrawalSubmitting">{{ withdrawalSubmitting ? '提交中...' : '提交提现申请' }}</button>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'shop'">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>药师祖庭</span></div>
            <h2>货品中心</h2>
            <p>查看当前上架货品、价格与库存</p>
          </div>

          <div v-if="productMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(productMessage), fail: isFailureMessage(productMessage) }">{{ productMessage }}</div>
          <div class="panel-card">
            <div class="pc-header"><h3>货品筛选</h3><span class="tag-count">{{ publicProducts.length }} 件</span></div>
            <div class="filter-bar">
              <select v-model="publicProductType"><option value="">全部类型</option><option value="1">仪器</option><option value="2">套盒</option><option value="3">单品</option></select>
              <button class="btn-main" @click="loadPublicProducts" :disabled="publicProductLoading">{{ publicProductLoading ? '加载中' : '查询' }}</button>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead><tr><th>货品</th><th>类型</th><th>价格</th><th>库存</th><th>说明</th></tr></thead>
                <tbody>
                  <tr v-for="item in publicProducts" :key="item.id">
                    <td><div class="product-cell"><img :src="imageUrl(item.image)" alt="" /><div><b>{{ item.productName }}</b><span>{{ item.productCode || `ID ${item.id}` }}</span></div></div></td>
                    <td>{{ productTypeMap[item.productType] || item.productType }}</td>
                    <td><div class="price-stack"><span>加盟 ¥{{ formatMoney(item.joinPrice) }}</span><span>市场 ¥{{ formatMoney(item.marketPrice) }}</span></div></td>
                    <td>{{ item.stock }} {{ item.unit || '套' }}</td>
                    <td>{{ item.description || '-' }}</td>
                  </tr>
                  <tr v-if="!publicProducts.length"><td colspan="5"><div class="empty-inline"><img :src="logoUrl" alt="" />暂无上架货品</div></td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'join'">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>药师祖庭</span></div>
            <h2>加盟升级</h2>
            <p>按当前身份创建店铺、代理或合伙人加盟订单</p>
          </div>

          <div v-if="joinMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(joinMessage), fail: isFailureMessage(joinMessage) }">{{ joinMessage }}</div>
          <div class="asset-cards">
            <div class="asset-card green">
              <span class="ac-icon">店</span>
              <div class="ac-body"><span class="ac-label">店铺加盟</span><span class="ac-value">¥{{ formatMoney(policies.STORE_JOIN_FEE || 13960) }}</span></div>
            </div>
            <div class="asset-card gold">
              <span class="ac-icon">代</span>
              <div class="ac-body"><span class="ac-label">代理升级</span><span class="ac-value">¥{{ formatMoney(policies.AGENT_JOIN_FEE || 39800) }}</span></div>
            </div>
            <div class="asset-card blue">
              <span class="ac-icon">合</span>
              <div class="ac-body"><span class="ac-label">合伙人升级</span><span class="ac-value">¥{{ formatMoney(policies.PARTNER_JOIN_FEE || 99800) }}</span></div>
            </div>
          </div>
          <div class="panel-card">
            <div class="pc-header"><h3>可执行操作</h3><span class="tag-count">{{ roleNameMap[role] || '会员' }}</span></div>
            <div class="pc-body join-actions">
              <button class="btn-main" @click="createJoinOrder('store')" :disabled="!canJoinStore || joinLoading">{{ joinLoading === 'store' ? '创建中...' : '创建店铺加盟订单' }}</button>
              <button class="btn-main" @click="createJoinOrder('agent')" :disabled="!canJoinAgent || joinLoading">{{ joinLoading === 'agent' ? '创建中...' : '创建代理加盟订单' }}</button>
              <button class="btn-main" @click="createJoinOrder('partner')" :disabled="!canJoinPartner || joinLoading">{{ joinLoading === 'partner' ? '创建中...' : '创建合伙人加盟订单' }}</button>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'team'">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>药师祖庭</span></div>
            <h2>我的团队</h2>
            <p>查看团队成员与结构关系</p>
          </div>
          <div class="panel-card">
            <div class="pc-header"><h3>团队成员</h3><span class="tag-count">{{ team?.length || 0 }} 人</span></div>
            <div class="pc-body">
              <div v-if="team?.length" class="team-grid">
                <div v-for="member in team" :key="member.userId || member.id" class="team-card">
                  <img class="tm-logo" :src="logoUrl" alt="" />
                  <div class="tm-info">
                    <div class="tm-name">{{ member.nickname || member.mobile || '团队成员' }}</div>
                    <div class="tm-phone">{{ member.mobile || `ID ${member.userId || member.id}` }}</div>
                  </div>
                  <span class="tm-badge" :class="'rb' + member.role">{{ roleNameMap[member.role] || '用户' }}</span>
                </div>
              </div>
              <div v-else class="empty-box brand-empty">
                <img :src="logoUrl" alt="" />
                <span>暂无团队成员</span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'invite'">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>药师祖庭</span></div>
            <h2>邀请会员</h2>
            <p>分享邀请链接并锁定上下级关系</p>
          </div>
          <div class="panel-card">
            <div class="pc-header"><h3>我的邀请码</h3></div>
            <div v-if="inviteQrData" class="invite-block">
              <div class="qr-card">
                <img class="qr-logo" :src="logoUrl" alt="药师祖庭" />
                <p class="qr-title">扫码锁定下级</p>
                <div class="qr-meta">
                  <span>邀请人: <b>{{ inviteQrData.parentNickname }}</b></span>
                  <span>ID: <b>{{ inviteQrData.parentId }}</b></span>
                </div>
                <p class="qr-hint">将链接发给新会员，绑定上下级关系</p>
                <code class="qr-link">{{ inviteQrData.inviteUrl }}</code>
              </div>
            </div>
            <div v-else class="empty-box brand-empty">
              <img :src="logoUrl" alt="" />
              <span>暂无邀请信息</span>
            </div>
          </div>
          <div class="panel-card bind-panel">
            <div class="pc-header"><h3>绑定关系</h3></div>
            <div class="bind-row">
              <input v-model="parentId" type="number" placeholder="输入上级用户 ID" />
              <button class="btn-main" @click="bindParent" :disabled="bindingLoading">立即绑定</button>
            </div>
            <div v-if="bindResult" class="bind-feedback" :class="{ ok: bindResult.code === 200, fail: bindResult.error }">
              {{ bindResult.message || bindResult.error || '绑定成功' }}
            </div>
            <div class="bind-row">
              <input v-model="bindingMobile" type="tel" placeholder="新会员手机号" maxlength="11" />
              <button class="btn-main outline" @click="lockParentBind" :disabled="bindingLoading">锁定下级</button>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'products' && isAdmin">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>管理台</span></div>
            <h2>商品管理</h2>
            <p>后台维护商品、价格、库存、图片与上下架状态</p>
          </div>

          <div v-if="productMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(productMessage), fail: isFailureMessage(productMessage) }">{{ productMessage }}</div>

          <div class="admin-grid two">
            <div class="panel-card">
              <div class="pc-header">
                <h3>{{ productForm.id ? '编辑商品' : '新增商品' }}</h3>
                <button class="text-btn" @click="resetProductForm">清空</button>
              </div>
              <div class="form-grid product-form">
                <label><span>商品名称</span><input v-model="productForm.productName" placeholder="请输入商品名称" /></label>
                <label><span>商品编码</span><input v-model="productForm.productCode" placeholder="可选，必须唯一" /></label>
                <label><span>商品类型</span><select v-model.number="productForm.productType"><option :value="1">仪器</option><option :value="2">套盒</option><option :value="3">单品</option></select></label>
                <label><span>状态</span><select v-model.number="productForm.status"><option :value="1">上架</option><option :value="0">下架</option></select></label>
                <label><span>市场价</span><input v-model.number="productForm.marketPrice" type="number" min="0" step="0.01" /></label>
                <label><span>加盟价</span><input v-model.number="productForm.joinPrice" type="number" min="0" step="0.01" /></label>
                <label><span>代理价</span><input v-model.number="productForm.agentPrice" type="number" min="0" step="0.01" /></label>
                <label><span>合伙价</span><input v-model.number="productForm.partnerPrice" type="number" min="0" step="0.01" /></label>
                <label><span>库存</span><input v-model.number="productForm.stock" type="number" min="0" /></label>
                <label><span>单位</span><input v-model="productForm.unit" placeholder="套 / 件 / 盒" /></label>
                <label class="span-2"><span>商品图片</span><div class="upload-row"><input v-model="productForm.image" placeholder="/uploads/products/xxx.png" /><label class="file-btn"><input type="file" accept="image/*" @change="uploadProductImage" />{{ productUploading ? '上传中' : '上传' }}</label></div></label>
                <label class="span-2"><span>商品描述</span><textarea v-model="productForm.description" rows="3" placeholder="商品说明"></textarea></label>
                <div v-if="productForm.image" class="span-2 preview-row">
                  <img :src="imageUrl(productForm.image)" alt="" />
                  <span>{{ productForm.image }}</span>
                </div>
                <button class="btn-main span-2" @click="saveProduct" :disabled="productSaving">{{ productSaving ? '保存中...' : '保存商品' }}</button>
              </div>
            </div>

            <div class="panel-card">
              <div class="pc-header"><h3>商品筛选</h3><span class="tag-count">{{ productTotal }} 条</span></div>
              <div class="filter-bar stacked">
                <input v-model="productFilters.keyword" placeholder="商品名 / 编码" />
                <select v-model="productFilters.productType"><option value="">全部类型</option><option value="1">仪器</option><option value="2">套盒</option><option value="3">单品</option></select>
                <select v-model="productFilters.status"><option value="">全部状态</option><option value="1">上架</option><option value="0">下架</option></select>
                <button class="btn-main" @click="searchProducts" :disabled="productLoading">{{ productLoading ? '加载中' : '查询' }}</button>
              </div>
            </div>
          </div>

          <div class="panel-card">
            <div class="pc-header">
              <h3>商品列表</h3>
              <div class="header-actions">
                <button class="text-btn" @click="exportProducts" :disabled="!products.length">导出CSV</button>
                <span class="tag-count">{{ productTotal }} 条</span>
              </div>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead><tr><th>商品</th><th>类型</th><th>价格</th><th>库存</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in products" :key="item.id">
                    <td><div class="product-cell"><img :src="imageUrl(item.image)" alt="" /><div><b>{{ item.productName }}</b><span>{{ item.productCode || `ID ${item.id}` }}</span></div></div></td>
                    <td>{{ productTypeMap[item.productType] || item.productType }}</td>
                    <td><div class="price-stack"><span>加盟 ¥{{ formatMoney(item.joinPrice) }}</span><span>市场 ¥{{ formatMoney(item.marketPrice) }}</span></div></td>
                    <td>{{ item.stock }} {{ item.unit || '套' }}</td>
                    <td><span class="status-pill" :class="{ on: item.status === 1, off: item.status !== 1 }">{{ productStatusMap[item.status] }}</span></td>
                    <td>{{ formatTime(item.updateTime || item.createTime) }}</td>
                    <td><div class="actions"><button class="text-btn" @click="editProduct(item)">编辑</button><button class="text-btn" @click="toggleProductStatus(item)">{{ item.status === 1 ? '下架' : '上架' }}</button></div></td>
                  </tr>
                  <tr v-if="!products.length"><td colspan="7"><div class="empty-inline"><img :src="logoUrl" alt="" />暂无商品</div></td></tr>
                </tbody>
              </table>
            </div>
            <div class="pagination-bar">
              <span>第 {{ productPage.page }} / {{ totalPages(productTotal, productPage) }} 页</span>
              <select v-model.number="productPage.size" @change="searchProducts"><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option></select>
              <button class="text-btn" @click="setPage(productPage, productPage.page - 1, productTotal, loadProducts)" :disabled="!canPrev(productPage)">上一页</button>
              <button class="text-btn" @click="setPage(productPage, productPage.page + 1, productTotal, loadProducts)" :disabled="!canNext(productTotal, productPage)">下一页</button>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'users' && isAdmin">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>管理台</span></div>
            <h2>人员管理</h2>
            <p>查询会员，调整角色与账号状态</p>
          </div>

          <div v-if="userMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(userMessage), fail: isFailureMessage(userMessage) }">{{ userMessage }}</div>
          <div class="panel-card">
            <div class="pc-header"><h3>人员筛选</h3><span class="tag-count">{{ userTotal }} 人</span></div>
            <div class="filter-bar">
              <input v-model="userFilters.keyword" placeholder="手机号 / 昵称" />
              <select v-model="userFilters.role"><option value="">全部角色</option><option value="0">普通会员</option><option value="1">店铺会员</option><option value="2">代理会员</option><option value="3">合伙人</option><option value="9">管理员</option><option value="10">超级管理员</option></select>
              <select v-model="userFilters.status"><option value="">全部状态</option><option value="1">正常</option><option value="0">禁用</option></select>
              <button class="btn-main" @click="searchAdminUsers" :disabled="userLoading">{{ userLoading ? '加载中' : '查询' }}</button>
            </div>
          </div>

          <div class="panel-card">
            <div class="pc-header">
              <h3>人员列表</h3>
              <div class="header-actions">
                <button class="text-btn" @click="exportUsers" :disabled="!adminUsers.length">导出CSV</button>
                <span class="tag-count">{{ userTotal }} 人</span>
              </div>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead><tr><th>会员</th><th>上级</th><th>团队</th><th>资产</th><th>角色</th><th>状态</th><th>注册时间</th></tr></thead>
                <tbody>
                  <tr v-for="item in adminUsers" :key="item.id">
                    <td><div class="user-cell"><img :src="item.avatar || logoUrl" alt="" /><div><b>{{ item.nickname || item.mobile }}</b><span>{{ item.mobile || `ID ${item.id}` }}</span></div></div></td>
                    <td><div class="price-stack"><span>{{ item.parentNickname || item.parentMobile || '-' }}</span><span v-if="item.parentId">ID {{ item.parentId }}</span></div></td>
                    <td>代理 {{ item.agentCount || 0 }} / 店铺 {{ item.storeCount || 0 }}</td>
                    <td><div class="price-stack"><span>余额 ¥{{ formatMoney(item.balance) }}</span><span>累计 ¥{{ formatMoney(item.totalEarnings) }}</span></div></td>
                    <td><select v-model.number="item.role" @change="updateUser(item, { role: item.role })"><option :value="0">普通会员</option><option :value="1">店铺会员</option><option :value="2">代理会员</option><option :value="3">合伙人</option><option :value="9">管理员</option><option :value="10">超级管理员</option></select></td>
                    <td><select v-model.number="item.status" @change="updateUser(item, { status: item.status })"><option :value="1">正常</option><option :value="0">禁用</option></select></td>
                    <td>{{ formatTime(item.createTime) }}</td>
                  </tr>
                  <tr v-if="!adminUsers.length"><td colspan="7"><div class="empty-inline"><img :src="logoUrl" alt="" />暂无人员</div></td></tr>
                </tbody>
              </table>
            </div>
            <div class="pagination-bar">
              <span>第 {{ userPage.page }} / {{ totalPages(userTotal, userPage) }} 页</span>
              <select v-model.number="userPage.size" @change="searchAdminUsers"><option :value="10">10人/页</option><option :value="20">20人/页</option><option :value="50">50人/页</option></select>
              <button class="text-btn" @click="setPage(userPage, userPage.page - 1, userTotal, loadAdminUsers)" :disabled="!canPrev(userPage)">上一页</button>
              <button class="text-btn" @click="setPage(userPage, userPage.page + 1, userTotal, loadAdminUsers)" :disabled="!canNext(userTotal, userPage)">下一页</button>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'financeAdmin' && isAdmin">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>管理台</span></div>
            <h2>财务管理</h2>
            <p>查看资金概览、提现审核与收益流水</p>
          </div>

          <div v-if="financeMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(financeMessage), fail: isFailureMessage(financeMessage) }">{{ financeMessage }}</div>

          <div class="asset-cards">
            <div class="asset-card green"><span class="ac-icon">余</span><div class="ac-body"><span class="ac-label">会员总余额</span><span class="ac-value">¥{{ formatMoney(financeSummary.totalBalance) }}</span></div></div>
            <div class="asset-card gold"><span class="ac-icon">收</span><div class="ac-body"><span class="ac-label">累计收益</span><span class="ac-value">¥{{ formatMoney(financeSummary.totalEarnings) }}</span></div></div>
            <div class="asset-card blue"><span class="ac-icon">审</span><div class="ac-body"><span class="ac-label">待审提现</span><span class="ac-value">¥{{ formatMoney(financeSummary.pendingWithdrawals) }}</span></div></div>
          </div>

          <div class="panel-card">
            <div class="pc-header">
              <h3>提现审核</h3>
              <div class="header-actions">
                <button class="text-btn" @click="exportWithdrawals" :disabled="!withdrawals.length">导出CSV</button>
                <span class="tag-count">{{ withdrawalTotal }} 条</span>
              </div>
            </div>
            <div class="filter-bar">
              <select v-model="withdrawalFilters.status"><option value="">全部状态</option><option value="0">待审核</option><option value="1">已审核</option><option value="2">已打款</option><option value="3">已拒绝</option></select>
              <input v-model="withdrawalFilters.userId" type="number" placeholder="用户 ID" />
              <button class="btn-main" @click="searchWithdrawals" :disabled="financeLoading">{{ financeLoading ? '加载中' : '查询' }}</button>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead><tr><th>单号</th><th>用户</th><th>金额</th><th>账户</th><th>状态</th><th>时间</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in withdrawals" :key="item.id">
                    <td>{{ item.withdrawSn }}</td>
                    <td><div class="price-stack"><span>{{ item.userNickname || item.userMobile || `ID ${item.userId}` }}</span><span>ID {{ item.userId }}</span></div></td>
                    <td><div class="price-stack"><span>申请 ¥{{ formatMoney(item.amount) }}</span><span>到账 ¥{{ formatMoney(item.actualAmount) }}</span></div></td>
                    <td><div class="price-stack"><span>{{ item.accountName || '-' }}</span><span>{{ item.bankName || item.accountNo || '-' }}</span></div></td>
                    <td><span class="status-pill" :class="{ on: item.status === 2, pending: item.status === 0, off: item.status === 3 }">{{ withdrawalStatusMap[item.status] || item.status }}</span></td>
                    <td>{{ formatTime(item.createTime) }}</td>
                    <td><div class="actions"><button v-if="item.status === 0" class="text-btn" @click="approveWithdrawal(item, true)">通过</button><button v-if="item.status === 0" class="text-btn danger" @click="approveWithdrawal(item, false)">拒绝</button><button v-if="item.status === 1" class="text-btn" @click="completeWithdrawal(item)">打款</button></div></td>
                  </tr>
                  <tr v-if="!withdrawals.length"><td colspan="7"><div class="empty-inline"><img :src="logoUrl" alt="" />暂无提现记录</div></td></tr>
                </tbody>
              </table>
            </div>
            <div class="pagination-bar">
              <span>第 {{ withdrawalPage.page }} / {{ totalPages(withdrawalTotal, withdrawalPage) }} 页</span>
              <select v-model.number="withdrawalPage.size" @change="searchWithdrawals"><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option></select>
              <button class="text-btn" @click="setPage(withdrawalPage, withdrawalPage.page - 1, withdrawalTotal, loadFinanceAdmin)" :disabled="!canPrev(withdrawalPage)">上一页</button>
              <button class="text-btn" @click="setPage(withdrawalPage, withdrawalPage.page + 1, withdrawalTotal, loadFinanceAdmin)" :disabled="!canNext(withdrawalTotal, withdrawalPage)">下一页</button>
            </div>
          </div>

          <div class="panel-card">
            <div class="pc-header">
              <h3>收益流水</h3>
              <div class="header-actions">
                <button class="text-btn" @click="exportProfitLogs" :disabled="!profitLogs.length">导出CSV</button>
                <span class="tag-count">{{ profitTotal }} 条</span>
              </div>
            </div>
            <div class="filter-bar">
              <input v-model="profitFilters.receiverId" type="number" placeholder="收款用户 ID" />
              <input v-model="profitFilters.type" placeholder="流水类型" />
              <button class="btn-main" @click="searchProfitLogs" :disabled="financeLoading">查询</button>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead><tr><th>订单</th><th>收款人</th><th>贡献人</th><th>金额</th><th>类型</th><th>备注</th><th>时间</th></tr></thead>
                <tbody>
                  <tr v-for="item in profitLogs" :key="item.id">
                    <td>{{ item.orderSn || '-' }}</td>
                    <td><div class="price-stack"><span>{{ item.receiverNickname || item.receiverMobile || `ID ${item.receiverId}` }}</span><span>ID {{ item.receiverId }}</span></div></td>
                    <td><div class="price-stack"><span>{{ item.contributorNickname || item.contributorMobile || '-' }}</span><span v-if="item.contributorId">ID {{ item.contributorId }}</span></div></td>
                    <td><b class="money-up">¥{{ formatMoney(item.amount) }}</b></td>
                    <td>{{ item.type }}</td>
                    <td>{{ item.remark || '-' }}</td>
                    <td>{{ formatTime(item.createTime) }}</td>
                  </tr>
                  <tr v-if="!profitLogs.length"><td colspan="7"><div class="empty-inline"><img :src="logoUrl" alt="" />暂无收益流水</div></td></tr>
                </tbody>
              </table>
            </div>
            <div class="pagination-bar">
              <span>第 {{ profitPage.page }} / {{ totalPages(profitTotal, profitPage) }} 页</span>
              <select v-model.number="profitPage.size" @change="searchProfitLogs"><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option></select>
              <button class="text-btn" @click="setPage(profitPage, profitPage.page - 1, profitTotal, loadFinanceAdmin)" :disabled="!canPrev(profitPage)">上一页</button>
              <button class="text-btn" @click="setPage(profitPage, profitPage.page + 1, profitTotal, loadFinanceAdmin)" :disabled="!canNext(profitTotal, profitPage)">下一页</button>
            </div>
          </div>
        </section>

        <section v-if="activeMenu === 'admin' && isAdmin">
          <div class="page-top">
            <div class="page-brand"><img :src="logoUrl" alt="" /><span>管理台</span></div>
            <h2>系统配置</h2>
            <p>管理加盟费用与分润政策</p>
          </div>
          <div v-if="policyMessage" class="bind-feedback admin-message" :class="{ ok: !isFailureMessage(policyMessage), fail: isFailureMessage(policyMessage) }">{{ policyMessage }}</div>
          <div v-if="policyWarnings.length" class="policy-warning-panel">
            <strong>配置风险提示</strong>
            <p v-for="warning in policyWarnings" :key="warning">{{ warning }}</p>
          </div>
          <div v-for="group in policyGroups" :key="group.title" class="panel-card">
            <div class="pc-header"><h3>{{ group.title }}</h3></div>
            <div class="config-grid editable">
              <div v-for="item in group.items" :key="item.key" class="cfg-item editable">
                <label :for="item.key">{{ item.label }}</label>
                <div class="cfg-edit-row">
                  <span v-if="(item.prefix ?? '¥')" class="money-prefix">{{ item.prefix ?? '¥' }}</span>
                  <input :id="item.key" v-model="policies[item.key]" type="number" min="0" :step="item.step || 0.01" />
                  <span v-if="item.suffix" class="config-suffix">{{ item.suffix }}</span>
                  <button class="mini-save" @click="savePolicy(item)" :disabled="policySaving">保存</button>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
:root {
  --pri: #2b6b1f;
  --pri-light: #3f7f2c;
  --pri-dark: #1d4416;
  --accent: #c8a45c;
  --success: #52c41a;
  --danger: #ff4d4f;
  --text1: #1f2a1c;
  --text2: #4b5744;
  --text3: #8b9484;
  --bg: #f4f6f1;
  --card: #fff;
  --border: #e4e8dc;
  --shadow: 0 8px 24px rgba(30, 54, 23, .07);
  --shadow-soft: 0 1px 3px rgba(30, 54, 23, .06);
}
body { font-family: -apple-system, "SF Pro Display", "Segoe UI", Roboto, sans-serif; background: var(--bg); color: var(--text1); line-height: 1.6; -webkit-font-smoothing: antialiased; }
button, input, select, textarea { font: inherit; }
button { cursor: pointer; }

.login-page { min-height: 100vh; padding: 32px 16px; background: linear-gradient(160deg, #1d4416 0%, #2b6b1f 55%, #497f34 100%); display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; }
.login-bg { position: absolute; inset: 0; overflow: hidden; background: linear-gradient(180deg, rgba(255,255,255,.05), rgba(255,255,255,0)); }
.login-container { position: relative; z-index: 1; width: min(380px, 100%); }
.login-brand { text-align: center; margin-bottom: 32px; }
.brand-logo { width: 88px; height: 88px; margin: 0 auto 18px; border-radius: 8px; background: rgba(255,255,255,.12); display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 10px 30px rgba(13, 28, 18, .22); }
.brand-logo img { width: 100%; height: 100%; object-fit: cover; }
.brand-name { font-size: 34px; font-weight: 700; color: #f2d77a; }
.brand-sub { color: rgba(255,245,196,.82); font-size: 14px; margin-top: 4px; }
.login-card { background: #fff; border-radius: 8px; padding: 32px 28px; box-shadow: 0 20px 60px rgba(0,0,0,.2); }
.card-tabs { margin-bottom: 24px; }
.tab { font-size: 18px; font-weight: 600; color: var(--pri); }
.input-group { margin-bottom: 18px; }
.input-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text2); margin-bottom: 6px; }
.input-box input { width: 100%; height: 48px; padding: 0 14px; border: 1.5px solid var(--border); border-radius: 8px; font-size: 16px; background: #fafbfc; transition: all .25s; }
input:focus, select:focus, textarea:focus { outline: none; border-color: var(--pri); box-shadow: 0 0 0 3px rgba(43,107,31,.08); }
.code-box { display: flex; gap: 10px; }
.code-box input { flex: 1; min-width: 0; }
.code-btn { flex-shrink: 0; height: 48px; padding: 0 16px; background: var(--pri); color: #fff; border: 0; border-radius: 8px; font-size: 13px; font-weight: 600; min-width: 110px; transition: all .25s; }
.code-btn:disabled { background: #ccc; cursor: not-allowed; }
.error-tip { background: #fff2f0; border: 1px solid #ffccc7; color: var(--danger); padding: 10px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 16px; }
.submit-btn { width: 100%; height: 50px; background: var(--pri); color: #fff; border: none; border-radius: 8px; font-size: 17px; font-weight: 600; transition: all .25s; }
.submit-btn:hover, .btn-main:hover, .mini-save:hover { background: var(--pri-dark); }
.submit-btn:disabled, .btn-main:disabled, .mini-save:disabled { opacity: .6; cursor: not-allowed; }
.quick-login { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--border); display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text3); }
.quick-tag { padding: 4px 12px; border: 0; border-radius: 8px; background: #f0f5ec; color: var(--pri); font-weight: 500; }
.login-footer { text-align: center; margin-top: 20px; color: rgba(255,245,196,.62); font-size: 12px; }

.app-layout { display: flex; min-height: 100vh; }
.sidebar { width: 260px; background: #1a2e18; color: #d4e0d2; display: flex; flex-direction: column; position: fixed; height: 100vh; z-index: 100; }
.sidebar-brand { padding: 28px 24px 24px; border-bottom: 1px solid rgba(255,255,255,.08); display: flex; align-items: center; gap: 14px; }
.sb-logo { width: 48px; height: 48px; border-radius: 8px; background: rgba(255,255,255,.08); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.sb-logo img { width: 100%; height: 100%; object-fit: cover; }
.sb-name { font-size: 17px; font-weight: 700; color: #f2d77a; }
.sb-sub { font-size: 11px; color: rgba(255,245,196,.48); }
.sidebar-menu { flex: 1; padding: 20px 16px; overflow-y: auto; }
.menu-group { margin-bottom: 28px; }
.menu-label { font-size: 11px; text-transform: uppercase; letter-spacing: 2px; color: rgba(255,255,255,.25); padding: 0 14px; margin-bottom: 8px; }
.menu-item { width: 100%; display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 8px; border: 0; background: transparent; font-size: 14px; font-weight: 500; color: rgba(255,255,255,.68); transition: all .2s; text-align: left; }
.menu-item:hover { background: rgba(255,255,255,.06); color: #fff; }
.menu-item.active { background: var(--pri); color: #fff; }
.mi-icon { width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; border-radius: 6px; background: rgba(255,255,255,.12); font-size: 12px; font-weight: 700; }
.sidebar-user { padding: 20px 16px; border-top: 1px solid rgba(255,255,255,.08); }
.user-card { display: flex; align-items: center; gap: 12px; }
.user-avatar-img { width: 38px; height: 38px; border-radius: 8px; object-fit: cover; background: var(--accent); }
.ut-name { font-size: 14px; font-weight: 600; color: #fff; }
.ut-role { font-size: 11px; color: rgba(255,255,255,.48); }
.exit-btn { width: 100%; margin-top: 14px; padding: 10px; background: rgba(255,255,255,.05); border: none; border-radius: 8px; color: rgba(255,255,255,.58); font-size: 13px; transition: all .2s; }
.exit-btn:hover { background: rgba(255,77,79,.2); color: #ff7875; }

.main-panel { flex: 1; margin-left: 260px; padding: 36px 40px; min-width: 0; }
.page-top { margin-bottom: 30px; }
.page-brand { display: inline-flex; align-items: center; gap: 8px; padding: 6px 10px; border-radius: 999px; background: #eef4e7; color: var(--pri); font-size: 13px; font-weight: 600; margin-bottom: 12px; }
.page-brand img { width: 24px; height: 24px; border-radius: 7px; object-fit: cover; }
.page-top h2 { font-size: 26px; font-weight: 700; color: var(--text1); margin-bottom: 4px; }
.page-top p { color: var(--text3); font-size: 14px; }

.asset-cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; margin-bottom: 28px; }
.asset-card { padding: 24px; border-radius: 8px; color: var(--text1); display: flex; align-items: center; gap: 18px; min-width: 0; background: #fff; border: 1px solid var(--border); box-shadow: var(--shadow-soft); position: relative; overflow: hidden; }
.asset-card::before { content: ""; position: absolute; inset: 0 0 auto; height: 3px; background: var(--pri); opacity: .9; }
.asset-card.gold::before { background: var(--accent); }
.asset-card.green::before { background: var(--pri); }
.asset-card.blue::before { background: #6c8d5b; }
.ac-icon { width: 42px; height: 42px; border-radius: 8px; background: #eef4e7; color: var(--pri); display: inline-flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.asset-card.gold .ac-icon { background: #fff7e6; color: #b89129; }
.asset-card.blue .ac-icon { background: #eef3ea; color: #527146; }
.ac-body { display: flex; flex-direction: column; min-width: 0; }
.ac-label { font-size: 13px; color: var(--text3); margin-bottom: 6px; }
.ac-value { font-size: 26px; font-weight: 700; color: var(--text1); overflow-wrap: anywhere; }
.asset-card.gold .ac-value { color: #b76a00; }
.asset-card.green .ac-value { color: var(--pri-dark); }
.asset-card.blue .ac-value { color: #385b2d; }

.panel-card { background: var(--card); border-radius: 8px; border: 1px solid var(--border); box-shadow: var(--shadow); margin-bottom: 22px; overflow: hidden; }
.pc-header { padding: 18px 24px; border-bottom: 1px solid #eef1e8; display: flex; align-items: center; justify-content: space-between; gap: 12px; background: linear-gradient(180deg, #ffffff 0%, #fbfcf8 100%); }
.pc-header h3 { font-size: 16px; font-weight: 600; }
.tag-count { font-size: 12px; padding: 3px 10px; background: #f0f5ec; color: var(--pri); border-radius: 8px; white-space: nowrap; }
.pc-body { padding: 20px 24px; }

.flow-list { display: flex; flex-direction: column; gap: 14px; }
.flow-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 16px; background: #fafbf8; border-radius: 8px; }
.flow-left { flex: 1; min-width: 0; }
.flow-title { font-weight: 500; font-size: 14px; margin-bottom: 3px; }
.flow-time { font-size: 12px; color: var(--text3); }
.flow-amount { font-weight: 700; font-size: 15px; color: var(--text2); white-space: nowrap; }
.flow-amount.up, .money-up { color: var(--success); }
.join-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.team-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.team-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: #fafbf8; border-radius: 8px; }
.tm-logo { width: 42px; height: 42px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.tm-info { flex: 1; min-width: 0; }
.tm-name { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tm-phone { font-size: 12px; color: var(--text3); }
.tm-badge { font-size: 11px; padding: 3px 10px; border-radius: 8px; font-weight: 500; flex-shrink: 0; }
.rb0 { background: #f0f1ec; color: var(--text3); }
.rb1 { background: #eaf5e4; color: var(--pri); }
.rb2 { background: #eef5ea; color: #3a7a2b; }
.rb3, .rb9 { background: #faf3e8; color: #b89129; }

.invite-block { padding: 24px; text-align: center; }
.qr-card { padding: 34px 24px; background: #fff; border: 1px solid #e4eadc; border-radius: 8px; box-shadow: inset 0 0 0 6px #f7faf3; }
.qr-logo { width: 76px; height: 76px; border-radius: 8px; object-fit: cover; box-shadow: 0 8px 24px rgba(31, 68, 22, .16); margin-bottom: 14px; }
.qr-title { font-size: 18px; font-weight: 700; color: var(--pri); margin-bottom: 16px; }
.qr-meta { display: flex; justify-content: center; gap: 32px; font-size: 14px; color: var(--text2); flex-wrap: wrap; }
.qr-meta b { color: var(--pri); }
.qr-hint { margin-top: 16px; font-size: 12px; color: var(--text3); }
.qr-link { display: block; margin-top: 12px; padding: 10px 14px; background: #fff; border: 1px solid var(--border); border-radius: 6px; font-size: 12px; color: var(--text3); word-break: break-all; }

.bind-panel { padding-bottom: 4px; }
.bind-row { display: flex; gap: 12px; padding: 0 24px 16px; }
.bind-row input { flex: 1; height: 44px; min-width: 0; padding: 0 14px; border: 1.5px solid var(--border); border-radius: 8px; font-size: 14px; }
.btn-main { padding: 10px 22px; background: var(--pri); color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; white-space: nowrap; transition: all .2s; box-shadow: 0 4px 12px rgba(43, 107, 31, .16); }
.btn-main.outline { background: #fff; color: var(--pri); border: 1.5px solid var(--pri); }
.btn-main.outline:hover { background: #f0f5ec; }
.bind-feedback { margin: 0 24px 16px; padding: 12px 16px; border-radius: 8px; font-size: 13px; }
.bind-feedback.ok { background: #f6ffed; color: var(--success); border: 1px solid #b7eb8f; }
.bind-feedback.fail { background: #fff2f0; color: var(--danger); border: 1px solid #ffccc7; }
.admin-message { margin: 0 0 18px; }

.admin-grid.two { display: grid; grid-template-columns: minmax(420px, 1.2fr) minmax(260px, .8fr); gap: 22px; align-items: start; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; padding: 20px 24px; }
.form-grid label { display: flex; flex-direction: column; gap: 7px; font-size: 13px; color: var(--text2); font-weight: 600; }
.form-grid input, .form-grid select, .form-grid textarea, .filter-bar input, .filter-bar select, .data-table select { width: 100%; min-width: 0; border: 1.5px solid var(--border); border-radius: 8px; background: #fff; color: var(--text1); }
.form-grid input, .form-grid select, .filter-bar input, .filter-bar select, .data-table select { height: 40px; padding: 0 10px; }
.form-grid textarea { padding: 10px; resize: vertical; }
.span-2 { grid-column: span 2; }
.upload-row { display: flex; gap: 10px; }
.upload-row input { flex: 1; }
.file-btn { height: 40px; min-width: 74px; align-items: center; justify-content: center; border-radius: 8px; background: #eef4e7; color: var(--pri); font-weight: 700; }
.file-btn input { display: none; }
.preview-row { display: flex; align-items: center; gap: 12px; color: var(--text3); font-size: 12px; word-break: break-all; }
.preview-row img { width: 52px; height: 52px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border); flex-shrink: 0; }
.filter-bar { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 0 20px 18px; padding: 14px; align-items: center; background: #fbfcf8; border: 1px solid #edf1e7; border-radius: 8px; box-shadow: inset 0 1px 0 rgba(255, 255, 255, .8); }
.filter-bar.stacked { grid-template-columns: 1fr; }
.filter-bar .btn-main { height: 40px; padding: 0 18px; box-shadow: 0 3px 10px rgba(43, 107, 31, .14); }
.text-btn { min-height: 30px; padding: 0 10px; border: 1px solid #dce8d4; border-radius: 8px; background: #f7fbf3; color: var(--pri); font-weight: 700; font-size: 13px; transition: background .18s, border-color .18s, color .18s; }
.text-btn:hover:not(:disabled) { background: #eef6e8; border-color: #c8dcc0; }
.text-btn.danger { background: #fff7f5; border-color: #ffd9d2; color: var(--danger); }
.text-btn.danger:hover:not(:disabled) { background: #fff1ee; border-color: #ffc1b8; }
.text-btn:disabled { background: #f6f7f3; border-color: #ecefe7; color: var(--text3); cursor: not-allowed; opacity: .65; }
.header-actions { display: inline-flex; align-items: center; justify-content: flex-end; gap: 12px; flex-wrap: wrap; }
.header-actions .text-btn { background: var(--pri); border-color: var(--pri); color: #fff; box-shadow: 0 3px 10px rgba(43, 107, 31, .14); }
.header-actions .text-btn:hover:not(:disabled) { background: var(--pri2); border-color: var(--pri2); }

.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; min-width: 860px; }
.data-table th, .data-table td { padding: 13px 16px; border-bottom: 1px solid #eef1e8; text-align: left; font-size: 13px; line-height: 1.45; vertical-align: middle; }
.data-table th { color: #68715f; font-weight: 800; background: linear-gradient(180deg, #fbfcf8 0%, #f7f9f3 100%); white-space: nowrap; }
.data-table td { color: var(--text2); }
.data-table tbody tr { transition: background .18s; }
.data-table tbody tr:nth-child(even) { background: #fffefb; }
.data-table tbody tr:hover { background: #f8fbf4; }
.product-cell, .user-cell { display: flex; align-items: center; gap: 12px; min-width: 180px; }
.product-cell img, .user-cell img { width: 42px; height: 42px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border); flex-shrink: 0; }
.product-cell b, .user-cell b { display: block; color: var(--text1); font-size: 14px; max-width: 190px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-cell span, .user-cell span, .price-stack span { display: block; color: var(--text3); font-size: 12px; }
.price-stack { display: flex; flex-direction: column; gap: 2px; min-width: 92px; }
.status-pill { display: inline-flex; align-items: center; height: 24px; padding: 0 9px; border: 1px solid #e2e6db; border-radius: 8px; font-size: 12px; font-weight: 800; background: #f5f6f2; color: #6f7868; white-space: nowrap; }
.status-pill.on { background: #f3fbef; border-color: #cde8c0; color: var(--success); }
.status-pill.pending { background: #fff8ea; border-color: #ffe0a6; color: #b86600; }
.status-pill.off { background: #fff3f1; border-color: #ffd4cd; color: var(--danger); }
.actions { display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.empty-inline { min-height: 96px; display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--text3); }
.empty-inline img { width: 34px; height: 34px; border-radius: 8px; object-fit: cover; opacity: .75; }
.pagination-bar { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding: 14px 18px; border-top: 1px solid #f0f1ec; color: var(--text3); font-size: 13px; flex-wrap: wrap; }
.pagination-bar select { height: 34px; padding: 0 8px; border: 1.5px solid var(--border); border-radius: 8px; background: #fff; color: var(--text2); }

.config-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; padding: 18px 20px 20px; }
.config-grid.editable { padding-top: 6px; }
.cfg-item { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 15px 18px; background: #fafbf8; border: 1px solid #edf1e7; border-radius: 8px; font-size: 14px; }
.cfg-item.editable { display: block; background: #fff; }
.cfg-item.editable label { display: block; margin-bottom: 10px; font-weight: 600; color: var(--text2); }
.cfg-edit-row { display: flex; align-items: center; gap: 8px; }
.money-prefix { color: var(--pri); font-weight: 700; }
.config-suffix { color: var(--text3); font-size: 12px; white-space: nowrap; }
.cfg-edit-row input { flex: 1; min-width: 0; height: 38px; padding: 0 10px; border: 1.5px solid var(--border); border-radius: 8px; font-size: 14px; background: #fff; }
.mini-save { height: 38px; padding: 0 14px; border: none; border-radius: 8px; background: var(--pri); color: #fff; font-size: 13px; font-weight: 600; }
.policy-warning-panel { margin: 0 0 16px; padding: 14px 16px 14px 18px; border: 1px solid #ffdca8; border-left: 4px solid #f08c00; border-radius: 8px; background: linear-gradient(180deg, #fff9ef 0%, #fff6e8 100%); color: #8a4b00; box-shadow: 0 6px 18px rgba(161, 93, 0, .06); }
.policy-warning-panel strong { display: block; margin-bottom: 7px; font-size: 14px; color: #9a5100; }
.policy-warning-panel p { margin: 4px 0; font-size: 13px; line-height: 1.5; }

.empty-box { text-align: center; padding: 48px 24px; color: var(--text3); font-size: 14px; }
.brand-empty { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.brand-empty img { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; opacity: .8; }

@media (max-width: 1100px) {
  .asset-cards { grid-template-columns: 1fr; }
  .admin-grid.two { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .app-layout { display: block; min-height: 100vh; padding-bottom: 82px; }
  .sidebar { position: fixed; inset: auto 0 0; width: 100%; height: auto; background: rgba(255, 255, 255, .96); color: var(--text2); border-top: 1px solid #e7ecdf; box-shadow: 0 -8px 28px rgba(31, 68, 22, .1); backdrop-filter: blur(12px); }
  .sidebar-brand, .sidebar-user { display: none; }
  .sidebar-menu { display: flex; gap: 8px; overflow-x: auto; padding: 9px 12px calc(9px + env(safe-area-inset-bottom)); scroll-snap-type: x mandatory; scrollbar-width: none; }
  .sidebar-menu::-webkit-scrollbar { display: none; }
  .menu-group { flex: 0 0 auto; display: flex; gap: 8px; margin-bottom: 0; }
  .menu-label { display: none; }
  .menu-item { min-width: 72px; min-height: 58px; flex-direction: column; justify-content: center; gap: 4px; padding: 7px 8px; scroll-snap-align: start; color: var(--text3); background: transparent; font-size: 12px; line-height: 1.2; text-align: center; }
  .menu-item:hover { background: #f4f8ef; color: var(--pri); }
  .menu-item.active { background: #eaf5e4; color: var(--pri); box-shadow: inset 0 0 0 1px #cfe4c5; }
  .mi-icon { width: 24px; height: 24px; background: #f0f5ec; color: var(--pri); }
  .menu-item.active .mi-icon { background: var(--pri); color: #fff; }
  .main-panel { margin-left: 0; padding: 18px 14px 22px; }
  .page-top { margin-bottom: 18px; }
  .page-brand { padding: 5px 9px; font-size: 12px; }
  .page-top h2 { font-size: 22px; }
  .page-top p { font-size: 13px; line-height: 1.5; }
  .team-grid, .config-grid, .form-grid, .filter-bar { grid-template-columns: 1fr; }
  .span-2 { grid-column: span 1; }
  .bind-row, .upload-row { flex-direction: column; }
  .pc-header { align-items: flex-start; flex-direction: column; padding: 15px 16px; }
  .pc-body, .form-grid, .config-grid, .invite-block { padding: 16px; }
  .filter-bar { margin: 0 12px 16px; padding: 12px; }
  .header-actions, .pagination-bar { justify-content: flex-start; width: 100%; }
  .asset-cards { gap: 12px; margin-bottom: 18px; }
  .asset-card { padding: 18px 16px; gap: 14px; }
  .ac-icon { width: 38px; height: 38px; }
  .ac-value { font-size: 22px; }
  .flow-list { gap: 10px; }
  .flow-row, .team-card { padding: 13px; }
  .qr-card { padding: 24px 16px; box-shadow: inset 0 0 0 4px #f7faf3; }
  .qr-meta { gap: 12px; }
  .bind-row { padding: 0 16px 16px; }
  .code-box { flex-direction: column; }
  .code-btn, .btn-main, .mini-save { width: 100%; }
  .text-btn { min-height: 34px; }
  .table-wrap { margin: 0 -1px; }
  .data-table { min-width: 720px; }
  .data-table th, .data-table td { padding: 12px 13px; }
  .cfg-edit-row { flex-wrap: wrap; }
}

@media (max-width: 480px) {
  .login-page { align-items: flex-start; padding: 64px 14px 28px; }
  .login-container { max-width: 280px; }
  .login-brand { margin-bottom: 26px; }
  .brand-logo { width: 72px; height: 72px; }
  .brand-name { font-size: 30px; }
  .login-card { padding: 28px; }
  .main-panel { padding: 16px 12px 20px; }
  .asset-card { align-items: flex-start; }
  .asset-card .ac-body { width: 100%; }
  .team-card { gap: 10px; }
  .tm-badge { padding: 3px 8px; }
  .pagination-bar { gap: 8px; }
}
</style>

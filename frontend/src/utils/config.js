export const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const config = {
  // 微信小程序AppID
  mpAppId: 'your-wechat-appid',
  
  // 支付配置
  payConfig: {
    wechat: {
      appId: 'your-appid',
      mchId: 'your-mchid',
      apiKey: 'your-apikey'
    }
  }
};

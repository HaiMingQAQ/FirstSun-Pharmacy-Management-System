import request from '@/sheep/request';

const AuthUtil = {
  // 使用手机 + 密码登录
  login: (data) => {
    return request({
      url: '/member/auth/login',
      method: 'POST',
      data,
      custom: {
        showSuccess: true,
        loadingMsg: '登录中',
        successMsg: '登录成功',
      },
    });
  },
  // 使用手机 + 验证码登录
  smsLogin: (data) => {
    return request({
      url: '/member/auth/sms-login',
      method: 'POST',
      data,
      custom: {
        showSuccess: true,
        loadingMsg: '登录中',
        successMsg: '登录成功',
      },
    });
  },
  // 手机号 + 验证码快捷登录（不存在则自动注册）
  // 验证码由后端校验，必须与手机号一起提交；
  // 验证码放在请求头 X-Sms-Code（而不是 params / data），避免被框架的请求日志打印出来
  loginOrRegister: (mobile, code) => {
    return request({
      url: '/member/auth/login-or-register',
      method: 'POST',
      params: {
        mobile,
      },
      header: {
        'X-Sms-Code': code,
      },
      custom: {
        showSuccess: true,
        loadingMsg: '登录中',
        successMsg: '登录成功',
      },
    });
  },
  // 发送手机验证码（后端不返回验证码内容，验证码取自后端配置的开发测试值）
  sendSmsCode: (mobile) => {
    return request({
      url: '/member/auth/send-sms-code',
      method: 'POST',
      params: {
        mobile,
      },
      custom: {
        loadingMsg: '发送中',
        showSuccess: true,
        successMsg: '验证码已发送',
      },
    });
  },
  // 登出系统
  logout: () => {
    return request({
      url: '/member/auth/logout',
      method: 'POST',
    });
  },
  // 刷新令牌
  refreshToken: (refreshToken) => {
    return request({
      url: '/member/auth/refresh-token',
      method: 'POST',
      params: {
        refreshToken,
      },
      custom: {
        showLoading: false, // 不用加载中
        showError: false, // 不展示错误提示
      },
    });
  },
  // 社交授权的跳转
  socialAuthRedirect: (type, redirectUri) => {
    return request({
      url: '/member/auth/social-auth-redirect',
      method: 'GET',
      params: {
        type,
        redirectUri,
      },
      custom: {
        showSuccess: true,
        loadingMsg: '登录中',
      },
    });
  },
  // 社交快捷登录
  socialLogin: (type, code, state) => {
    return request({
      url: '/member/auth/social-login',
      method: 'POST',
      data: {
        type,
        code,
        state,
      },
      custom: {
        showSuccess: true,
        loadingMsg: '登录中',
      },
    });
  },
  // 微信小程序的一键登录
  weixinMiniAppLogin: (phoneCode, loginCode, state) => {
    return request({
      url: '/member/auth/weixin-mini-app-login',
      method: 'POST',
      data: {
        phoneCode,
        loginCode,
        state,
      },
      custom: {
        showSuccess: true,
        loadingMsg: '登录中',
        successMsg: '登录成功',
      },
    });
  },
  // 创建微信 JS SDK 初始化所需的签名
  createWeixinMpJsapiSignature: (url) => {
    return request({
      url: '/member/auth/create-weixin-jsapi-signature',
      method: 'POST',
      params: {
        url,
      },
      custom: {
        showError: false,
        showLoading: false,
      },
    });
  },
  //
};

export default AuthUtil;

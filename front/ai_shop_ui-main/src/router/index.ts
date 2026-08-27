import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/login/Login.vue'),
    },
    {
      path: '/home',
      name: 'Home',
      component: () => import('../views/home/Home.vue'),
    },
    {
      path: '/cart',
      name: 'Cart',
      component: () => import('../views/cart/Cart.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/hotsale',
      name: 'HotSale',
      component: () => import('../views/home/HotSale.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/flash-sale',
      name: 'FlashSale',
      component: () => import('../views/flashsale/FlashSale.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/product/:id',
      name: 'ProductDetail',
      component: () => import('../views/product/ProductDetail.vue'),
    },
    {
      path: '/orders',
      name: 'MyOrders',
      component: () => import('../views/order/MyOrders.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/order-confirm',
      name: 'OrderConfirm',
      component: () => import('../views/cart/OrderConfirm.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/payment/:id',
      name: 'Payment',
      component: () => import('../views/cart/Payment.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/coupons',
      name: 'CouponCenter',
      component: () => import('../views/home/CouponCenter.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat/merchant',
      name: 'CustomerChat',
      component: () => import('../components/chat/CustomerChat.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/platform-chat',
      name: 'PlatformChat',
      component: () => import('../views/chat/PlatformChat.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/ai-chat',
      name: 'AIChat',
      component: () => import('../views/chat/AIChat.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/shop/:id',
      name: 'Shop',
      component: () => import('../views/product/Shop.vue'),
      meta: { requiresAuth: true },
    },
    // 个人中心
    {
      path: '/profile',
      name: 'UserProfile',
      component: () => import('../views/profile/ProfileEdit.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/edit',
      redirect: '/profile',
    },
    {
      path: '/profile/address',
      name: 'AddressManage',
      component: () => import('../views/profile/AddressManage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/favorites',
      name: 'MyFavorites',
      component: () => import('../views/profile/MyFavorites.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/password',
      name: 'ChangePassword',
      component: () => import('../views/profile/ChangePassword.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/follows',
      name: 'MyFollows',
      component: () => import('../views/profile/MyFollows.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/coupons',
      name: 'MyCoupons',
      component: () => import('../views/profile/MyCoupons.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/chat-history',
      name: 'CustomerChatHistory',
      component: () => import('../views/profile/CustomerChatHistory.vue'),
      meta: { requiresAuth: true },
    },
    // 评价晒单
    {
      path: '/review/pending',
      name: 'PendingReviews',
      component: () => import('../views/review/PendingReviews.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/review/write',
      name: 'WriteReview',
      component: () => import('../views/review/WriteReview.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/review/mine',
      name: 'MyReviews',
      component: () => import('../views/review/MyReviews.vue'),
      meta: { requiresAuth: true },
    },
    // 售后
    {
      path: '/aftersale/apply',
      name: 'ApplyAfterSale',
      component: () => import('../views/aftersale/ApplyAfterSale.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/aftersale/list',
      name: 'MyAfterSaleList',
      component: () => import('../views/aftersale/MyAfterSaleList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/aftersale/detail',
      name: 'AfterSaleDetail',
      component: () => import('../views/aftersale/AfterSaleDetail.vue'),
      meta: { requiresAuth: true },
    },
    // 商家入驻（普通用户）
    {
      path: '/merchant/apply',
      name: 'MerchantApply',
      component: () => import('../views/merchant/MerchantApply.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/merchant/apply/status',
      name: 'MerchantApplyStatus',
      component: () => import('../views/merchant/MerchantApplyStatus.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/merchant',
      component: () => import('../views/merchant/MerchantLayout.vue'),
      redirect: '/merchant/dashboard',
      meta: { requiresMerchant: true },
      children: [
        {
          path: 'dashboard',
          name: 'MerchantDashboard',
          component: () => import('../views/merchant/MerchantHome.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'shop/settings',
          name: 'MerchantShopSettings',
          component: () => import('../views/merchant/MerchantShopSettings.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'shop/password',
          name: 'MerchantPassword',
          component: () => import('../views/merchant/MerchantPassword.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'product',
          name: 'MerchantProduct',
          component: () => import('../views/merchant/MerchantProductList.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'order',
          name: 'MerchantOrder',
          component: () => import('../views/merchant/MerchantOrderList.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'aftersale',
          name: 'MerchantAfterSale',
          component: () => import('../views/merchant/MerchantAfterSale.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'aftersale/process',
          name: 'MerchantAfterSaleProcess',
          component: () => import('../views/merchant/MerchantAfterSaleProcess.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'comment',
          name: 'MerchantComment',
          component: () => import('../views/merchant/MerchantCommentList.vue'),
          meta: { requiresMerchant: true },
        },
        {
          path: 'customer-service',
          name: 'MerchantCustomerService',
          component: () => import('../views/merchant/CustomerServicePanel.vue'),
          meta: { requiresMerchant: true },
        },
      ],
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../layout/AdminLayout.vue'),
      redirect: '/admin/user',
      meta: { requiresAdmin: true },
      children: [
        {
          path: 'user',
          name: 'UserManage',
          component: () => import('../views/admin/UserManage.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'merchant',
          name: 'MerchantManage',
          component: () => import('../views/admin/MerchantManage.vue'),
          meta: { title: '商家管理' },
        },
        {
          path: 'shop',
          name: 'ShopManage',
          component: () => import('../views/admin/ShopManage.vue'),
          meta: { title: '店铺管理' },
        },
        {
          path: 'product',
          name: 'ProductManage',
          component: () => import('../views/admin/ProductManage.vue'),
          meta: { title: '商品管理' },
        },
        {
          path: 'category',
          name: 'CategoryManage',
          component: () => import('../views/admin/CategoryManage.vue'),
          meta: { title: '分类管理' },
        },
        {
          path: 'order',
          name: 'OrderManage',
          component: () => import('../views/admin/OrderManage.vue'),
          meta: { title: '订单管理' },
        },
        {
          path: 'coupon',
          name: 'CouponManage',
          component: () => import('../views/admin/CouponManage.vue'),
          meta: { title: '优惠券管理' },
        },
        {
          path: 'after-sale',
          name: 'AfterSaleManage',
          component: () => import('../views/admin/AfterSaleManage.vue'),
          meta: { title: '售后管理' },
        },
        {
          path: 'comment',
          name: 'CommentManage',
          component: () => import('../views/admin/CommentManage.vue'),
          meta: { title: '评价管理' },
        },
        {
          path: 'stats',
          name: 'StatsManage',
          component: () => import('../views/admin/StatsManage.vue'),
          meta: { title: '数据统计' },
        },
        {
          path: 'customer-service',
          name: 'AdminCustomerWorkbench',
          component: () => import('../views/admin/AdminCustomerWorkbench.vue'),
          meta: { title: '客服工作台' },
        },
        {
          path: 'system-config',
          name: 'SystemConfig',
          component: () => import('../views/admin/SystemConfig.vue'),
          meta: { title: '系统配置' },
        },
      ],
    },
    {
      path: '/',
      redirect: '/login',
    },
  ],
})

// 路由守卫 - 校验登录 + 角色权限
router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  const token = auth.token
  const user = auth.user

  // 允许游客访问的路径
  const guestPaths = ['/login', '/home', '/product/']
  const isGuestAllowed = guestPaths.some(p => to.path === p || to.path.startsWith(p))

  // 未登录且不是游客允许路径，强制跳转登录页
  if (!token && !isGuestAllowed) {
    return next('/login')
  }

  // 已登录但无用户信息，跳转登录（跳过游客路径）
  if (!isGuestAllowed && token && !user) {
    auth.logout()
    return next('/login')
  }

  // 访问 /admin 需要 SUPER_ADMIN 或 ADMIN 角色
  if (to.meta.requiresAdmin) {
    if (user?.roleCode !== 'SUPER_ADMIN' && user?.roleCode !== 'ADMIN') {
      ElMessage.error('无管理员权限')
      auth.logout()
      return next('/login')
    }
  }

  // 访问 /merchant 需要 MERCHANT 角色
  if (to.meta.requiresMerchant) {
    if (user?.roleCode !== 'MERCHANT') {
      ElMessage.error('无商家权限')
      return next('/home')
    }
  }

  next()
})

export default router
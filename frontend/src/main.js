import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './index.css'
import VueApexCharts from 'vue3-apexcharts'

// 1. Import your API service
import * as api from './apiService.js' // Use './apiService.js' since main.js is in the src root

// Import components
import Landing from './components/Landing.vue'
import Login from './components/Login.vue'
import Home from './components/Home.vue'
import StockDetail from './components/StockDetail.vue'
import Following from './components/Following.vue'
import Privacy from './components/Privacy.vue'
import TOS from './components/TOS.vue'
import Cookies from './components/Cookies.vue'
import Stocks from './components/stocksChart.vue'
import Auth from './components/AuthSuccess.vue'

// Define routes
const routes = [
  { path: '/', component: Landing },
  { path: '/auth-success', component: Auth },
  { path: '/cookies', component: Cookies },
  { path: '/stocks', component: Stocks },
  { path: '/privacy', component: Privacy },
  { path: '/terms', component: TOS },
  { path: '/login', component: Login },
  { path: '/Home', component: Home, meta: { requiresAuth: true } }, // Mark protected routes
  { path: '/following', component: Following, meta: { requiresAuth: true } }, // Mark protected routes
  { path: '/stock/:symbol', component: StockDetail, props: true },
  { path: '/:pathMatch(.*)*', redirect: '/' } // Catch-all redirects to landing
]

// Create router
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return { top: 0 };
  }
})

// 🔐 Navigation guard (Refactored)
router.beforeEach(async (to, from, next) => {
  // 2. A more scalable way to define public paths
  const publicPaths = ['/', '/login', '/auth-success', '/stocks', '/privacy', '/terms', '/cookies'];
  
  // A path is public if it's in the list OR if it's any stock detail page.
  const isPublic = publicPaths.includes(to.path) || to.path.startsWith('/stock/');

  // Allow access to all public pages immediately.
  // Also, don't block access if the route doesn't require auth.
  if (isPublic || !to.meta.requiresAuth) {
    return next();
  }

  // For protected routes, check for an active session.
  try {
    // 3. Use the apiService to check authentication status. No more hardcoded URLs!
    const data = await api.checkAuthStatus(); 

    if (data.user) {
      // User is authenticated, allow access.
      return next();
    } else {
      // Session is invalid or expired, redirect to login.
      return next('/login');
    }
  } catch (err) {
    // If the /me call fails (e.g., network error, server down), redirect to login.
    console.error('Auth check failed:', err);
    return next('/login');
  }
});


// Create and mount app
const app = createApp(App)
app.use(VueApexCharts)
app.use(router)
app.mount('#app')
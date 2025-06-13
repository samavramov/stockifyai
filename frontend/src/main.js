import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './index.css'
import VueApexCharts from 'vue3-apexcharts'

// Import components
import Landing from './components/Landing.vue'
import Login from './components/Login.vue'
import Home from './components/Home.vue'
import StockDetail from './components/StockDetail.vue'
import Following from './components/Following.vue'
import Privacy from './components/Privacy.vue'
import TOS from './components/TOS.Vue'
import Cookies from './components/Cookies.vue'
import Stocks from './components/stocksChart.vue' // Assuming this is your "All Stocks" page with the table
import Auth from './components/AuthSuccess.vue'

// Define routes
const routes = [
  { path: '/', component: Landing },
  { path: '/auth-success', component: Auth },
  { path: '/cookies', component: Cookies },
  { path: '/stocks', component: Stocks }, // This route shows the table
  { path: '/privacy', component: Privacy },
  { path: '/terms', component: TOS },
  { path: '/login', component: Login },
  { path: '/Home', component: Home },
  { path: '/following', component: Following },
  { path: '/stock/:symbol', component: StockDetail, props: true },
  { path: '/:pathMatch(.*)*', redirect: '/login' } // Catch-all
]

// Create router
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return { top: 0 };
  }
})

let isInOAuthFlow = false;
// 🔐 Navigation guard
router.beforeEach(async (to, from, next) => {
  const publicPaths = ['/', '/login', '/auth-success', '/stocks', '/privacy', '/terms', '/cookies', '/stock/AAPL', '/stock/ADBE', '/stock/AMD', 'stock/AMZN', 'stock/COIN', '/stock/CRM', '/stock/DDOG', '/stock/DOCU'];
  
  if (publicPaths.includes(to.path)) {
    return next();
  }

  try {
    const res = await fetch('http://localhost:8001/me', {
      credentials: 'include' // Important: This sends the cookies
    });

    if (!res.ok) {
      return next('/login');
    }

    const data = await res.json();
    if (data.user) {
      // User is authenticated
      if (to.path === '/auth-success') {
        return next('/home');
      }
      return next();
    }
    return next('/login');
  } catch (err) {
    console.error('Auth check failed:', err);
    return next('/login');
  }
});


// Create and mount app
const app = createApp(App)
app.use(VueApexCharts)
app.use(router)
app.mount('#app')
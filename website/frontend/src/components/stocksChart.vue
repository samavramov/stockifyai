<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="w-full bg-white shadow flex items-center justify-between px-6">
      <img src="/Users/samavramov/stockdashboard/website/frontend/src/images/logo.png" alt="Stockify AI Logo"
        class="h-20 sm:h-20 md:h-20 object-contain" />
      <div class="flex items-center space-x-4">
        <button @click="goToLogin"
          class="bg-royalpurple-500 text-white px-4 py-2 rounded-lg hover:bg-blue-900 transition-colors duration-200 flex items-center space-x-2 flashing-button">
          <span>Sign In</span>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
            stroke="currentColor" class="size-6">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M17.982 18.725A7.488 7.488 0 0 0 12 15.75a7.488 7.488 0 0 0-5.982 2.975m11.963 0a9 9 0 1 0-11.963 0m11.963 0A8.966 9 0 0 1 12 21a8.966 9 0 0 1-5.982-2.275M15 9.75a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
          </svg>
        </button>
        <button @click="logout"
          class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors duration-200 flex items-center space-x-2">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
        </button>
      </div>
    </nav>
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="mb-8">
        <h2 class="text-3xl font-bold text-royalpurple-500 mb-4">Today's Top Movers</h2>
        <p class="text-gray-600 py-3">
          To see the top 5 stocks across all data, please
          <span @click="goToLogin" class="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-purple-400
               cursor-pointer transition-all duration-300 ease-in-out
               hover:from-blue-600 hover:to-purple-600 hover:underline flashing-button">
            Sign In
          </span>
        </p>
        <div class="grid grid-cols-1 md:grid-cols-5 gap-4">
          <div v-for="stock in trendingStocks" :key="stock.symbol" @click="goToStockDetail(stock.symbol)"
            class="bg-white rounded-xl shadow-lg p-4 cursor-pointer hover:shadow-xl transform hover:scale-105 transition-all duration-200 border-l-4"
            :class="stock.percentChange >= 0 ? 'border-green-500' : 'border-red-500'">
            <div class="text-center">
              <div class="text-lg font-bold text-gray-900">{{ stock.symbol }}</div>
              <div class="text-sm text-gray-600 mb-2">{{ stock.name }}</div>
              <div :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'" class="text-xl font-bold">
                {{ stock.percentChange > 0 ? '+' : '' }}{{ stock.percentChange != null ? stock.percentChange.toFixed(2)
                  : '—' }}%
              </div>
              <div class="text-xs text-gray-500 mt-1">
                Sentiment:
                <span :class="sentimentClass(stock.dailySentiment)" class="font-medium">
                  {{ stock.dailySentiment != null ? stock.dailySentiment.toFixed(2) : '—' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="mb-6 border-b border-gray-200">
        <nav class="flex space-x-4" aria-label="Tabs">
          <button @click="activeTab = 'all'"
            :class="[activeTab === 'all' ? 'border-royalpurple-500 text-royalpurple-500' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300', 'whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm']">
            All Stocks
          </button>
          <button @click="activeTab = 'followed'"
            :class="[activeTab === 'followed' ? 'border-royalpurple-500 text-royalpurple-500' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300', 'whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm']">
            Followed Stocks
          </button>
        </nav>
      </div>
      <div>
        <div v-if="activeTab === 'all'" class="space-y-6">
          <div class="mb-8">
            <h2 class="text-3xl font-bold text-royalpurple-500 mb-2">Market Sentiment Overview</h2>
          </div>
        </div>
        <div v-if="activeTab === 'all'" class="relative bg-white rounded-xl shadow-lg overflow-hidden">
          <hr class=" border-gray-200">
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Symbol
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Company Name
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Daily Sentiment
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    10-Day Average
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    % Change
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="(stock, index) in stocks.slice(0, 8)" :key="stock.symbol"
                  @click="goToStockDetail(stock.symbol)" class="hover:bg-gray-50 cursor-pointer">
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ stock.symbol }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ stock.name }}</td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span v-if="stock.dailySentiment !== null" :class="sentimentClass(stock.dailySentiment)"
                      class="text-sm font-semibold">
                      {{ stock.dailySentiment.toFixed(2) }}
                    </span>
                    <span v-else class="text-sm text-gray-400">—</span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span v-if="stock.tenDayAverage !== null" :class="sentimentClass(stock.tenDayAverage)"
                      class="text-sm font-semibold">
                      {{ stock.tenDayAverage.toFixed(2) }}
                    </span>
                    <span v-else class="text-sm text-gray-400">—</span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span v-if="stock.percentChange != null"
                      :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'"
                      class="text-sm font-semibold">
                      {{ stock.percentChange.toFixed(2) + '%' }}
                    </span>
                    <span v-else class="text-sm text-gray-400">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!userEmail" class="relative">
              <table class="min-w-full divide-y divide-gray-200">
                <tbody class="bg-white divide-y divide-gray-200 blur-sm opacity-50 pointer-events-none">
                  <tr v-for="(stock, index) in stocks.slice(8)" :key="stock.symbol"
                    class="hover:bg-gray-50 cursor-pointer">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900"></td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900"></td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span v-if="stock.percentChange != null"
                        :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'"
                        class="text-sm font-semibold">
                      </span>
                      <span v-else class="text-sm text-gray-400">—</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span v-if="stock.percentChange != null"
                        :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'"
                        class="text-sm font-semibold">
                      </span>
                      <span v-else class="text-sm text-gray-400">—</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span v-if="stock.percentChange != null"
                        :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'"
                        class="text-sm font-semibold">
                      </span>
                      <span v-else class="text-sm text-gray-400">—</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="absolute inset-0 z-20 flex items-center justify-center bg-gray-500 bg-opacity-75 rounded-xl">
                <div class="text-center p-6 bg-white rounded-lg shadow-xl max-w-sm">
                  <p class="font-bold text-xl text-gray-900 mb-4">Unlock All Market Data!</p>
                  <p class="text-gray-700 mb-6">Sign in to view the complete list of stocks and unlock advanced
                    features.</p>
                  <button @click="goToLogin"
                    class="bg-royalpurple-500 text-white px-6 py-3 rounded-lg hover:bg-blue-900 transition-colors duration-200 text-lg font-semibold flashing-button">
                    Sign In Now
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="activeTab === 'followed'" class="space-y-6">
          <div class="mb-8 bg-white rounded-xl shadow-lg py-8">
            <div class="text-center">
              <h2 class="text-3xl md:text-3xl font-bold mb-6">
                To view followed stocks and advanced analytics, please
                <span @click="goToLogin" class="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-purple-400
                 cursor-pointer
                 transition-all duration-300 ease-in-out
                 hover:from-blue-600 hover:to-purple-600 hover:underline flashing-button">
                  sign in
                </span>
              </h2>
            </div>
          </div>
        </div>
      </div>
    </main>
    <div v-if="showDropdown" @click="closeDropdown" class="fixed inset-0 z-40"></div>
  </div>
  <footer class="bg-gray-900 text-white py-8 md:py-12">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <h3 class="font-merriweather text-xl md:text-2xl font-bold mb-3 md:mb-4">stockify.ai</h3>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <p class="text-gray-400 text-sm md:text-base mb-2 md:mb-3">
          Advanced stock sentiment analysis for smarter investment decisions.
        </p>
        <div class="flex flex-col md:flex-row items-center justify-center gap-2">
          <router-link to="/terms" class="text-gray-400 hover:underline text-xs md:text-sm">
            Terms of Service
          </router-link>
          <span class="hidden md:block mx-2 text-gray-400"> | </span>
          <router-link to="/privacy" class="text-gray-400 hover:underline text-xs md:text-sm">
            Privacy Policy
          </router-link>
          <span class="hidden md:block mx-2 text-gray-400"> | </span>
          <router-link to="/cookies" class="text-gray-400 hover:underline text-xs md:text-sm">
            Cookie Policy
          </router-link>
        </div>
      </div>
      <p class="text-gray-400 text-xs md:text-sm mt-2 md:mt-3">
        Contact us at
        <a href="mailto:stockify.ai.inquries@gmail.com"
          class="text-royalpurple-500 underline">stockify.ai.inquries@gmail.com</a>
      </p>
    </div>
  </footer>
</template>

<script>
import VueApexCharts from 'vue3-apexcharts';

export default {
  name: 'Dashboard', // Ensure this is 'Home' if that's your component name for routing clarity
  components: {
    apexchart: VueApexCharts
  },
  data() {
    return {
      activeTab: 'all',
      showDropdown: false,
      userName: '',
      userEmail: '',
      userPicture: '',
      API_BASE_URL: 'http://localhost:8001/api',
      stocks: [
        { symbol: 'AAPL', name: 'Apple Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'AMD', name: 'Advanced Micro Devices', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'AMZN', name: 'Amazon.com Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'AVGO', name: 'Broadcom Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'BA', name: 'Boeing Company', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'COIN', name: 'Coinbase Global Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'DIS', name: 'Walt Disney Co.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'GME', name: 'GameStop Corp.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'GOOGL', name: 'Alphabet Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'INTC', name: 'Intel Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'LCID', name: 'Lucid Group Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'META', name: 'Meta Platforms Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'MSFT', name: 'Microsoft Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'MU', name: 'Micron Technology Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'NFLX', name: 'Netflix Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'NVDA', name: 'NVIDIA Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'ORCL', name: 'Oracle Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'PLTR', name: 'Palantir Technologies', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'PYPL', name: 'PayPal Holdings Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'QCOM', name: 'Qualcomm Incorporated', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'RBLX', name: 'Roblox Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'SHOP', name: 'Shopify Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'SNAP', name: 'Snapchat Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'SOFI', name: 'SoFi Technologies Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'SPOT', name: 'Spotify Technology SA', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'TSLA', name: 'Tesla Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'UBER', name: 'Uber Technologies Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'WBD', name: 'Warner Bros. Discovery Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null },
        { symbol: 'ZOOM', name: 'Zoom Video Communications', dailySentiment: null, tenDayAverage: null, percentChange: null }
      ],
      searchQuery: '',
      showSuggestions: true,
      isAddingStock: false,
      addStockError: null,
      isLoading: true,
      loadingError: null,
      followedStocks: [],
    };
  },
  computed: {
    trendingStocks() {
      let sourceForTrending = [];
      if (!this.userEmail) {
        sourceForTrending = this.stocks.slice(0, 8);
      } else {
        sourceForTrending = this.stocks; // If signed in, use all stocks
      }
      return [...sourceForTrending]
        .sort((a, b) => Math.abs(b.percentChange ?? 0) - Math.abs(a.percentChange ?? 0))
        .slice(0, 5);
    },
    lastTenDates() {
      const dates = [];
      const today = new Date();
      for (let i = 9; i >= 0; i--) {
        const d = new Date(today);
        d.setDate(today.getDate() - i);
        dates.push(d.toISOString().slice(0, 10));
      }
      return dates;
    },
    chartSeries() {
      return this.followedStocks
        .filter(stock => stock.lastTen && stock.lastTen.length)
        .map(stock => ({
          name: stock.symbol,
          data: stock.lastTen.slice().reverse().map((value, idx) => ({
            x: this.lastTenDates[idx],
            y: value
          }))
        }));
    },
    chartOptions() {
      return {
        chart: { id: 'sentiment-line-chart', toolbar: { show: false } },
        xaxis: {
          title: { text: 'Date' },
          labels: {
            rotate: -45,
            formatter: (val) => new Date(val).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
          }
        },
        yaxis: { min: -1, max: 1, title: { text: 'Sentiment' } },
        stroke: { curve: 'smooth' },
        tooltip: { enabled: true },
        legend: { position: 'bottom' }
      };
    },
    filteredStocks() {
      const query = this.searchQuery.trim().toUpperCase();
      if (!query) return [];
      return this.stocks.filter(stock =>
        stock.symbol.includes(query) || stock.name.toUpperCase().includes(query)
      );
    },
    followedStocksWithDetails() {
      return this.followedStocks.map(stock => {
        const details = this.stocks.find(s => s.symbol === stock.symbol) || {};
        return { ...details, ...stock };
      });
    }
  },
  mounted() {
    this.loadUserData();
    this.loadAllSentiments();
  },
  methods: {
    goToLogin() {
      this.$router.push('/login')
    },
    async loadUserData() {
      try {
        const sessionRes = await fetch('http://localhost:8001/me', {
          credentials: 'include'
        });
        if (!sessionRes.ok) {
          throw new Error('Not authenticated');
        }

        const sessionData = await sessionRes.json();
        console.log('Session data from /me:', sessionData);

        if (!sessionData.user || !sessionData.user.email) {
          throw new Error('No user object or email found in session data');
        }

        this.userEmail = sessionData.user.email;
        this.userName = sessionData.user.name;
        this.userPicture = sessionData.user.picture;
        await this.loadFollowedStocksData();
      } catch (err) {
        console.error('Failed to load user data:', err);
        this.userEmail = ''; // Ensure userEmail is cleared if authentication fails
        // No redirect here, as this is the public-facing Home.vue.
        // The router guard in main.js handles protecting authenticated routes.
      }
    },
    async logout() {
      this.closeDropdown();
      localStorage.clear();
      sessionStorage.clear();
      try {
        const response = await fetch('http://localhost:8001/logout', { method: 'GET', credentials: 'include' });
        if (!response.ok) {
          console.error('Server-side logout failed:', response.statusText);
        }
      } catch (err) {
        console.error('Network error during logout request:', err);
      } finally {
        this.$router.push('/'); // Redirect to the landing page or login page
      }
    },
    toggleDropdown() { this.showDropdown = !this.showDropdown; },
    closeDropdown() { this.showDropdown = false; },
    goToFollowingPage() { this.$router.push(`/following`); this.closeDropdown(); },
    goToStockDetail(symbol) { this.$router.push(`/stock/${symbol}`); },
    sentimentClass(value) { return value >= 0 ? 'text-green-600' : 'text-red-600'; },
    async loadAllSentiments() {
      try {
        const response = await fetch(`${this.API_BASE_URL}/sentiments`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const allSentiments = await response.json();
        allSentiments.forEach(item => {
          const idx = this.stocks.findIndex(s => s.symbol === item.stockSymbol);
          if (idx !== -1) {
            this.stocks[idx].dailySentiment = item.sentimentValue;
            this.stocks[idx].tenDayAverage = item.tenDayAverage;
            this.stocks[idx].percentChange = item.percentChange;
            this.stocks[idx].name = item.companyName || this.stocks[idx].name;
          }
        });
      } catch (err) {
        console.error('Failed to load all sentiments:', err);
      }
    },
    onInputChange() {
      this.addStockError = null;
      this.showSuggestions = true;
    },
    selectStock(symbol) {
      this.searchQuery = symbol;
      this.showSuggestions = false;
    },
    async loadFollowedStocksData() {
      if (!this.userEmail) {
        this.followedStocks = [];
        this.isLoading = false;
        return;
      }
      this.isLoading = true;
      this.loadingError = null;
      try {
        const symbolsResponse = await fetch(`${this.API_BASE_URL}/getFollowedStocks?email=${encodeURIComponent(this.userEmail)}`);
        if (!symbolsResponse.ok) throw new Error(`HTTP ${symbolsResponse.status}`);
        const followedSymbols = await symbolsResponse.json();

        const sentimentsResponse = await fetch(`${this.API_BASE_URL}/sentiments`);
        if (!sentimentsResponse.ok) throw new Error(`HTTP ${sentimentsResponse.status}`);
        const allSentiments = await sentimentsResponse.json();

        this.followedStocks = followedSymbols.map(symbol => {
          const stockData = allSentiments.find(s => s.stockSymbol === symbol); // Changed from s.symbol to s.stockSymbol
          return stockData ? {
            symbol: stockData.stockSymbol,
            name: stockData.companyName,
            sentimentValue: stockData.sentimentValue,
            lastTen: stockData.lastTen || [],
          } : { symbol, name: 'Data not found', sentimentValue: null, lastTen: [] };
        });

      } catch (error) {
        this.loadingError = `Failed to load followed stocks. ${error.message}`;
      } finally {
        this.isLoading = false;
      }
    },
    async addStock() {
      this.addStockError = null;
      const symbol = this.searchQuery.trim().toUpperCase();
      if (!symbol) return;
      if (!this.stocks.some(s => s.symbol === symbol)) {
        this.addStockError = `${symbol} is not a valid stock symbol.`; return;
      }
      if (this.followedStocks.some(s => s.symbol === symbol)) {
        this.addStockError = `${symbol} is already followed.`; return;
      }
      this.isAddingStock = true;
      try {
        const response = await fetch(`${this.API_BASE_URL}/followStock`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ email: this.userEmail, stockSymbol: symbol })
        });
        if (!response.ok) throw new Error(`Server responded with ${response.status}`);
        await this.loadFollowedStocksData();
        this.searchQuery = '';
      } catch (error) {
        this.addStockError = `Error following stock: ${error.message}`;
      } finally {
        this.isAddingStock = false;
        this.showSuggestions = false;
      }
    },
    async removeStock(symbol) {
      try {
        const response = await fetch(`${this.API_BASE_URL}/unfollowStock`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ email: this.userEmail, stockSymbol: symbol })
        });
        if (!response.ok) throw new Error(`Failed to unfollow stock: ${response.status}`);
        await this.loadFollowedStocksData();
      } catch (error) {
        console.error("Failed to remove stock:", error);
        alert(`Error: ${error.message}`);
      }
    },
  }
};
</script>

<style>
/* Keyframes for the subtle flashing effect */
@keyframes subtle-flash {

  0%,
  100% {
    filter: brightness(100%);
    /* Normal brightness */
  }

  50% {
    filter: brightness(125%);
    /* Slightly brighter */
  }
}

/* Apply the animation to the button */
.flashing-button {
  animation: subtle-flash 1.5s infinite alternate;
  /* 2s duration, infinite loop, alternates direction */
}
</style>
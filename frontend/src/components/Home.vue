<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="w-full bg-white shadow flex items-center justify-between px-6">
      <img src="../images/logo.png" alt="Stockify AI Logo"
        class="h-20 sm:h-20 md:h-20 object-contain" />
      <div class="relative">
        <button @click="toggleDropdown" class="flex items-center space-x-2 focus:outline-none">
          <div
            class="w-10 h-10 bg-gradient-to-r from-purple-500 to-blue-500 rounded-full flex items-center justify-center text-white font-semibold">
            <img v-if="userPicture && !userPicture.includes('default-user')" :src="userPicture"
              referrerPolicy="no-referrer" alt="User Avatar " class="w-10 h-10 rounded-full" />
            <div v-else
              class="w-10 h-10 rounded-full bg-purple-500 text-white flex items-center justify-center text-lg font-semibold">
              {{ userName?.[0]?.toUpperCase() || '?' }}
            </div>
          </div>
        </button>
        <div v-if="showDropdown"
          class="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
          <button @click="goToFollowingPage"
            class="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center space-x-2">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
            </svg>
            <span>Following</span>
          </button>
          <hr class="my-2 border-gray-200">
          <button @click="logout"
            class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center space-x-2">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            <span>Logout</span>
          </button>
        </div>
      </div>
    </nav>
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="mb-8">
        <h2 class="text-3xl font-bold text-royalpurple-500 mb-4">Today's Top Movers</h2>
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
          <div class="mb-8 flex justify-between items-center">
            <h2 class="text-3xl font-bold text-royalpurple-500">Market Sentiment Overview</h2>
            <div class="relative">
              <button @click="toggleSortDropdown"
                class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-royalpurple-500">
                Sort By
                <svg class="-mr-1 ml-2 h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"
                  fill="currentColor" aria-hidden="true">
                  <path fill-rule="evenodd"
                    d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                    clip-rule="evenodd" />
                </svg>
              </button>
              <div v-if="showSortDropdown"
                class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-50">
                <div class="py-1" role="menu" aria-orientation="vertical" aria-labelledby="options-menu">
                  <a href="#" @click.prevent="selectSort('symbol')"
                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                    role="menuitem">Symbol</a>
                  <a href="#" @click.prevent="selectSort('name')"
                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                    role="menuitem">Company Name</a>
                  <a href="#" @click.prevent="selectSort('dailySentiment')"
                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                    role="menuitem">Daily Sentiment</a>
                  <a href="#" @click.prevent="selectSort('tenDayAverage')"
                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                    role="menuitem">10-Day Average</a>
                  <a href="#" @click.prevent="selectSort('percentChange')"
                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-gray-900"
                    role="menuitem">% Change</a>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="activeTab === 'all'" class="bg-white rounded-xl shadow-lg overflow-hidden">
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
                  <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Follow
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="stock in sortedStocks" :key="stock.symbol" @click="goToStockDetail(stock.symbol)"
                  class="hover:bg-gray-50 cursor-pointer">
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
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium flex justify-center items-center">
                    <button v-if="isStockFollowed(stock.symbol)" @click.stop="removeStock(stock.symbol)"
                      class="text-red-500 hover:text-red-700 p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-red-500">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                        stroke="currentColor" class="size-5">
                        <path stroke-linecap="round" stroke-linejoin="round"
                          d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                      </svg>
                    </button>
                    <button v-else @click.stop="addStockFromAll(stock.symbol)"
                      class="text-royalpurple-500 hover:text-royalpurple-700 p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-royalpurple-500">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                        stroke="currentColor" class="size-5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                      </svg>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div v-if="activeTab === 'followed'" class="space-y-6">
          <div class="mb-8">
            <h2 class="text-3xl font-bold text-gray-900 mb-2">{{ userName }}'s Stocks</h2>
            <p class="text-gray-600">
              In-depth analysis available on the
              <router-link to="/following"
                class="text-transparent bg-clip-text bg-gradient-to-r from-purple-500 to-blue-500 font-semibold hover:underline">
                following page
              </router-link>
            </p>
          </div>
        </div>
        <div v-if="activeTab === 'followed'">
          <div class="mb-6 relative item-center">
            <div class="grid grid-cols-1 md:grid-cols-9 gap-4 items-center">
              <div class="lg:col-span-8">
                <input v-model="searchQuery" type="text" placeholder="Search stocks to follow..."
                  class="w-full border border-gray-300 rounded-lg px-4 py-2" @keyup.enter="addStockFromInput"
                  @input="onInputChange" />
                <ul v-if="searchQuery && filteredStocks.length && showSuggestions"
                  class="absolute z-10 w-full bg-white border border-gray-200 rounded-md mt-1 max-h-60 overflow-y-auto shadow-md">
                  <li v-for="stock in filteredStocks" :key="stock.symbol" @click="selectStock(stock.symbol)"
                    class="px-4 py-2 cursor-pointer hover:bg-gray-100">
                    {{ stock.symbol }} - {{ stock.name }}
                  </li>
                </ul>
              </div>
              <div>
                <div class=" text-right">
                  <button :disabled="!searchQuery.trim()" @click="addStockFromInput"
                    class="bg-royalpurple-500 text-white px-4 py-2 rounded-xl disabled:opacity-50">
                    + Add Stock
                  </button>
                </div>
              </div>
              <p v-if="isAddingStock" class="text-sm text-gray-500 mt-2">Adding stock...</p>
              <p v-if="addStockError" class="text-sm text-red-600 mt-2">{{ addStockError }}</p>
            </div>
          </div>
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:items-start">
            <div class="lg:col-span-1">
              <div v-if="isLoading" class="text-center py-8">
                <p class="text-gray-600">Loading followed stocks...</p>
              </div>
              <div v-else-if="loadingError" class="text-center py-8">
                <p class="text-red-600">{{ loadingError }}</p>
              </div>
              <div v-else-if="followedStocks.length === 0" class="text-center py-8 text-gray-500">
                <p>You are not following any stocks yet.</p>
              </div>
              <div v-else class="space-y-4">
                <div v-for="stock in followedStocksWithDetails" :key="stock.symbol"
                  @click="goToStockDetail(stock.symbol)"
                  class="bg-white shadow rounded-xl p-4 flex justify-between items-center cursor-pointer hover:bg-gray-50">
                  <div>
                    <h2 class="text-lg font-semibold">{{ stock.symbol }} - {{ stock.name }}</h2>
                    <p class="text-sm text-gray-600">Sentiment: {{ stock.sentimentValue ?? 'N/A' }}</p>
                  </div>
                  <button @click.stop="removeStock(stock.symbol)" class="text-red-500 hover:text-red-700 p-2">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                      stroke="currentColor" class="size-6">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <div class="lg:col-span-2">
              <div class="bg-white rounded-xl p-6 shadow">
                <h2 class="text-xl font-bold mb-4">Followed Stocks - 10 Day Sentiment</h2>
                <apexchart type="line" height="400" :options="chartOptions" :series="chartSeries" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
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

    <div v-if="showDropdown || showSortDropdown" @click="closeAllDropdowns" class="fixed inset-0 z-40"></div>
  </div>
</template>

<script>
import VueApexCharts from 'vue3-apexcharts';
// 1. Import your entire api service
import * as api from '../apiService.js';

export default {
  name: 'Dashboard',
  components: {
    apexchart: VueApexCharts,
  },
  data() {
    return {
      // 2. Data is simplified. No hardcoded URLs or stock lists.
      // They will be populated from the API.
      stocks: [], // Will hold all stock data from the API
      followedStocks: [], // Will hold followed stock data from the API

      // Component State
      isLoading: true,
      loadingError: null,
      isAddingStock: false,
      addStockError: null,
      activeTab: 'all',
      sortColumn: 'symbol', // Default sort
      sortDirection: 'asc',
      
      // UI State
      showDropdown: false,
      showSortDropdown: false,
      searchQuery: '',
      showSuggestions: true,
      
      // User State
      userName: '',
      userEmail: '',
      userPicture: '',
    };
  },
  computed: {
    // ... all your computed properties remain the same, they will react to the data changes ...
    sortedStocks() {
      if (!this.sortColumn) return this.stocks;
      return [...this.stocks].sort((a, b) => {
        const valA = a[this.sortColumn];
        const valB = b[this.sortColumn];
        if (valA == null && valB == null) return 0;
        if (valA == null) return this.sortDirection === 'asc' ? 1 : -1;
        if (valB == null) return this.sortDirection === 'asc' ? -1 : 1;
        if (typeof valA === 'number' && typeof valB === 'number') {
          return this.sortDirection === 'asc' ? valA - valB : valB - valA;
        }
        const strA = String(valA).toLowerCase();
        const strB = String(valB).toLowerCase();
        if (strA < strB) return this.sortDirection === 'asc' ? -1 : 1;
        if (strA > strB) return this.sortDirection === 'asc' ? 1 : -1;
        return 0;
      });
    },
    trendingStocks() {
      return [...this.stocks]
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
      // Use the followed stocks list, but get full details from the main stocks list
      const followedSymbols = this.followedStocks.map(f => f.symbol);
      return this.stocks
        .filter(stock => followedSymbols.includes(stock.symbol) && stock.lastTen && stock.lastTen.length)
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
      const followedSymbols = new Set(this.followedStocks.map(f => f.symbol));
      return this.stocks.filter(stock => followedSymbols.has(stock.symbol));
    }
  },
  // 3. A single, efficient initialization method in mounted()
  mounted() {
    this.initializePageData();
  },
  methods: {
    // 4. All methods below now use the apiService, making them clean and readable
    async initializePageData() {
      this.isLoading = true;
      try {
        const sessionData = await api.checkAuthStatus();
        if (!sessionData.user || !sessionData.user.email) {
          throw new Error('User not authenticated.');
        }
        this.userEmail = sessionData.user.email;
        this.userName = sessionData.user.name;
        this.userPicture = sessionData.user.picture;

        // Fetch all necessary data in parallel for speed
        const [allSentiments, followedSymbols] = await Promise.all([
          api.getAllSentiments(),
          api.getFollowedSymbols(this.userEmail)
        ]);

        // Populate the main stocks list (single source of truth)
        this.stocks = allSentiments.map(sentiment => ({
          symbol: sentiment.stockSymbol,
          name: sentiment.companyName,
          dailySentiment: sentiment.sentimentValue,
          tenDayAverage: sentiment.tenDayAverage,
          percentChange: sentiment.percentChange,
          lastTen: sentiment.lastTen || [],
        }));

        // Populate the list of followed stocks
        this.followedStocks = followedSymbols.map(symbol => ({ symbol }));

      } catch (err) {
        console.error('Failed to initialize page data:', err);
        this.$router.push('/login'); // Redirect to login if any part fails
      } finally {
        this.isLoading = false;
      }
    },

    async handleFollow(symbol) {
      // Stop row click navigation
      const event = window.event; 
      if (event) event.stopPropagation();

      this.addStockError = null;
      if (this.isStockFollowed(symbol)) return; // Already followed

      this.isAddingStock = true;
      try {
        await api.followStock(this.userEmail, symbol);
        this.followedStocks.push({ symbol }); // Optimistically update UI
        this.searchQuery = ''; // Clear search
      } catch (error) {
        this.addStockError = `Error: ${error.message}`;
      } finally {
        this.isAddingStock = false;
      }
    },

    async handleUnfollow(symbol) {
      // Stop row click navigation
      const event = window.event;
      if (event) event.stopPropagation();
      
      try {
        await api.unfollowStock(this.userEmail, symbol);
        this.followedStocks = this.followedStocks.filter(s => s.symbol !== symbol); // Optimistically update UI
      } catch (error) {
        console.error(`Failed to unfollow ${symbol}:`, error);
        alert(`Error unfollowing stock: ${error.message}`);
      }
    },
    
    async logout() {
      this.closeAllDropdowns();
      localStorage.clear();
      sessionStorage.clear();
      try {
        await api.logoutUser();
      } catch (err) {
        console.error('Logout request failed:', err);
      } finally {
        // Fix: Corrected the redirect path from a git command typo
        this.$router.push('/login'); 
      }
    },

    // UI and Helper methods
    isStockFollowed(symbol) {
      return this.followedStocks.some(stock => stock.symbol === symbol);
    },
    sortTable(column) {
      if (this.sortColumn === column) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortColumn = column;
        this.sortDirection = 'asc';
      }
    },
    toggleDropdown() { this.showDropdown = !this.showDropdown; if (this.showDropdown) this.showSortDropdown = false; },
    toggleSortDropdown() { this.showSortDropdown = !this.showSortDropdown; if (this.showSortDropdown) this.showDropdown = false; },
    closeAllDropdowns() { this.showDropdown = false; this.showSortDropdown = false; },
    goToFollowingPage() { this.$router.push(`/following`); this.closeAllDropdowns(); },
    goToStockDetail(symbol) { this.$router.push(`/stock/${symbol}`); },
    sentimentClass(value) { return value >= 0 ? 'text-green-600' : 'text-red-600'; },
    onInputChange() { this.addStockError = null; this.showSuggestions = true; },
    selectStock(symbol) { this.searchQuery = symbol; this.showSuggestions = false; },
  }
};
</script>
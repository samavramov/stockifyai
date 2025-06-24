<template>
  <nav class="w-full bg-white shadow flex items-center justify-between px-6">
    <img src="../images/logo.png" alt="Stockify AI Logo"
      class="h-20 sm:h-20 md:h-20 object-contain" />
    <div class="text-right">
      <button @click="goBack"
        class="mr-4 p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors duration-200">
        ← Back to Home
      </button>
    </div>
  </nav>
  <hr class=" border-gray-200">
  <div class="bg-gray-50 px-6 py-6 min-h-screen">

    <div class="mb-8">
       <h2 class="text-3xl font-bold text-royalpurple-500 mb-4">Your Top Movers</h2>
      <div v-if="isLoading" class="flex overflow-x-auto gap-4 pb-4">
        <div v-for="i in 5" :key="i" class="min-w-full sm:min-w-[400px] bg-white rounded-xl shadow-lg p-4 animate-pulse">
          <div class="h-6 bg-gray-200 rounded mb-2"></div>
          <div class="h-4 bg-gray-200 rounded w-3/4 mb-3"></div>
          <div class="h-8 bg-gray-200 rounded w-1/2 mx-auto"></div>
        </div>
      </div>

      <div v-else-if="loadingError" class="text-center py-4 text-red-600">
        {{ loadingError }}
      </div>

      <div v-else-if="sortedFollowedStocks.length > 0" class="flex overflow-x-auto gap-4 pb-4 stocks-scroll-container">
        <div v-for="stock in sortedFollowedStocks" :key="stock.symbol" @click="goToStockDetail(stock.symbol)"
          class="min-w-[400px] bg-white rounded-xl shadow-lg p-4 cursor-pointer hover:shadow-xl transform hover:scale-105 transition-all duration-200 border-l-4"
          :class="stock.percentChange >= 0 ? 'border-green-500' : 'border-red-500'">
          <div class="text-center">
            <div class="text-lg font-bold text-gray-900">{{ stock.symbol }}</div>
            <div class="text-sm text-gray-600 mb-2">{{ stock.name }}</div>
            <div :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'" class="text-xl font-bold">
              {{ stock.percentChange > 0 ? '+' : '' }}{{ stock.percentChange.toFixed(2) }}%
            </div>
            <div class="text-xs text-gray-500 mt-1">
              Sentiment:
              <span :class="sentimentClass(stock.sentimentValue)" class="font-medium">
                {{ stock.sentimentValue !== null ? stock.sentimentValue.toFixed(2) : '—' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-4 text-gray-500">
        You don't have any followed stocks with recent data.
      </div>
    </div>
    <hr class="my-[32px] border-gray-200">

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:items-start">
      
      <div class="lg:col-span-1">
        <div v-if="isLoading" class="text-center py-8">
          <p class="text-gray-600">Loading your followed stocks...</p>
        </div>

        <div v-else-if="loadingError" class="text-center py-8">
          <p class="text-red-600">Error loading data: {{ loadingError }}</p>
          <button @click="loadUserDataAndFollowedStocks"
            class="mt-4 bg-blue-500 text-white px-4 py-2 rounded-md hover:bg-blue-600">Retry</button>
        </div>

        <div v-else class="space-y-4">
          <div v-if="followedStocks.length === 0" class="text-center py-8 text-gray-500">
              <p>You are not following any stocks yet.</p>
              <p class="text-sm mt-2">Go to the homepage to discover and follow stocks.</p>
            </div>
          <draggable v-model="followedStocks" item-key="symbol" handle=".drag-handle" @end="onDragEnd" tag="div"
            class="space-y-4">
            <template #item="{ element: stock }">
              <div @click="goToStockDetail(stock.symbol)"
                class="bg-white shadow rounded-xl p-4 flex flex-col md:flex-row justify-between items-start md:items-center cursor-pointer hover:bg-gray-50 transition-colors">
                <div class="mb-2 md:mb-0 flex-grow">
                  <h2 class="text-lg font-semibold">{{ stock.symbol }} - {{ stock.name }}</h2>
                  <p class="text-sm text-gray-600">
                    Current Sentiment: {{ stock.sentimentValue !== null ? stock.sentimentValue.toFixed(2) : 'Loading...'
                    }}<br />
                    Ten Day Average: {{ stock.tenDayAverage !== null ? stock.tenDayAverage.toFixed(2) : 'N/A' }}
                  </p>
                </div>
                <div class="flex items-center space-x-2">
                  <div class="drag-handle cursor-grab p-2 rounded-md hover:bg-gray-200">
                    <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                      xmlns="http://www.w3.org/2000/svg">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16">
                      </path>
                    </svg>
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
            </template>
          </draggable>
        </div>
      </div>

      <div class="lg:col-span-2">
        <div class="bg-white rounded-xl p-6 shadow mb-8">
          <h2 class="text-xl font-bold mb-4">Sentiment Snapshot</h2>
          <apexchart type="bar" height="350" :options="barChartOptions" :series="barChartSeries" />
        </div>
        <div class="bg-white rounded-xl p-6 shadow">
          <h2 class="text-xl font-bold mb-4">10-Day Sentiment Trends
          </h2>
          <apexchart type="line" height="400" :options="chartOptions" :series="chartSeries" />
        </div>
      </div>
    </div>
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
import draggable from 'vuedraggable';

export default {
  name: 'FollowingPage',
  components:
  {
    apexchart: VueApexCharts,
    draggable,
  },
  data() {
    return {
      isLoading: true,
      loadingError: null,
      userEmail: '',
      followedStocks: [], // Will hold full objects: { symbol, name, sentimentValue, ... }
      API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
    };
  },
  computed: {
    sortedFollowedStocks() {
      // Sorts followed stocks by the absolute percentage change for the "Top Movers" display
      return [...this.followedStocks]
        .filter(stock => stock.percentChange !== null)
        .sort((a, b) => Math.abs(b.percentChange) - Math.abs(a.percentChange));
    },
    barChartSeries() {
      return [{
        name: 'Daily Sentiment',
        data: this.followedStocks.map(stock => ({
          x: stock.symbol,
          y: stock.sentimentValue ?? 0
        }))
      }];
    },
    barChartOptions() {
      const today = new Date();
      return {
        chart: { type: 'bar', toolbar: { show: false }, animations: { enabled: true } },
        plotOptions: { bar: { distributed: true, borderRadius: 4, horizontal: false, } },
        colors: this.followedStocks.map(stock => {
          const value = stock.sentimentValue ?? 0;
          if (value > 0.05) return '#16a34a';
          if (value < -0.05) return '#dc2626';
          return '#6b7280';
        }),
        xaxis: { categories: this.followedStocks.map(stock => stock.symbol) },
        yaxis: { min: -1, max: 1, title: { text: `Score For ${today.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}` } },
        legend: { show: false },
      };
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
        .filter(stock => stock.lastTen && stock.lastTen.length > 0)
        .map(stock => ({
          name: stock.symbol,
          data: stock.lastTen.slice().reverse().map((value, idx) => ({
            x: this.lastTenDates[idx] || new Date().toISOString().slice(0,10),
            y: value
          }))
        }));
    },
    chartOptions() {
      return {
        chart: { id: 'sentiment-line-chart', toolbar: { show: false }, animations: { easing: 'easeinout', speed: 400 } },
        xaxis: { type: 'datetime', title: { text: 'Date' }, labels: { rotate: -45, formatter: (val) => new Date(val).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) } },
        yaxis: { min: -1, max: 1, title: { text: 'Sentiment' } },
        stroke: { curve: 'smooth' },
        tooltip: { enabled: true, x: { format: 'dd MMM yyyy' }},
        legend: { position: 'bottom' }
      };
    },
  },
  async mounted() {
    await this.loadPageData();
  },
  methods: {
    async loadPageData() {
      this.isLoading = true;
      this.loadingError = null;
      try {
        // Step 1: Get user email from session
        const sessionRes = await fetch(`${this.API_BASE_URL}/me`, { credentials: 'include' });
        if (!sessionRes.ok) throw new Error('Your session has expired. Please log in again.');
        const sessionData = await sessionRes.json();
        if (!sessionData.user || !sessionData.user.email) throw new Error('Invalid user session data.');
        this.userEmail = sessionData.user.email;

        // Step 2: Get the list of followed stock symbols
        const symbolsResponse = await fetch(`${this.API_BASE_URL}/api/getFollowedStocks?email=${encodeURIComponent(this.userEmail)}`);
        if (!symbolsResponse.ok) throw new Error(`Could not fetch followed stocks list: ${symbolsResponse.statusText}`);
        const followedSymbols = await symbolsResponse.json();

        if (followedSymbols.length === 0) {
          this.followedStocks = [];
          this.isLoading = false;
          return;
        }

        // Step 3: Fetch details for ONLY the followed stocks in parallel
        const detailPromises = followedSymbols.map(symbol =>
          fetch(`${this.API_BASE_URL}/api/sentiments?symbol=${symbol}&limit=1`).then(res => {
            if (!res.ok) {
              console.warn(`Could not fetch details for ${symbol}. It might not have sentiment data yet.`);
              return null; // Return null for failed fetches to not break Promise.all
            }
            return res.json();
          })
        );
        
        const detailedResponses = await Promise.all(detailPromises);
        
        // Step 4: Populate the local data array
        this.followedStocks = detailedResponses
            .flat() // The API returns an array, so flatten the array of arrays
            .filter(stockData => stockData); // Filter out any null responses from failed fetches

      } catch (error) {
        console.error("Failed to load followed stocks page data:", error);
        this.loadingError = error.message;
        this.followedStocks = [];
      } finally {
        this.isLoading = false;
      }
    },
    async removeStock(symbol) {
      try {
        const response = await fetch(`${this.API_BASE_URL}/api/unfollowStock`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ email: this.userEmail, stockSymbol: symbol })
        });

        if (!response.ok) throw new Error(`Failed to unfollow stock: ${response.status}`);
        
        // Optimistic UI update: Instantly remove the stock from the local list
        this.followedStocks = this.followedStocks.filter(s => s.symbol !== symbol);

      } catch (error) {
        console.error("Failed to remove stock:", error);
        alert(`Error: ${error.message}`);
      }
    },
    onDragEnd() {
      // You can implement saving the new order to the backend here if you wish
      console.log('New order:', this.followedStocks.map(s => s.symbol));
    },
    goBack() {
      this.$router.push('/home');
    },
    goToStockDetail(symbol) {
      this.$router.push(`/stock/${symbol}`);
    },
    sentimentClass(value) {
      if (value === null) return 'text-gray-400';
      return value >= 0 ? 'text-green-600' : 'text-red-600';
    },
  }
};
</script>

<style scoped>
.stocks-scroll-container {
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.stocks-scroll-container::-webkit-scrollbar {
  display: none;
}

.sortable-ghost {
  opacity: 0.5;
  background-color: #f0f4f8;
  border: 1px dashed #9ca3af;
}

.sortable-chosen {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  transform: rotate(2deg);
}
</style>
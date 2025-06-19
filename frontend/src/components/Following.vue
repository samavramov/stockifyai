<template>
  <nav class="w-full bg-white shadow flex items-center justify-between px-6">
    <img src="../images/logo.png" alt="Stockify AI Logo"
      class="h-20 sm:h-20 md:h-20 object-contain" />
    <div class="text-right">
      <button @click="goBack"
        class="mr-4 p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors duration-200">
        ← Back
      </button>
    </div>
  </nav>
  <hr class=" border-gray-200">
  <div class="bg-gray-50 px-6 py-6">
    <div class="mb-8">
      <div v-if="isLoading" class="flex overflow-x-auto gap-4 pb-4">
        <div v-for="i in 5" :key="i" class="min-w-full bg-white rounded-xl shadow-lg p-4 animate-pulse">
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
        You don't have any followed stocks with movement data yet.
      </div>
    </div>
    <hr class="my-[32px] border-gray-200">

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:items-start">
      <div class="lg:col-span-1">
        <div v-if="isLoading" class="text-center py-8">
          <p class="text-gray-600">Loading followed stocks data...</p>
        </div>

        <div v-else-if="loadingError" class="text-center py-8">
          <p class="text-red-600">Error loading data: {{ loadingError }}</p>
          <button @click="loadFollowedStocksData"
            class="mt-4 bg-blue-500 text-white px-4 py-2 rounded-md hover:bg-blue-600">Retry</button>
        </div>

        <div v-else class="space-y-4">
          <div class="mb-6 relative">
            <div class="grid grid-cols-1 lg:grid-cols-4 gap-3 lg:items-start">
              <div class="lg:col-span-2">
                <input v-model="searchQuery" type="text" placeholder="Search stocks to follow..."
                  class="w-full border border-gray-300 rounded-lg px-4 py-2" @keyup.enter="addStock"
                  @input="onInputChange" />
                <ul v-if="searchQuery && filteredStocks.length && showSuggestions"
                  class="absolute z-10 w-full bg-white border border-gray-200 rounded-md mt-1 max-h-60 overflow-y-auto shadow-md">
                  <li v-for="stock in filteredStocks" :key="stock.symbol" @click="selectStock(stock.symbol)"
                    class="px-4 py-2 cursor-pointer hover:bg-gray-100">
                    {{ stock.symbol }} - {{ stock.name }}
                  </li>
                </ul>
              </div>
              <div class="lg:col-span-1">
                <button :disabled="!searchQuery.trim()" @click="addStock"
                  class="bg-royalpurple-500 text-white px-4 py-2 rounded-xl disabled:opacity-50">
                  + Add Stock
                </button>
              </div>
              <div class="relative lg:col-span-1">
                <button @click="toggleSortDropdown"
                  class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-royalpurple-500 w-full justify-center">
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
            <div v-if="followedStocks.length === 0" class="text-center py-8 text-gray-500">
              <p>You are not following any stocks yet. Use the search bar above to add some!</p>
            </div>
            <p v-if="isAddingStock" class="text-sm text-gray-500 mt-2">Adding stock...</p>
            <p v-if="addStockError" class="text-sm text-red-600 mt-2">{{ addStockError }}</p>
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
                    Ten Day Average: {{ stock.tenDayAverage !== null ? stock.tenDayAverage.toFixed(2) : 'N/A' }}<br />
                    Percent Change: {{ stock.percentChange !== null ? stock.percentChange.toFixed(2) + '%' : 'N/A' }}
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
          <h2 class="text-xl font-bold mb-4">Today's Sentiment Snapshot</h2>
          <apexchart type="bar" height="350" :options="barChartOptions" :series="barChartSeries" />
        </div>
        <div class="bg-white rounded-xl p-6 shadow">
          <h2 class="text-xl font-bold mb-4">Sentiment Trends Over the Last 10 days
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
          class="text-royalpurple-500 underline">stockify.ai.inquries@gmail.0com</a>
      </p>
    </div>
  </footer>
</template>

<script>
import VueApexCharts from 'vue3-apexcharts'
import draggable from 'vuedraggable' // Import draggable

export default {
  data() {
    return {
      date: new Date(),
      searchQuery: '',
      showSuggestions: true,
      isAddingStock: false,
      addStockError: null,
      isLoading: true,
      loadingError: null,
      followedStocks: [],
      originalFollowedStocksOrder: [], // To store the initial order
      userEmail: localStorage.getItem('userEmail') || '',
      userName: localStorage.getItem('userName') || '',
      userPicture: localStorage.getItem('userPicture') || '',
      API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
      showSortDropdown: false, // New data property for dropdown visibility
      allowedStocks: [
        { symbol: 'AAPL', name: 'Apple Inc.' },
        { symbol: 'QCOM', name: 'Qualcomm Incorporated' },
        { symbol: 'AMD', name: 'Advanced Micro Devices' },
        { symbol: 'AMZN', name: 'Amazon.com Inc.' },
        { symbol: 'MU', name: 'Micron Technology Inc.' },
        { symbol: 'GME', name: 'GameStop Corp.' },
        { symbol: 'SOFI', name: 'SoFi Technologies Inc.' },
        { symbol: 'NFLX', name: 'Netflix Inc.' },
        { symbol: 'GOOGL', name: 'Alphabet Inc.' },
        { symbol: 'DIS', name: 'Walt Disney Co.' },
        { symbol: 'INTC', name: 'Intel Corporation' },
        { symbol: 'META', name: 'Meta Platforms Inc.' },
        { symbol: 'MSFT', name: 'Microsoft Corporation' },
        { symbol: 'NVDA', name: 'NVIDIA Corporation' },
        { symbol: 'ORCL', name: 'Oracle Corporation' },
        { symbol: 'WBD', name: 'Warner Bros. Discovery Inc.' },
        { symbol: 'PLTR', name: 'Palantir Technologies' },
        { symbol: 'PYPL', name: 'PayPal Holdings Inc.' },
        { symbol: 'RBLX', name: 'Roblox Corporation' },
        { symbol: 'LCID', name: 'Lucid Group Inc.' },
        { symbol: 'SNAP', name: 'Snapchat Inc.' },
        { symbol: 'SHOP', name: 'Shopify Inc.' },
        { symbol: 'SPOT', name: 'Spotify Technology SA' },
        { symbol: 'COIN', name: 'Coinbase Global Inc.' },
        { symbol: 'TSLA', name: 'Tesla Inc.' },
        { symbol: 'BA', name: 'Boeing Company' },
        { symbol: 'AVGO', name: 'Broadcom Inc.' },
        { symbol: 'UBER', name: 'Uber Technologies Inc.' },
        { symbol: 'ZOOM', name: 'Zoom Video Communications' }
      ]
    };
  },
  components:
  {
    apexchart: VueApexCharts,
    draggable,
  },
  computed: {
    sortedFollowedStocks() {
      // This computed property will now sort based on the criteria for the top row display
      return [...this.followedStocks]
        .filter(stock => stock.percentChange !== null)
        .sort((a, b) => Math.abs(b.percentChange) - Math.abs(a.percentChange));
    },
    barChartSeries() {
      // Ensure the bar chart series data is derived from the *currently sorted* followedStocks
      return [{
        name: 'Daily Sentiment',
        data: this.followedStocks.map(stock => ({
          x: stock.symbol,
          y: [-1, stock.sentimentValue ?? 0]
        }))
      }];
    },
    barChartOptions() {
      const today = this.date;
      return {
        chart: {
          type: 'bar',
          toolbar: { show: false },
          animations: { enabled: true }
        },
        plotOptions: {
          bar: {
            distributed: true,
            borderRadius: 4,
            horizontal: false,
          }
        },
        // Ensure colors correspond to the sorted stock symbols
        colors: this.followedStocks.map(stock => {
          const value = stock.sentimentValue ?? 0;
          if (value > 0.05) return '#16a34a';
          if (value < -0.05) return '#dc2626';
          return '#6b7280';
        }),
        xaxis: {
          // Ensure categories correspond to the sorted stock symbols
          categories: this.followedStocks.map(stock => stock.symbol),
          title: {
            text: 'Stock Symbols'
          }
        },
        yaxis: {
          min: -1,
          max: 1,
          title: {
            text: `Score For ${today.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`
          }
        },
        legend: {
          show: false
        },
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
        .filter(stock => stock.lastTen && stock.lastTen.length)
        .map(stock => ({
          name: stock.symbol,
          data: stock.lastTen
            .slice()
            .reverse()
            .map((value, idx) => ({
              x: this.lastTenDates[idx],
              y: value
            }))
        }));
    },
    chartOptions() {
      return {
        chart: {
          id: 'sentiment-line-chart',
          toolbar: { show: false },
          animations: { easing: 'easeinout', speed: 400 }
        },
        xaxis: {
          title: { text: 'Date' },
          labels: {
            rotate: -45,
            formatter: function (val) {
              const date = new Date(val);
              return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
            }
          }
        },
        yaxis: {
          min: -1,
          max: 1,
          title: { text: 'Sentiment' }
        },
        stroke: {
          curve: 'smooth'
        },
        tooltip: {
          enabled: 'true'

        },
        legend: {
          position: 'bottom'
        }
      };
    },
    filteredStocks() {
      const query = this.searchQuery.trim().toUpperCase();
      return this.allowedStocks.filter(stock =>
        stock.symbol.includes(query) || stock.name.toUpperCase().includes(query)
      );
    },
    followedStocksWithDetails() {
      return this.followedStocks.map(stock => {
        const sentiment = this.getSentimentDetails(stock.sentimentValue);
        return {
          ...stock,
          displayTenDayAverage: stock.tenDayAverage !== null ? stock.tenDayAverage.toFixed(2) : 'N/A',
          displayPercentChange: stock.percentChange !== null ? `${stock.percentChange.toFixed(2)}%` : '...',
          sentimentText: sentiment.text
        };
      });
    }
  },
  async mounted() {
    if (this.userEmail) {
      await this.loadFollowedStocksData();
    } else {
      await this.loadUserData();
    }
    // Close dropdown if clicked outside
    document.addEventListener('click', this.closeSortDropdownOnClickOutside);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.closeSortDropdownOnClickOutside);
  },

  methods: {
    selectSort(column) {
      this.sortTable(column);
      this.showSortDropdown = false;
    },
    async loadFollowedStocksData() {
      this.isLoading = true;
      this.loadingError = null;

      try {
        const symbolsResponse = await fetch(
          `${this.API_BASE_URL}/api/getFollowedStocks?email=${encodeURIComponent(this.userEmail)}`
        );

        if (!symbolsResponse.ok) {
          throw new Error(`Failed to get followed stocks: ${symbolsResponse.status}`);
        }

        const followedSymbols = await symbolsResponse.json();

        const sentimentsResponse = await fetch(`${this.API_BASE_URL}/api/sentiments`);
        if (!sentimentsResponse.ok) {
          throw new Error(`Failed to get sentiments: ${sentimentsResponse.status}`);
        }

        const allSentiments = await sentimentsResponse.json();

        this.followedStocks = followedSymbols.map(symbol => {
          const stockData = allSentiments.find(s => s.stockSymbol === symbol);

          return stockData ? {
            symbol: stockData.stockSymbol,
            name: stockData.companyName || this.getStockName(symbol) || 'N/A',
            tenDayAverage: stockData.tenDayAverage ?? null,
            percentChange: stockData.percentChange ?? null,
            sentimentValue: stockData.sentimentValue ?? null,
            lastTen: Array.isArray(stockData.lastTen) ? stockData.lastTen : [],
          } : {
            symbol,
            name: this.getStockName(symbol) || 'N/A',
            tenDayAverage: null,
            percentChange: null,
            sentimentValue: null,
            lastTen: [],
          };
        });

        // Store the initial order
        this.originalFollowedStocksOrder = [...this.followedStocks];

      } catch (error) {
        console.error("Failed to load followed stocks data:", error);
        this.loadingError = `Failed to load data. Please try again. (${error.message})`;
        this.followedStocks = [];
      } finally {
        this.isLoading = false;
      }
    },

    getStockName(symbol) {
      const stock = this.allowedStocks.find(s => s.symbol === symbol);
      return stock ? stock.name : null;
    },
    onInputChange() {
      this.addStockError = null;
      this.showSuggestions = true;
    },
    selectStock(symbol) {
      this.searchQuery = symbol;
      this.showSuggestions = false;
    },
    async loadUserData() {
      try {
        const sessionRes = await fetch(`${this.API_BASE_URL}/me`, {
          credentials: 'include'
        });

        if (!sessionRes.ok) throw new Error(`Session error: ${sessionRes.status}`);

        const sessionData = await sessionRes.json();

        if (sessionData.user && sessionData.user.email) {
          this.userEmail = sessionData.user.email;
          localStorage.setItem('userEmail', this.userEmail);

          const res = await fetch(`${this.API_BASE_URL}/api/getUser?email=${encodeURIComponent(this.userEmail)}`);
          if (!res.ok) throw new Error(`HTTP ${res.status}`);

          const data = await res.json();
          this.userName = data.name;
          localStorage.setItem('userName', data.name);
          this.userPicture = data.picture;
          localStorage.setItem('userPicture', data.picture);
        }

        await this.loadFollowedStocksData();
      } catch (err) {
        console.error('Failed to load user data:', err);
        this.$router.push('/login');
      }
    },
    async addStock() {
      this.addStockError = null;
      const symbol = this.searchQuery.trim().toUpperCase();

      if (!symbol) {
        this.addStockError = "Please enter a stock symbol.";
        return;
      }

      const isValid = this.allowedStocks.some(s => s.symbol === symbol);
      if (!isValid) {
        this.addStockError = `${symbol} is not a valid stock symbol.`;
        return;
      }

      if (this.followedStocks.some(s => s.symbol === symbol)) {
        this.addStockError = `${symbol} is already in your followed list.`;
        return;
      }

      this.isAddingStock = true;
      try {
        const followResponse = await fetch(`${this.API_BASE_URL}/api/followStock`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
          },
          credentials: 'include',
          body: JSON.stringify({
            email: this.userEmail,
            stockSymbol: symbol
          })
        });

        if (!followResponse.ok) {
          throw new Error(`Failed to follow stock: ${followResponse.status}`);
        }

        // After successfully following, reload ALL followed stocks data to ensure
        // that the newly added stock, as well as existing ones, have their latest data.
        await this.loadFollowedStocksData();

        this.searchQuery = '';
      } catch (error) {
        this.addStockError = `Error following stock: ${error.message}`;
      } finally {
        this.isAddingStock = false;
      }
    },
    async removeStock(symbol) {
      try {
        const response = await fetch(`${this.API_BASE_URL}/api/unfollowStock`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
          },
          credentials: 'include',
          body: JSON.stringify({
            email: this.userEmail,
            stockSymbol: symbol
          })
        });

        if (!response.ok) throw new Error(`Failed to unfollow stock: ${response.status}`);
        this.followedStocks = this.followedStocks.filter(s => s.symbol !== symbol);
        this.originalFollowedStocksOrder = [...this.followedStocks]; // Update original order
      } catch (error) {
        console.error("Failed to remove stock:", error);
      }
    },
    onDragEnd(event) {
      console.log('New order of stocks:', this.followedStocks.map(s => s.symbol));
      // Optionally save the new order to your backend here
      // this.saveStockOrder(this.followedStocks.map(s => s.symbol));
    },

    goBack() {
      if (this.$router) this.$router.push('/home');
      else {
        console.warn("Vue Router not found. Cannot navigate to /home");
        alert("Back functionality requires Vue Router.");
      }
    },
    goToStockDetail(symbol) {
      if (this.$router) this.$router.push(`/stock/${symbol}`);
      else console.warn("Vue Router not found. Cannot navigate to stock detail.");
    },
    sentimentClass(value) {
      if (value === null) return '';
      return value >= 0 ? 'text-green-600' : 'text-red-600';
    },
    toggleSortDropdown() {
      this.showSortDropdown = !this.showSortDropdown;
    },
    closeSortDropdownOnClickOutside(event) {
      // Check if the click occurred outside the dropdown button and the dropdown content
      const sortButton = this.$el.querySelector('.relative.lg\\:col-span-1 > button');
      const sortDropdown = this.$el.querySelector('.origin-top-right.absolute.right-0.mt-2.w-48.rounded-md.shadow-lg.bg-white.ring-1.ring-black.ring-opacity-5.z-50');

      if (this.showSortDropdown && sortButton && sortDropdown &&
        !sortButton.contains(event.target) && !sortDropdown.contains(event.target)) {
        this.showSortDropdown = false;
      }
    },
    sortTable(criteria) {
      let stocksToSort = [...this.followedStocks];

      stocksToSort.sort((a, b) => {
        let valA, valB;

        switch (criteria) {
          case 'symbol':
            return a.symbol.localeCompare(b.symbol);
          case 'name':
            return a.name.localeCompare(b.name);
          case 'dailySentiment':
            valA = a.sentimentValue ?? -Infinity; // Treat null as very low for sorting
            valB = b.sentimentValue ?? -Infinity;
            return valB - valA; // Descending
          case 'tenDayAverage':
            valA = a.tenDayAverage ?? -Infinity;
            valB = b.tenDayAverage ?? -Infinity;
            return valB - valA; // Descending
          case 'percentChange':
            valA = a.percentChange ?? -Infinity;
            valB = b.percentChange ?? -Infinity;
            return valB - valA; // Descending
          default:
            // If no specific criteria, revert to the original order (if stored)
            // Or maintain current order if original order isn't explicitly managed for this case
            return 0;
        }
      });
      this.followedStocks = stocksToSort;
    }
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

/* Optional: Add some styling for the dragging ghost element */
.sortable-ghost {
  opacity: 0.5;
  background-color: #f0f4f8;
  /* Light gray for the ghost */
  border: 1px dashed #9ca3af;
}

/* Optional: Add styling for the chosen element (the one currently being dragged) */
.sortable-chosen {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  transform: rotate(2deg);
  /* A slight rotation to indicate dragging */
}
</style>
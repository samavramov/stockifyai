<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="w-full bg-white shadow flex items-center justify-between px-6">
      <img src="../images/logo.png" alt="Stockify AI Logo" class="h-20 sm:h-20 md:h-20 object-contain" />
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
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6 inline-block">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v6m3-3H9m12 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
            </svg>
            <span>Following</span>
          </button>
          <hr class="my-2 border-gray-200">
          <button @click="logout"
            class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 flex items-center space-x-2">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6 inline-block">
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
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-4">
          <div v-for="stock in trendingStocks" :key="stock.symbol" @click="goToStockDetail(stock.symbol)"
            class="bg-white rounded-xl shadow-lg p-2 md:p-4 cursor-pointer hover:shadow-xl transform hover:scale-105 transition-all duration-200 border-l-4"
            :class="stock.percentChange >= 0 ? 'border-green-500' : 'border-red-500'">
            <div class="text-center">
              <div class="text-base md:text-lg font-bold text-gray-900">{{ stock.symbol }}</div>
              <div class="text-xs md:text-sm text-gray-600 mb-1 md:mb-2">{{ stock.name }}</div>
              <div :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'"
                class="text-lg md:text-xl font-bold">
                {{ stock.percentChange > 0 ? '+' : '' }}{{ stock.percentChange != null ? stock.percentChange.toFixed(2)
                  : '—' }}%
              </div>
              <div class="text-xs text-gray-500 mt-1 hidden sm:block">
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
            <h2 class="text-3xl font-bold text-royalpurple-500">Market Sentiment <span
              class="hidden md:inline">Overview</span></h2>
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
  <hr class="border-gray-200">
  <div class="overflow-x-auto">
    <table class="min-w-full divide-y divide-gray-200">
      <thead class="bg-gray-50">
        <tr class="hidden md:table-row">
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Symbol</th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Company Name</th>
          <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Daily Sentiment</th>
          <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">10-Day Average</th>
          <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">% Change</th>
          <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Follow</th>
        </tr>
        <tr class="md:hidden">
          <th class="p-4 flex-shrink-0 mr-4 w-24 text-left font-semibold text-sm text-gray-600">Stock</th>
          <th class="p-4 flex-grow text-right font-semibold text-sm text-gray-600">Sentiment</th>
          <th class="p-4 flex-grow text-center font-semibold text-sm text-gray-600">Average</th>
          <th class="p-4 flex-grow text-center font-semibold text-sm text-gray-600">Change</th>
          <th class="p-4 flex-shrink-0 w-10 text-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6 inline-block">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v6m3-3H9m12 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
            </svg>
          </th>
          <th class="p-4 flex-shrink-0 w-10"></th> </tr>
      </thead>
      <tbody class="bg-white divide-y divide-gray-200">
        <tr v-for="stock in sortedStocks" :key="stock.symbol" @click="goToStockDetail(stock.symbol)" class="hover:bg-gray-50 cursor-pointer">
          <td class="hidden md:table-cell px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ stock.symbol }}</td>
          <td class="hidden md:table-cell px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{ stock.name }}</td>
          <td class="hidden md:table-cell px-6 py-4 whitespace-nowrap text-sm text-center">
            <span v-if="stock.dailySentiment !== null" :class="sentimentClass(stock.dailySentiment)" class="font-semibold">
              {{ stock.dailySentiment.toFixed(2) }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </td>
          <td class="hidden md:table-cell px-6 py-4 whitespace-nowrap text-sm text-center">
            <span v-if="stock.tenDayAverage !== null" :class="stock.tenDayAverage >= 0 ? 'text-green-600' : 'text-red-600'" class="font-semibold">
              {{ stock.tenDayAverage.toFixed(2) }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </td>
          <td class="hidden md:table-cell px-6 py-4 whitespace-nowrap text-sm text-center">
            <span v-if="stock.percentChange != null" :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'" class="font-semibold">
              {{ stock.percentChange >= 0 ? '+' : '' }}{{ stock.percentChange.toFixed(2) }}%
            </span>
            <span v-else class="text-gray-400">—</span>
          </td>
          <td class="hidden md:table-cell px-6 py-4 whitespace-nowrap text-center text-sm font-medium">
            <button v-if="isStockFollowed(stock.symbol)" @click.stop="removeStock(stock.symbol)" class="text-red-500 hover:text-red-700 p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-red-500">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
              </svg>
            </button>
            <button v-else @click.stop="addStockFromAll(stock.symbol)" class="text-royalpurple-500 hover:text-royalpurple-700 p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-royalpurple-500">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
            </button>
          </td>

          <td class="md:hidden p-4 flex-shrink-0 mr-4">
            <div class="inline-block bg-gradient-to-r from-purple-500 to-blue-500 text-white text-sm font-bold px-3 py-1 rounded-md">
              {{ stock.symbol }}
            </div>
            <div class="text-sm text-gray-700 mt-1 w-24 truncate" :title="stock.name">{{ stock.name }}</div>
          </td>
          <td class="md:hidden p-4 text-center">
            <div class="text-sm font-semibold" :class="sentimentClass(stock.dailySentiment)">
              {{ stock.dailySentiment != null ? stock.dailySentiment.toFixed(2) : '—' }}
            </div>
          </td>
          <td class="md:hidden p-4 text-center">
            <div v-if="stock.tenDayAverage != null" :class="stock.tenDayAverage >= 0 ? 'text-green-600' : 'text-red-600'" class="text-sm font-semibold">
              {{ stock.tenDayAverage.toFixed(2) }}
            </div>
            <div v-else class="text-sm text-gray-400">—</div>
          </td>
          <td class="md:hidden p-4 text-center">
            <div v-if="stock.percentChange != null" :class="stock.percentChange >= 0 ? 'text-green-600' : 'text-red-600'" class="text-sm font-semibold">
              {{ stock.percentChange >= 0 ? '+' : '' }}{{ stock.percentChange.toFixed(2) }}%
            </div>
            <div v-else class="text-sm text-gray-400">—</div>
          </td>
          <td class="md:hidden p-4 text-center">
            <button v-if="isStockFollowed(stock.symbol)" @click.stop="removeStock(stock.symbol)" class="text-red-500 hover:text-red-700 p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-red-500">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
              </svg>
            </button>
            <button v-else @click.stop="addStockFromAll(stock.symbol)" class="text-royalpurple-500 hover:text-royalpurple-700 p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-royalpurple-500">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-5">
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
            <div class="grid grid-cols-1 md:grid-cols-1 gap-4 items-center">
              <div class="lg:col-span-8 flex items-center space-x-4">
                <div class="flex-grow"> <input v-model="searchQuery" type="text"
                    placeholder="Search stocks to follow..." class="w-full border border-gray-300 rounded-lg px-4 py-2"
                    @keyup.enter="addStockFromInput" @input="onInputChange" />
                  <ul v-if="searchQuery && filteredStocks.length && showSuggestions"
                    class="absolute z-10 w-full bg-white border border-gray-200 rounded-md mt-1 max-h-60 overflow-y-auto shadow-md">
                    <li v-for="stock in filteredStocks" :key="stock.symbol" @click="selectStock(stock.symbol)"
                      class="px-4 py-2 cursor-pointer hover:bg-gray-100 truncated">
                      {{ stock.symbol }} - {{ stock.name }}
                    </li>
                  </ul>
                </div>
                <button :disabled="!searchQuery.trim()" @click="addStockFromInput"
                  class="bg-royalpurple-500 text-white px-4 py-2 rounded-xl disabled:opacity-50">
                  + Add Stock
                </button>
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
              <div v-else class="grid grid-cols-2 sm:grid-cols-1 gap-4">
                <div v-for="stock in followedStocksWithDetails" :key="stock.symbol"
                  @click="goToStockDetail(stock.symbol)"
                  class="bg-white shadow rounded-xl p-4 flex justify-between items-center cursor-pointer hover:bg-gray-50">
                  <div>
                    <h2 class="text-lg font-semibold">{{ stock.symbol }} - {{ stock.name }}</h2>
                    <p class="text-sm text-gray-600">Sentiment: <span v-if="stock.sentimentValue != null"
                        :class="stock.sentimentValue >= 0 ? 'text-green-600' : 'text-red-600'" class="font-semibold">
                        {{ stock.sentimentValue.toFixed(2) }}
                      </span></p>
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
                <h2 class="text-xl font-bold mb-4">Followed Stocks - 10 Day Sentiment Trends</h2>
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

<<script>
// The <script> block remains unchanged as the request was purely template-based.
// Paste the original <script> block here.
import VueApexCharts from 'vue3-apexcharts';

export default {
  name: 'Dashboard',
  components: {
    apexchart: VueApexCharts
  },
  data() {
    return {
      // General State
      sortColumn: null,
      sortDirection: 'asc',
      activeTab: 'all',
      showDropdown: false,
      showSortDropdown: false,
      userName: '',
      userEmail: '',
      userPicture: '',
      base_url: import.meta.env.VITE_API_BASE_URL,
      // Master list of all stocks and their details
      stocks: [
        { symbol: 'AAPL', name: 'Apple Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'AMD', name: 'Advanced Micro Devices', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'AMZN', name: 'Amazon.com Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'AVGO', name: 'Broadcom Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'BA', name: 'Boeing Company', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'COIN', name: 'Coinbase Global Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'DIS', name: 'Walt Disney Co.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'GME', name: 'GameStop Corp.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'GOOGL', name: 'Alphabet Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'INTC', name: 'Intel Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'LCID', name: 'Lucid Group Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'META', name: 'Meta Platforms Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'MSFT', name: 'Microsoft Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'MU', name: 'Micron Technology Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'NFLX', name: 'Netflix Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'NVDA', name: 'NVIDIA Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'ORCL', name: 'Oracle Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'PLTR', name: 'Palantir Technologies', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'PYPL', name: 'PayPal Holdings Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'QCOM', name: 'Qualcomm Incorporated', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'RBLX', name: 'Roblox Corporation', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'SHOP', name: 'Shopify Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'SNAP', name: 'Snapchat Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'SOFI', name: 'SoFi Technologies Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'SPOT', name: 'Spotify Technology SA', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'TSLA', name: 'Tesla Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'UBER', name: 'Uber Technologies Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'WBD', name: 'Warner Bros. Discovery Inc.', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] },
        { symbol: 'ZOOM', name: 'Zoom Video Communications', dailySentiment: null, tenDayAverage: null, percentChange: null, lastTen: [] }
      ],
      // Followed Stocks State
      searchQuery: '',
      showSuggestions: true,
      isAddingStock: false,
      addStockError: null,
      isLoading: true, // Master loading state for the whole component
      loadingError: null,
      followedStocks: [], // This will hold the user's followed stocks
      // New: Add a property to track window width
      windowWidth: window.innerWidth,
      isMobile: false, // NEW: Add isMobile property
    };
  },
  computed: {
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
      const sorted = [...this.stocks].sort((a, b) => Math.abs(b.percentChange ?? 0) - Math.abs(a.percentChange ?? 0));
      // Conditionally slice based on window width
      // Tailwind's 'sm' breakpoint is 640px. Let's use that as our mobile threshold.
      if (this.windowWidth < 640) { // Using 640px as the 'sm' breakpoint for mobile
        return sorted.slice(0, 4); // Display only 4 stocks on mobile
      }
      return sorted.slice(0, 5); // Display 5 stocks on larger screens
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
            y: value.toFixed(2)
          }))
        }));
    },
    chartOptions() {
      return {
        chart: {
          id: 'sentiment-line-chart',
          toolbar: { show: false },
          // Disable zoom for the line chart based on isMobile
          zoom: {
            enabled: !this.isMobile,
          }
        },
        xaxis: {
          title: { text: this.isMobile ? '' : 'Date' },
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
    // NEW: Add barChartOptions computed property
    barChartSeries() {
      return [{
        name: 'Daily Sentiment',
        data: this.stocks.map(stock => ({
          x: stock.symbol,
          y: stock.dailySentiment ?? 0
        }))
      }];
    },
    barChartOptions() {
      return {
        chart: {
          type: 'bar',
          toolbar: { show: false },
          animations: { enabled: true },
          // Conditionally disable zoom for the bar chart based on isMobile
          zoom: {
            enabled: !this.isMobile, // Disable zoom if isMobile is true
          }
        },
        plotOptions: {
          bar: {
            distributed: true,
            borderRadius: 4,
            horizontal: false,
          }
        },
        tooltip: {
            enabled: true,
            y: {
                formatter: (val) => val.toFixed(2)
            }
        },
        dataLabels: {
          enabled: false,
        },
        colors: this.stocks.map(stock => {
          const value = stock.dailySentiment ?? 0;
          if (value > 0.05) return '#16a34a'; // Green
          if (value < -0.05) return '#dc2626'; // Red
          return '#6b7280'; // Gray (neutral)
        }),
        xaxis: { categories: this.stocks.map(stock => stock.symbol), title: { text: 'Stock Symbols' } },
        yaxis: { min: -1, max: 1, title: { text: `Sentiment Score` } },
        legend: { show: false },
      };
    },
    filteredStocks() {
      const query = this.searchQuery.trim().toUpperCase();
      if (!query) return [];
      // Suggest stocks that are available but not already followed
      return this.stocks.filter(stock =>
        !this.isStockFollowed(stock.symbol) &&
        (stock.symbol.includes(query) || stock.name.toUpperCase().includes(query))
      );
    },
    followedStocksWithDetails() {
      return this.followedStocks.map(followed => {
        const details = this.stocks.find(s => s.symbol === followed.symbol) || {};
        return { ...details, ...followed };
      });
    }
  },
  mounted() {
    this.loadInitialData();
    // Add event listener for window resize
    window.addEventListener('resize', this.updateWindowWidth);
    this.checkIfMobile(); // NEW: Check on mount
    window.addEventListener('resize', this.checkIfMobile); // NEW: Check on resize
  },
  beforeUnmount() {
    // Clean up event listener when component is destroyed
    window.removeEventListener('resize', this.updateWindowWidth);
    window.removeEventListener('resize', this.checkIfMobile); // NEW: Cleanup listener
  },
  methods: {
    checkIfMobile() { // NEW: Method to check if mobile
      this.isMobile = window.innerWidth < 768; // Using 768px as a common mobile breakpoint
    },
    // New method to update windowWidth
    updateWindowWidth() {
      this.windowWidth = window.innerWidth;
    },
    // --- NEW, EFFICIENT DATA LOADING ORCHESTRATION ---
    async loadInitialData() {
      this.isLoading = true;
      this.loadingError = null;
      try {
        await this.loadUserData();
        await this.fetchAllStockDataOnce();
        await this.loadFollowedStocks();
      } catch (error) {
        console.error("Failed during initial data load:", error);
        this.loadingError = error.message || "An unexpected error occurred.";
        if (error.message.includes('Not authenticated')) {
          this.$router.push('/login');
        }
      } finally {
        this.isLoading = false;
      }
    },

    async loadUserData() {
      if (this.userEmail) return;
      try {
        const sessionRes = await fetch(`${this.base_url}/me`, { credentials: 'include' });
        if (!sessionRes.ok) throw new Error('Not authenticated');
        const sessionData = await sessionRes.json();
        if (!sessionData.user?.email) throw new Error('Invalid user session');
        this.userEmail = sessionData.user.email;
        this.userName = sessionData.user.name;
        this.userPicture = sessionData.user.picture;
      } catch (err) {
        console.error('Session check failed:', err);
        throw err;
      }
    },

    async fetchAllStockDataOnce() {
      try {
        const response = await fetch(`${this.base_url}/api/sentiments`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const allSentiments = await response.json();

        // Update the master `this.stocks` array with the fetched data
        allSentiments.forEach(item => {
          const stock = this.stocks.find(s => s.symbol === item.stockSymbol);
          if (stock) {
            stock.dailySentiment = item.sentimentValue;
            stock.tenDayAverage = item.tenDayAverage;
            stock.percentChange = item.percentChange;
            stock.name = item.companyName || stock.name;
            stock.lastTen = item.lastTen || [];
          }
        });
      } catch (err) {
        console.error('Failed to load all stock sentiments:', err);
        throw err;
      }
    },

    async loadFollowedStocks() {
      if (!this.userEmail) return;
      try {
        const symbolsResponse = await fetch(`${this.base_url}/api/getFollowedStocks?email=${encodeURIComponent(this.userEmail)}`);
        if (!symbolsResponse.ok) throw new Error(`Could not fetch followed symbols: ${symbolsResponse.status}`);
        const followedSymbols = await symbolsResponse.json();

        // MERGE STEP: Use the master `this.stocks` data we already fetched
        this.followedStocks = followedSymbols.map(symbol => {
          const stockData = this.stocks.find(s => s.symbol === symbol);
          return this.formatStockData(symbol, stockData);
        });
      } catch (error) {
        console.error("Failed to process followed stocks:", error);
        this.loadingError = `Failed to load followed stocks. ${error.message}`;
        // Don't rethrow here, so the rest of the page can still render
      }
    },

    formatStockData(symbol, stockData) {
      // Helper to create a consistent stock object for the `followedStocks` array
      if (stockData) {
        return {
          symbol: stockData.symbol,
          name: stockData.name,
          sentimentValue: stockData.dailySentiment,
          lastTen: stockData.lastTen || [],
        };
      }
      // Fallback if data isn't in the master list for some reason
      return { symbol, name: 'N/A', sentimentValue: null, lastTen: [] };
    },

    // --- USER INTERACTION METHODS ---

    isStockFollowed(symbol) {
      return this.followedStocks.some(stock => stock.symbol === symbol);
    },

    async addStockFromAll(symbol) {
      await this.addStock(symbol);
    },

    async addStockFromInput() {
      const symbol = this.searchQuery.trim().toUpperCase();
      await this.addStock(symbol);
    },

    async addStock(symbol) {
      if (window.event) window.event.stopPropagation();
      this.addStockError = null;
      if (!symbol) return;

      if (!this.userEmail) {
        this.addStockError = 'Please log in to follow stocks.';
        return;
      }

      const stockExists = this.stocks.some(s => s.symbol === symbol);
      if (!stockExists) {
        this.addStockError = `${symbol} is not a valid stock symbol.`;
        return;
      }

      if (this.isStockFollowed(symbol)) {
        this.addStockError = `${symbol} is already followed.`;
        return;
      }

      this.searchQuery = '';
      this.showSuggestions = false;

      // Optimistic Update
      const stockToAdd = this.stocks.find(s => s.symbol === symbol);
      if (stockToAdd) {
        this.followedStocks.push(this.formatStockData(symbol, stockToAdd));
      }

      // API call in background
      try {
        this.isAddingStock = true;
        const response = await fetch(`${this.base_url}/api/followStock`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ email: this.userEmail, stockSymbol: symbol })
        });
        if (!response.ok) throw new Error(`Server responded with ${response.status}`);
      } catch (error) {
        this.addStockError = `Error following stock: ${error.message}`;
        // Rollback on failure
        this.followedStocks = this.followedStocks.filter(s => s.symbol !== symbol);
        alert(`Failed to follow ${symbol}. Please try again.`);
      } finally {
        this.isAddingStock = false;
      }
    },

    async removeStock(symbol) {
      if (window.event) window.event.stopPropagation();

      const stockIndex = this.followedStocks.findIndex(s => s.symbol === symbol);
      if (stockIndex === -1) return;

      // Optimistic Update
      const removedStock = this.followedStocks[stockIndex];
      this.followedStocks.splice(stockIndex, 1);

      // API call in background
      try {
        const response = await fetch(`${this.base_url}/api/unfollowStock`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ email: this.userEmail, stockSymbol: symbol })
        });
        if (!response.ok) throw new Error(`Failed to unfollow stock: ${response.status}`);
      } catch (error) {
        console.error("Failed to remove stock:", error);
        // Rollback on failure
        this.followedStocks.splice(stockIndex, 0, removedStock);
        alert(`Error unfollowing ${symbol}: ${error.message}`);
      }
    },

    // --- UI and Navigation Methods ---

    onInputChange() {
      this.addStockError = null;
      this.showSuggestions = true;
    },
    selectStock(symbol) {
      this.searchQuery = symbol;
      this.showSuggestions = false;
    },
    toggleDropdown() {
      this.showDropdown = !this.showDropdown;
      if (this.showDropdown) this.showSortDropdown = false;
    },
    toggleSortDropdown() {
      this.showSortDropdown = !this.showSortDropdown;
      if (this.showSortDropdown) this.showDropdown = false;
    },
    selectSort(column) {
      this.sortTable(column);
      this.showSortDropdown = false;
    },
    closeAllDropdowns() {
      this.showDropdown = false;
      this.showSortDropdown = false;
    },
    sortTable(column) {
      if (this.sortColumn === column) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortColumn = column;
        this.sortDirection = 'asc';
      }
    },
    async logout() {
      this.closeAllDropdowns();
      localStorage.clear();
      sessionStorage.clear();
      try {
        await fetch(`${this.base_url}/logout`, { method: 'GET', credentials: 'include' });
      } catch (err) {
        console.error('Network error during logout request:', err);
      } finally {
        this.$router.push('/login');
      }
    },
    goToFollowingPage() {
      this.$router.push(`/following`);
      this.closeAllDropdowns();
    },
    goToStockDetail(symbol) {
      this.$router.push(`/stock/${symbol}`);
    },
    sentimentClass(value) {
      return value >= 0 ? 'text-green-600' : 'text-red-600';
    },
  }
};
</script>
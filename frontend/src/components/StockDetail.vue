<template>
  <div class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center">

            <h1 class="text-2xl font-bold text-gray-900 flex items-center">{{ symbol }}</h1>
            <span class="ml-3 text-lg text-gray-600 flex items-center">
              {{ currentStock?.companyName || 'Loading...' }}
            </span>
          </div>
          <div class="text-right flex items-center">
            <button @click="goBack"
              class="mr-4 p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors duration-200 flex items-center">
              ← Back
            </button>
          </div>
        </div>
      </div>
    </header>
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div class="lg:col-span-2">
          <div class="bg-white rounded-xl shadow-lg p-6">
            <div class="flex justify-between items-center mb-6">
              <h3 class="text-xl font-semibold text-gray-900">
                {{ activeChart === 'current' ? 'Current Sentiment' : '10-Day Average' }} Trend
              </h3>
              <div class="flex items-center space-x-3">
                <span :class="activeChart === 'current' ? 'text-royalpurple-500 font-medium' : 'text-gray-500'"
                  class="text-sm transition-colors duration-200">
                  Current Sentiment
                </span>
                <button @click="toggleChart"
                  class="relative inline-flex items-center h-6 rounded-full w-11 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-royalpurple-500 focus:ring-offset-2"
                  :class="activeChart === 'average' ? 'bg-royalpurple-500' : 'bg-gray-400'">
                  <span
                    class="inline-block w-4 h-4 transform bg-white rounded-full transition-transform duration-200 ease-in-out"
                    :class="activeChart === 'average' ? 'translate-x-6' : 'translate-x-1'"></span>
                </button>

                <span :class="activeChart === 'average' ? 'text-royalpurple-500 font-medium' : 'text-gray-500'"
                  class="text-sm transition-colors duration-200">
                  10-Day Average
                </span>
              </div>
            </div>
            <div v-if="activeChart === 'current'"
              class="h-96 bg-gray-100 rounded-lg flex items-center justify-center relative">
              <div class="w-full h-full flex items-center justify-center">
                <Gauge :displayValue="stockData.sentimentValue" :min="-1" :max="1" class="w-full h-full" />
              </div>
              <div class="absolute top-3 right-3">
                <span
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                  Live Data
                </span>
              </div>
            </div>
            <div v-else class="w-full bg-gray-100 rounded-lg flex flex-col items-center justify-center p-4">
              <h4 class="text-center text-lg font-montserrat font-medium text-gray-800 mb-4">
                Last 10 Daily Sentiments
              </h4>
              <ApexChart type="line" :options="lineOptions" :series="[{ name: 'Sentiment', data: reversedLastTen }]"
                height="240" class="w-full" />
            </div>
          </div>
        </div>
        <div class="lg:col-span-1 space-y-6">
          <div class="bg-white rounded-xl shadow-lg p-6">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-xl text-left font-semibold text-gray-900 mb-4">Key Metrics</h3>
            </div>
            <div class="space-y-4">
              <div class="flex justify-between items-center">
                <span class="text-gray-600">Latest Sentiment</span>
                <span :class="sentimentClass(stockData.sentimentValue || 0)" class="font-semibold">
                  {{ stockData.sentimentValue !== undefined
                    ? stockData.sentimentValue.toFixed(2)
                    : '0.00'
                  }}
                </span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-gray-600">10-Day Average Sentiment</span>
                <span v-if="stockData.tenDayAverage !== null" :class="sentimentClass(stockData.tenDayAverage)"
                  class="font-semibold">
                  {{ stockData.tenDayAverage.toFixed(2) }}
                </span>
                <span v-else class="text-sm text-gray-400">—</span>
              </div>
              <div class="flex justify-between items-center" v-if="stockData.percentChange !== null">
                <span class="text-gray-600">% Change</span>
                <span :class="sentimentClass(stockData.percentChange)" class="font-semibold">
                  {{ stockData.percentChange.toFixed(2) + '%' }}
                </span>
              </div>
            </div>
          </div>
          <div class="bg-white rounded-xl shadow-lg p-6 space-y-3">
            <h3 class="text-xl font-semibold text-gray-900 mb-4">
              Recent News Affecting Sentiments
            </h3>
            <hr />
            <template v-if="stockData.url1 || stockData.url2 || stockData.url3">
              <div v-if="stockData.url1">
                <a :href="stockData.url1" target="_blank" @click.stop class="text-blue-600 hover:underline">
                  News Link 1
                </a>
              </div>
              <div v-if="stockData.url2">
                <a :href="stockData.url2" target="_blank" @click.stop class="text-blue-600 hover:underline">
                  News Link 2
                </a>
              </div>
              <div v-if="stockData.url3">
                <a :href="stockData.url3" target="_blank" @click.stop class="text-blue-600 hover:underline">
                  News Link 3
                </a>
              </div>
            </template>
            <div v-if="!stockData.url1 && !stockData.url2 && !stockData.url3" class="text-sm text-gray-500">
              No recent news links.
            </div>
          </div>
        </div>
        <div class="lg:col-span-3 bg-white rounded-xl shadow-lg p-6">
          <h3 class="text-xl font-semibold text-gray-900 mb-4">About {{ currentStock?.symbol || symbol }}</h3>
          <div class="text-gray-600 text-sm leading-relaxed">
            <p>{{ currentStock?.about || 'Company information not available.' }}</p>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-4 text-xs">
            <div>
              <span class="text-gray-500">Market:</span>
              <div class="font-medium text-gray-900">{{ currentStock?.market || 'N/A' }}</div>
            </div>
            <div>
              <span class="text-gray-500">Employees:</span>
              <div class="font-medium text-gray-900">{{ currentStock?.employees ?
                formatEmployees(currentStock.employees) :
                'N/A' }}</div>
            </div>
            <div>
              <span class="text-gray-500">Founded:</span>
              <div class="font-medium text-gray-900">{{ currentStock?.founded || 'N/A' }}</div>
            </div>
            <div>
              <span class="text-gray-500">Sector:</span>
              <div class="font-medium text-gray-900">{{ currentStock?.sector || 'N/A' }}</div>
            </div>
          </div>
        </div>
      </div>
    </main>
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
import Gauge from './Gauge.vue';
import ApexCharts from 'vue3-apexcharts';

export default {
  name: 'StockDetail',
  props: ['symbol'],
  components: {
    Gauge,
    ApexChart: ApexCharts
  },
  data() {
    return {
      userEmail: localStorage.getItem('userEmail') || '',
      API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
      isFollowed: false,
      isFollowing: false,
      stockData: {
        stockSymbol: '',
        companyName: '',
        sentimentValue: 0,
        tenDayAverage: null,
        percentChange: null,
        sentimentTimestamp: null,
        url1: '',
        url2: '',
        url3: '',
        llmAnalysis: '',
        lastTen: [],
      },
      activeChart: 'average',
      showAIModal: false,
      stocks: [
        {
          symbol: 'AAPL',
          companyName: 'Apple Inc.',
          about: 'Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, and accessories. The company is known for its innovative consumer electronics including the iPhone, iPad, Mac computers, and Apple Watch. Apple also operates a comprehensive ecosystem of services including the App Store, iCloud, Apple Music, and various subscription offerings.',
          founded: '1976',
          employees: '160,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'QCOM',
          companyName: 'Qualcomm Incorporated',
          about: 'Qualcomm Incorporated is a global leader in wireless technology and a pioneer in 5G development. The company designs and markets semiconductors, software, and services for mobile devices, networking equipment, and other wireless applications. Qualcomm\'s Snapdragon processors are widely used in smartphones, and its licensing business generates significant revenue from its extensive patent portfolio.',
          founded: '1985',
          employees: '50,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'AMD',
          companyName: 'Advanced Micro Devices',
          about: 'Advanced Micro Devices, Inc. (AMD) is a multinational semiconductor company that develops computer processors and related technologies for business and consumer markets. AMD\'s products include microprocessors, chipsets, graphics processing units (GPUs) for servers, workstations, personal computers, and embedded systems. The company is a major competitor to Intel and NVIDIA in its respective markets.',
          founded: '1969',
          employees: '25,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'AMZN',
          companyName: 'Amazon.com Inc.',
          about: 'Amazon.com Inc. is a multinational technology company focusing on e-commerce, cloud computing, digital streaming, and artificial intelligence. It is one of the largest online retailers globally and a leading provider of cloud services through Amazon Web Services (AWS). Amazon also produces consumer electronics like Kindle e-readers and Echo devices and operates various other ventures including groceries and entertainment.',
          founded: '1994',
          employees: '1,500,000',
          market: 'NASDAQ',
          sector: 'Consumer Discretionary'
        },
        {
          symbol: 'MU',
          companyName: 'Micron Technology Inc.',
          about: 'Micron Technology Inc. is a global leader in innovative memory and storage solutions. The company manufactures and markets dynamic random-access memory (DRAM) products, NAND flash memory, and NOR flash memory, used in a wide range of applications including computing, networking, mobile, and embedded solutions. Micron\'s products are essential components for various electronic devices and data centers.',
          founded: '1978',
          employees: '49,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'GME',
          companyName: 'GameStop Corp.',
          about: 'GameStop Corp. is a specialty retailer of video games, consumer electronics, and gaming merchandise. The company operates retail stores and e-commerce properties, offering new and pre-owned video game hardware, physical and digital game software, accessories, and collectibles. GameStop has been a prominent subject in discussions about retail investing and short squeezes.',
          founded: '1984',
          employees: '11,000',
          market: 'NYSE',
          sector: 'Consumer Discretionary'
        },
        {
          symbol: 'SOFI',
          companyName: 'SoFi Technologies Inc.',
          about: 'SoFi Technologies Inc. is a personal finance company that offers a range of financial products and services. These include student loan refinancing, personal loans, home loans, and investment platforms. SoFi aims to be a one-stop shop for its members\' financial needs, providing banking, lending, and advisory services primarily through its digital platform.',
          founded: '2011',
          employees: '4,000',
          market: 'NASDAQ',
          sector: 'Financials'
        },
        {
          symbol: 'NFLX',
          companyName: 'Netflix Inc.',
          about: 'Netflix Inc. is a global entertainment company and the world\'s leading streaming service. It provides a vast library of films and television series, including original productions, across a wide variety of genres and languages. Netflix operates on a subscription-based model, offering on-demand content accessible on various internet-connected devices.',
          founded: '1997',
          employees: '13,000',
          market: 'NASDAQ',
          sector: 'Communication Services'
        },
        {
          symbol: 'GOOGL',
          companyName: 'Alphabet Inc.',
          about: 'Alphabet Inc. is a multinational technology conglomerate and the parent company of Google. Its core businesses include search (Google Search), advertising (Google Ads), cloud computing (Google Cloud), and various other technology ventures. Alphabet also has significant investments in areas like autonomous driving (Waymo) and life sciences (Verily and Calico).',
          founded: '1998 (as Google)',
          employees: '180,000',
          market: 'NASDAQ',
          sector: 'Communication Services'
        },
        {
          symbol: 'DIS',
          companyName: 'Walt Disney Co.',
          about: 'The Walt Disney Company is a diversified multinational mass media and entertainment conglomerate. The company operates through various segments, including Media and Entertainment Distribution (streaming services, television networks, studios) and Disney Parks, Experiences and Products (theme parks, resorts, merchandise). Disney is renowned for its animation, film studios, and iconic characters.',
          founded: '1923',
          employees: '220,000',
          market: 'NYSE',
          sector: 'Communication Services'
        },
        {
          symbol: 'INTC',
          companyName: 'Intel Corporation',
          about: 'Intel Corporation is a global leader in the design and manufacturing of microprocessors for personal computers and data center servers. The company also produces chipsets, embedded processors, and other computing and communications-related products. Intel plays a crucial role in powering much of the world\'s computing infrastructure and is actively expanding into new areas like AI and autonomous driving.',
          founded: '1968',
          employees: '130,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'META',
          companyName: 'Meta Platforms Inc.',
          about: 'Meta Platforms Inc., formerly Facebook Inc., is a multinational technology conglomerate. The company\'s primary products include Facebook, Instagram, WhatsApp, and Messenger. Meta is heavily investing in the development of the metaverse, virtual reality (VR), and augmented reality (AR) technologies, aiming to build the next generation of social experiences.',
          founded: '2004 (as Facebook)',
          employees: '77,000',
          market: 'NASDAQ',
          sector: 'Communication Services'
        },
        {
          symbol: 'MSFT',
          companyName: 'Microsoft Corporation',
          about: 'Microsoft Corporation is a multinational technology company known for its software products, including the Windows operating system, Microsoft Office suite, and Internet Explorer web browser. The company has diversified into various other areas, including cloud computing (Azure), gaming (Xbox), hardware (Surface devices), and artificial intelligence. Microsoft is a dominant player in enterprise software and cloud services.',
          founded: '1975',
          employees: '220,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'NVDA',
          companyName: 'NVIDIA Corporation',
          about: 'NVIDIA Corporation is a leading designer of graphics processing units (GPUs) for the gaming, professional visualization, data center, and automotive markets. NVIDIA\'s GPUs are widely used for demanding applications like artificial intelligence, deep learning, and high-performance computing. The company is at the forefront of innovation in accelerated computing and AI platforms.',
          founded: '1993',
          employees: '26,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'ORCL',
          companyName: 'Oracle Corporation',
          about: 'Oracle Corporation is a multinational computer technology corporation. The company is best known for its database software and cloud engineered systems. Oracle develops and markets enterprise software products, including database management systems, enterprise resource planning (ERP) software, customer relationship management (CRM) software, and supply chain management (SCM) software, primarily for business customers.',
          founded: '1977',
          employees: '164,000',
          market: 'NYSE',
          sector: 'Technology'
        },
        {
          symbol: 'WBD',
          companyName: 'Warner Bros. Discovery Inc.',
          about: 'Warner Bros. Discovery Inc. is a global media and entertainment company. It was formed from the merger of WarnerMedia and Discovery Inc. The company\'s portfolio includes a vast collection of iconic brands, studios, and streaming services, encompassing film, television, news, and sports content. Key assets include Warner Bros. Pictures, HBO, CNN, and Discovery Channel.',
          founded: '2022 (merger)',
          employees: '30,000',
          market: 'NASDAQ',
          sector: 'Communication Services'
        },
        {
          symbol: 'PLTR',
          companyName: 'Palantir Technologies',
          about: 'Palantir Technologies Inc. is a software company specializing in big data analytics. The company builds and deploys software platforms for the intelligence community, enabling organizations to integrate, manage, and secure their data. Palantir\'s platforms, Gotham and Foundry, are used by government agencies and large enterprises for complex data analysis and decision-making.',
          founded: '2003',
          employees: '3,800',
          market: 'NYSE',
          sector: 'Technology'
        },
        {
          symbol: 'PYPL',
          companyName: 'PayPal Holdings Inc.',
          about: 'PayPal Holdings Inc. is a technology platform that enables digital and mobile payments globally. It operates as an online payment system supporting online money transfers and serves as an electronic alternative to traditional paper methods like checks and money orders. PayPal offers a range of services including merchant processing, peer-to-peer payments, and credit products.',
          founded: '1998',
          employees: '29,000',
          market: 'NASDAQ',
          sector: 'Financials'
        },
        {
          symbol: 'RBLX',
          companyName: 'Roblox Corporation',
          about: 'Roblox Corporation is a global platform that brings millions of people together through shared experiences. The company operates an online platform where users can create, play, and monetize their own 3D experiences and games. Roblox is popular among younger audiences and has its own virtual currency, Robux, which can be earned and spent within the platform.',
          founded: '2004',
          employees: '2,400',
          market: 'NYSE',
          sector: 'Communication Services'
        },
        {
          symbol: 'LCID',
          companyName: 'Lucid Group Inc.',
          about: 'Lucid Group Inc. is an American electric vehicle manufacturer headquartered in Newark, California. The company designs, develops, manufactures, and sells luxury electric vehicles, including its flagship Lucid Air sedan. Lucid aims to set new standards in electric vehicle technology, focusing on range, performance, and design.',
          founded: '2007',
          employees: '6,000',
          market: 'NASDAQ',
          sector: 'Consumer Discretionary'
        },
        {
          symbol: 'SNAP',
          companyName: 'Snap Inc.',
          about: 'Snap Inc. is a camera company. The company\'s flagship product, Snapchat, is a popular social media application that allows users to share photos and videos with friends, augmented with various filters and lenses. Snap also develops and sells hardware products like Spectacles and offers advertising solutions to businesses.',
          founded: '2011',
          employees: '6,500',
          market: 'NYSE',
          sector: 'Communication Services'
        },
        {
          symbol: 'SHOP',
          companyName: 'Shopify Inc.',
          about: 'Shopify Inc. is a multinational e-commerce company that provides a leading e-commerce platform for online stores and retail point-of-sale systems. The company\'s platform helps businesses of all sizes to set up, run, and grow their online businesses, offering tools for website creation, marketing, payments, and shipping.',
          founded: '2006',
          employees: '11,000',
          market: 'NYSE',
          sector: 'Technology'
        },
        {
          symbol: 'SPOT',
          companyName: 'Spotify Technology SA',
          about: 'Spotify Technology S.A. is a Swedish audio streaming and media services provider. It is one of the world\'s largest music streaming service providers, offering a vast library of music and podcasts to its users. Spotify operates on a freemium model, offering both free ad-supported and premium subscription tiers.',
          founded: '2006',
          employees: '9,000',
          market: 'NYSE',
          sector: 'Communication Services'
        },
        {
          symbol: 'COIN',
          companyName: 'Coinbase Global Inc.',
          about: 'Coinbase Global Inc. operates as a cryptocurrency exchange platform. The company provides a secure and user-friendly platform for buying, selling, transferring, and storing cryptocurrencies like Bitcoin, Ethereum, and others. Coinbase serves both retail users and institutions, offering a range of products including trading, custody, and staking services.',
          founded: '2012',
          employees: '3,700',
          market: 'NASDAQ',
          sector: 'Financials'
        },
        {
          symbol: 'TSLA',
          companyName: 'Tesla Inc.',
          about: 'Tesla Inc. is an American automotive and clean energy company. The company designs and manufactures electric vehicles, battery energy storage from home to grid-scale, solar panels and related products, and other associated services. Tesla is a pioneer in electric vehicle technology and autonomous driving, with a mission to accelerate the world\'s transition to sustainable energy.',
          founded: '2003',
          employees: '140,000',
          market: 'NASDAQ',
          sector: 'Consumer Discretionary'
        },
        {
          symbol: 'BA',
          companyName: 'Boeing Company',
          about: 'The Boeing Company is a multinational corporation that designs, manufactures, and sells airplanes, rotorcraft, rockets, satellites, telecommunications equipment, and missiles worldwide. Boeing is one of the largest global aircraft manufacturers and a major defense contractor, providing commercial and military aircraft, as well as space and security systems.',
          founded: '1916',
          employees: '170,000',
          market: 'NYSE',
          sector: 'Industrials'
        },
        {
          symbol: 'AVGO',
          companyName: 'Broadcom Inc.',
          about: 'Broadcom Inc. is a global infrastructure technology company that designs, develops, and supplies a broad range of semiconductor and infrastructure software solutions. Its products are used in various markets, including data center, broadband communication, enterprise software, storage, and industrial. Broadcom is a key supplier for networking, broadband, and wireless communication industries.',
          founded: '1991',
          employees: '20,000',
          market: 'NASDAQ',
          sector: 'Technology'
        },
        {
          symbol: 'UBER',
          companyName: 'Uber Technologies Inc.',
          about: 'Uber Technologies Inc. is a technology company that provides ride-sharing, food delivery (Uber Eats), and freight transportation services. The company connects riders with drivers, and customers with restaurants and grocery stores through its mobile applications. Uber operates in numerous cities worldwide, transforming urban mobility and delivery services.',
          founded: '2009',
          employees: '33,000',
          market: 'NYSE',
          sector: 'Industrials'
        },
        {
          symbol: 'ZOOM',
          companyName: 'Zoom Video Communications',
          about: 'Zoom Video Communications Inc. is a communications technology company that provides a cloud-based peer-to-peer software platform for video telephony and online chat services. Zoom\'s services include video meetings, voice calls, webinars, and chat, widely used for remote work, education, and social interactions.',
          founded: '2011',
          employees: '8,000',
          market: 'NASDAQ',
          sector: 'Technology'
        }
      ]
    };
  },
  computed: {
    currentStock() {
      return this.stocks.find(stock => stock.symbol === this.symbol) || null;
    },
    reversedLastTen() {
      return [...this.stockData.lastTen].reverse();
    },
    lineOptions() {
      const today = new Date();
      const categories = this.stockData.lastTen.map((_, idx) => {
        const offset = this.stockData.lastTen.length - 1 - idx;
        const d = new Date(today);
        d.setDate(today.getDate() - offset);
        return d.toLocaleDateString('en-US', { month: 'short', day: '2-digit' });
      });
      return {
        chart: {
          id: 'last-ten-sentiment-chart',
          toolbar: { show: false },
          zoom: { enabled: false }
        },
        colors: ['#543ade'],
        xaxis: {
          categories,
          title: { text: 'Date' },
          labels: { align: 'center' }
        },
        yaxis: {
          min: -1,
          max: 1,
          tickAmount: 4,
          title: { text: 'Sentiment Value' }
        },
        stroke: { curve: 'smooth' },
        markers: { size: 4 },
        tooltip: {
          x: { show: false },
          y: { formatter: val => val.toFixed(2) }
        }
      };
    }
  },
  mounted() {
    this.loadStockData();
    this.checkIfFollowed();
  },
  watch: {
    symbol() {
      this.loadStockData();
    }
  },
  methods: {
    async checkIfFollowed() {
      const userEmail = localStorage.getItem('userEmail');
      if (!userEmail) {
        this.isFollowed = false;
        return;
      }
      try {
        const response = await fetch(
          `${this.API_BASE_URL}/getFollowedStocks?email=${encodeURIComponent(userEmail)}`
        );
      } catch (error) {
        console.error('Error checking followed status:', error);
      }
    },

    async toggleFollowStock() {
      const userEmail = localStorage.getItem('userEmail');
      if (!userEmail) {
        alert('Please log in to follow stocks');
        this.$router.push('/login'); // Redirect to login
        return;
      }
      if (this.isFollowing) return;

      this.isFollowing = true;
      try {
        if (this.isFollowed) {
          await this.unfollowStock();
        } else {
          await this.followStock();
        }
      } catch (error) {
        console.error('Error toggling follow status:', error);
      } finally {
        this.isFollowing = false;
      }
    },

    async followStock() {
      const userEmail = localStorage.getItem('userEmail');
      if (!userEmail) return;
      const response = await fetch(`${this.API_BASE_URL}/followStock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: this.userEmail,
          stockSymbol: this.symbol
        })
      });

      if (!response.ok) throw new Error('Failed to follow stock');

      this.isFollowed = true;
    },

    async unfollowStock() {
      const response = await fetch(`${this.API_BASE_URL}/unfollowStock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: this.userEmail,
          stockSymbol: this.symbol
        })
      });

      if (!response.ok) throw new Error('Failed to unfollow stock');

      this.isFollowed = false;
    },
    async loadStockData() {
      try {
        const res = await fetch(`${this.API_BASE_URL}/api/sentiments`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const all = await res.json();
        const found = all.find(o => o.stockSymbol === this.symbol);

        if (found) {
          const extractUrl = (urlString) => {
            if (!urlString || typeof urlString !== 'string') return '';
            const parts = urlString.split(/:\s*(.*)/s);
            return parts[1] || '';
          };

          this.stockData = {
            stockSymbol: found.stockSymbol,
            companyName: found.companyName || 'Unknown Company',
            sentimentValue: found.sentimentValue ?? 0,
            tenDayAverage: found.tenDayAverage ?? null,
            percentChange: found.percentChange ?? null,
            sentimentTimestamp: found.sentimentTimestamp || null,
            url1: extractUrl(found.url1),
            url2: extractUrl(found.url2),
            url3: extractUrl(found.url3),
            llmAnalysis: found.llmAnalysis || '',
            lastTen: Array.isArray(found.lastTen) ? found.lastTen : []
          };
        } else {
          this.stockData.lastTen = [];
        }
      } catch (e) {
        console.error(e);
        this.stockData.lastTen = [];
      }
    },
    sentimentClass(v) {
      return v >= 0 ? 'text-green-600' : 'text-red-600';
    },
    toggleChart() {
      this.activeChart = this.activeChart === 'current' ? 'average' : 'current';
    },
    goBack() {
      this.$router.go(-1);
    },
    formatEmployees(employees) {
      const num = employees.replace(/,/g, '');
      return parseInt(num).toLocaleString();
    }
  }
};
</script>

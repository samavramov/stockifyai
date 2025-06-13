<template>
  <div class="min-h-screen bg-white">
    <nav class="w-full bg-white shadow flex items-center justify-between px-4 sm:px-6 ">
      <img
        src="/Users/samavramov/stockdashboard/frontend/src/images/logo.png"
        alt="Stockify AI Logo" class="h-20 sm:h-20 md:h-20 object-contain" />
      <div class="flex items-center space-x-2 sm:space-x-4">
        <button @click="goToLogin"
          class="bg-royalpurple-500 text-white text-sm px-3 py-1.5 sm:px-4 sm:py-2 rounded-lg hover:bg-blue-900 transition-colors duration-200 flex items-center space-x-1 sm:space-x-2">
          <span class="text-xs sm:text-base">Sign In</span>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
            stroke="currentColor" class="size-4 sm:size-5 md:size-6">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M17.982 18.725A7.488 7.488 0 0 0 12 15.75a7.488 7.488 0 0 0-5.982 2.975m11.963 0a9 9 0 1 0-11.963 0m11.963 0A8.966 8.966 0 0 1 12 21a8.966 8.966 0 0 1-5.982-2.275M15 9.75a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
          </svg>
        </button>
      </div>
    </nav>
    <div id="section-nav" class="hidden md:flex">
      <span class="nav-dot" data-target-id="hero-section"></span>
      <span class="nav-dot" data-target-id="why-section"></span>
      <span class="nav-dot" data-target-id="how-to-section"></span>
      <span class="nav-dot" data-target-id="ready-section"></span>
    </div>
    <section id="hero-section" class="relative bg-black text-white min-h-screen md:h-screen">
      <div class="absolute inset-0 z-0">
        <canvas id="hero-background"></canvas>
      </div>
      <div class="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 md:py-24">
        <div class="text-center">
          <h1 class="text-3xl md:text-5xl lg:text-6xl font-bold mb-4 md:mb-6">
            Decode Market Sentiment With
            <span class="text-transparent font-merriweather bg-clip-text bg-gradient-to-r from-blue-400 to-purple-400 block mt-2 pb-2">
              stockify.ai
            </span>
          </h1>
          <p class="text-base md:text-xl lg:text-2xl text-blue-200 mb-6 md:mb-8 max-w-3xl mx-auto">
            Harness the power of AI-driven sentiment analysis to make smarter investment decisions.
          </p>
          <button @click="goToLogin"
            class="bg-gradient-to-r from-blue-500 to-purple-600 text-white px-6 py-3 md:px-8 md:py-4 rounded-xl text-sm md:text-base font-semibold hover:from-blue-600 hover:to-purple-700 transform hover:scale-105 transition-all duration-200 shadow-2xl">
            Get Started Today
          </button>
        </div>
      </div>
      <div class="scroll-down-arrow hidden md:block">&#8659;</div>
    </section>
    <section id="why-section" class="py-10 md:py-20 bg-gray-50 min-h-screen md:h-screen">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-10 md:mb-16">
          <h2 class="text-2xl md:text-3xl lg:text-4xl font-bold text-gray-900 mb-3 md:mb-4">
            Why Stock Sentiment Analysis Matters
          </h2>
          <p class="text-base md:text-xl text-gray-600 max-w-3xl mx-auto">
            Understanding market sentiment gives you a competitive edge in predicting price movements and market trends.
          </p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
          <div
            class="bg-white rounded-xl md:rounded-2xl p-6 md:p-8 shadow-lg hover:shadow-xl transition-shadow duration-300">
            <div class="flex items-center mb-3 md:mb-4 space-x-2 md:space-x-3">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6 md:w-8 md:h-8 text-royalpurple-500 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M7.5 14.25v2.25m3-4.5v4.5m3-6.75v6.75m3-9v9M6 20.25h12A2.25 2.25 0 0 0 20.25 18V6A2.25 2.25 0 0 0 18 3.75H6A2.25 2.25 0 0 0 3.75 6v12A2.25 2.25 0 0 0 6 20.25Z" />
              </svg>
              <h3 class="text-xl md:text-2xl font-semibold text-gray-900">Predict Market Shifts</h3>
            </div>
            <p class="text-sm md:text-base text-gray-600">
              Sentiment often moves faster than price. By analyzing news, social media, and market commentary, you can
              anticipate price changes before they happen.
            </p>
          </div>
          <div
            class="bg-white rounded-xl md:rounded-2xl p-6 md:p-8 shadow-lg hover:shadow-xl transition-shadow duration-300">
            <div class="flex items-center mb-3 md:mb-4 space-x-2 md:space-x-3">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6 md:w-8 md:h-8 text-royalpurple-500 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z" />
              </svg>
              <h3 class="text-xl md:text-2xl font-semibold text-gray-900">Risk Management</h3>
            </div>
            <p class="text-sm md:text-base text-gray-600">
              Identify potential market volatility and downtrends early. Negative sentiment spikes often precede
              significant price corrections.
            </p>
          </div>
          <div
            class="bg-white rounded-xl md:rounded-2xl p-6 md:p-8 shadow-lg hover:shadow-xl transition-shadow duration-300">
            <div class="flex items-center mb-3 md:mb-4 space-x-2 md:space-x-3">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6 md:w-8 md:h-8 text-royalpurple-500 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" />
              </svg>
              <h3 class="text-xl md:text-2xl font-semibold text-gray-900">Real-Time Insights</h3>
            </div>
            <p class="text-sm md:text-base text-gray-600">
              Get sentiment analysis across multiple data sources. React quickly to daily news and
              market-moving events.
            </p>
          </div>
        </div>
      </div>
      <div class="scroll-up-arrow hidden md:block">&#8657;</div>
      <div class="scroll-down-arrow hidden md:block">&#8659;</div>
    </section>
    <section id="how-to-section"
      class="py-10 md:py-20 bg-gradient-to-r from-blue-600 to-purple-600 text-white min-h-screen md:h-screen">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-10 md:mb-16">
          <h2 class="text-2xl md:text-3xl lg:text-4xl font-bold text-white mb-3 md:mb-4">
            How to Use Sentiment Analysis
          </h2>
          <p class="text-base md:text-xl text-white max-w-3xl mx-auto">
            Transform raw market sentiment into actionable trading strategies.
          </p>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 md:gap-12 items-center">
          <div class="space-y-6 md:space-y-8">
            <div class="flex items-start space-x-3 md:space-x-4">
              <div
                class="flex-shrink-0 w-6 h-6 md:w-8 md:h-8 bg-white text-black rounded-full flex items-center justify-center font-semibold">
                1
              </div>
              <div>
                <h3 class="text-lg md:text-xl font-semibold text-white mb-1 md:mb-2">Monitor Daily Sentiment</h3>
                <p class="text-sm md:text-base">
                  Track sentiment scores for your watchlist. Look for sudden sentiment shifts that may
                  indicate upcoming price movements.
                </p>
              </div>
            </div>
            <div class="flex items-start space-x-3 md:space-x-4">
              <div
                class="flex-shrink-0 w-6 h-6 md:w-8 md:h-8 bg-white text-black rounded-full flex items-center justify-center font-semibold">
                2
              </div>
              <div>
                <h3 class="text-lg md:text-xl font-semibold text-white mb-1 md:mb-2">Analyze Trends</h3>
                <p class="text-sm md:text-base">
                  Compare current sentiment to 10-day averages. Divergences between sentiment and price often signal
                  reversal points.
                </p>
              </div>
            </div>
            <div class="flex items-start space-x-3 md:space-x-4">
              <div
                class="flex-shrink-0 w-6 h-6 md:w-8 md:h-8 bg-white text-black rounded-full flex items-center justify-center font-semibold">
                3
              </div>
              <div>
                <h3 class="text-lg md:text-xl font-semibold text-white mb-1 md:mb-2">Make Informed Decisions</h3>
                <p class="text-sm md:text-base">
                  Combine sentiment data with technical analysis and fundamental research for a comprehensive investment
                  strategy.
                </p>
              </div>
            </div>
          </div>
          <div class="bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl md:rounded-2xl p-6 md:p-8">
            <div class="text-center">
              <div class="text-4xl md:text-6xl mb-3 md:mb-4">🧠</div>
              <h3 class="text-xl md:text-2xl font-semibold text-gray-900 mb-3 md:mb-4">AI-Powered Analysis</h3>
              <p class="text-sm md:text-base text-gray-600 mb-4 md:mb-6">
                Our advanced algorithms process thousands of data points to deliver accurate sentiment scores.
              </p>
              <div class="grid grid-cols-2 gap-3 text-xs md:text-sm">
                <div class="bg-white rounded-lg p-2 md:p-3">
                  <div class="font-semibold text-gray-900">News Analysis</div>
                  <div class="text-gray-600">Real-time processing</div>
                </div>
                <div class="bg-white rounded-lg p-2 md:p-3">
                  <div class="font-semibold text-gray-900">Analyst Reports</div>
                  <div class="text-gray-600">Professional insights</div>
                </div>
                <div class="bg-white rounded-lg p-2 md:p-3">
                  <div class="font-semibold text-gray-900">Updated Frequently</div>
                  <div class="text-gray-600">Daily Calculations</div>
                </div>
                <div class="bg-white rounded-lg p-2 md:p-3">
                  <div class="font-semibold text-gray-900">Trend Visualizations</div>
                  <div class="text-gray-600">10-day Average Sentiments</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="scroll-up-arrow hidden md:block">&#8657;</div>
      <div class="scroll-down-arrow hidden md:block">&#8659;</div>
    </section>
    <section id="ready-section" class="py-10 md:py-20 bg-black text-black min-h-screen md:h-screen">
      <div class="scroll-up-arrow hidden md:block text-white">&#8657;</div>
      <div class="w-full max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8 px-4">
        <div
          class="bg-white/10 backdrop-blur-lg rounded-xl md:rounded-2xl p-6 md:p-8 shadow-2xl border border-white/20 text-white flex flex-col justify-center items-center space-y-4 md:space-y-6">
          <h2 class="text-2xl md:text-3xl lg:text-4xl font-bold mb-2 md:mb-4 text-center">
            Ready to Transform Your Trading?
          </h2>
          <p class="text-base md:text-xl text-blue-100 mb-4 md:mb-8 text-center">
            Join thousands of traders using sentiment analysis to gain a competitive edge.
          </p>
          <button @click="goToLogin"
            class="w-full max-w-xs flex items-center justify-center bg-white text-gray-700 py-2 md:py-3 px-4 rounded-lg font-semibold shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 mb-3 md:mb-4">
            Get Started Today
          </button>
        </div>
        <div
          class="bg-white/10 backdrop-blur-lg rounded-xl md:rounded-2xl p-6 md:p-8 shadow-2xl border border-white/20 text-white">
          <h2 class="text-xl md:text-2xl lg:text-3xl font-semibold mb-3 md:mb-4">Why Get Started?</h2>
          <ul class="space-y-2 md:space-y-3 text-blue-100 text-sm md:text-base">
            <li class="flex items-start hover:text-white transition-colors duration-200">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-5 h-5 mr-2 flex-shrink-0 mt-0.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
              </svg>
              <p><strong>Starting today</strong> puts you ahead of the curve</p>
            </li>
            <li class="flex items-start hover:text-white transition-colors duration-200">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-5 h-5 mr-2 flex-shrink-0 mt-0.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5m-9-6h.008v.008H12v-.008ZM12 15h.008v.008H12V15Zm0 2.25h.008v.008H12v-.008ZM9.75 15h.008v.008H9.75V15Zm0 2.25h.008v.008H9.75v-.008ZM7.5 15h.008v.008H7.5V15Zm0 2.25h.008v.008H7.5v-.008Zm6.75-4.5h.008v.008h-.008v-.008Zm0 2.25h.008v.008h-.008V15Zm0 2.25h.008v.008h-.008v-.008Zm2.25-4.5h.008v.008H16.5v-.008Zm0 2.25h.008v.008H16.5V15Z" />
              </svg>
              <p><strong>Every day counts:</strong> Build your portfolio with insights</p>
            </li>
            <li class="flex items-start hover:text-white transition-colors duration-200">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-5 h-5 mr-2 flex-shrink-0 mt-0.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4.26 10.147a60.438 60.438 0 0 0-.491 6.347A48.62 48.62 0 0 1 12 20.904a48.62 48.62 0 0 1 8.232-4.41 60.46 60.46 0 0 0-.491-6.347m-15.482 0a50.636 50.636 0 0 0-2.658-.813A59.906 59.906 0 0 1 12 3.493a59.903 59.903 0 0 1 10.399 5.84c-.896.248-1.783.52-2.658.814m-15.482 0A50.717 50.717 0 0 1 12 13.489a50.702 50.702 0 0 1 7.74-3.342M6.75 15a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm0 0v-3.675A55.378 55.378 0 0 1 12 8.443m-7.007 11.55A5.981 5.981 0 0 0 6.75 15.75v-1.5" />
              </svg>
              <p><strong>Learn:</strong> Start tracking stocks that matter to you</p>
            </li>
            <li class="flex items-start hover:text-white transition-colors duration-200">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-5 h-5 mr-2 flex-shrink-0 mt-0.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M13.5 10.5V6.75a4.5 4.5 0 1 1 9 0v3.75M3.75 21.75h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H3.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" />
              </svg>
              <p><strong>Completely free:</strong> No subscriptions or fees</p>
            </li>
          </ul>
        </div>
      </div>
    </section>
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
  </div>
</template>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow-x: hidden;
  scroll-behavior: smooth;
}

#app,
.min-h-screen {
  min-height: 100vh;
}

section {
  min-height: 100vh;
  height: auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  box-sizing: border-box;
  padding: 20px;
  overflow: hidden;
}

@media (min-width: 768px) {
  section {
    height: 100vh;
  }
}

.scroll-up-arrow,
.scroll-down-arrow {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 2.5em;
  cursor: pointer;
  z-index: 20;
  opacity: 0.7;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.scroll-up-arrow:hover,
.scroll-down-arrow:hover {
  opacity: 1;
  transform: translateX(-50%) scale(1.1);
}

.scroll-down-arrow {
  bottom: 20px;
}

.scroll-up-arrow {
  top: 20px;
}

#section-nav {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 100;
}

.nav-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.2s ease;
}

.nav-dot:hover {
  background-color: rgba(255, 255, 255, 0.6);
  transform: scale(1.1);
}

.nav-dot.active {
  background-color: #a78bfa;
  border-color: #a78bfa;
  transform: scale(1.2);
}
</style>

<script>
import { onMounted, onBeforeUnmount } from 'vue';
export default {
  name: 'Landing',
  methods: {
    goToLogin() {
      this.$router.push('/login');
    }
  },
  setup() {
    let resizeObserver;
    let animationFrameId;
    let observer;
    onMounted(() => {
      const canvas = document.getElementById('hero-background');
      const heroSection = canvas ? canvas.closest('section') : null;
      let ctx;
      let width, height;
      let particles = [];
      let numParticles;

      class Particle {
        constructor() {
          this.x = Math.random() * width;
          this.y = Math.random() * height;
          this.radius = Math.random() * 2.5 + 1;
          this.vx = (Math.random() - 0.5) * 0.3;
          this.vy = (Math.random() - 0.5) * 0.3;
          this.alpha = Math.random() * 0.4 + 0.3;
          this.phase = Math.random() * Math.PI * 2;
        }
        update() {
          this.x += this.vx;
          this.y += this.vy;
          if (this.x + this.radius > width || this.x - this.radius < 0) {
            this.vx *= -1;
          }
          if (this.y + this.radius > height || this.y - this.radius < 0) {
            this.vy *= -1;
          }
          const speed = 0.01;
          const r = 130 + Math.sin(this.phase + Date.now() * speed) * 40;
          const g = 100;
          const b = 180 + Math.cos(this.phase + Date.now() * speed) * 75;
          this.color = `rgba(${r}, ${g}, ${b}, ${this.alpha})`;
        }
        draw() {
          ctx.beginPath();
          ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
          ctx.fillStyle = this.color;
          ctx.fill();
        }
      }

      function initParticles() {
        if (!canvas || !heroSection) return;
        ctx = canvas.getContext('2d');
        width = heroSection.clientWidth;
        height = heroSection.clientHeight;
        canvas.width = width;
        canvas.height = height;
        particles.length = 0;
        numParticles = (width * height) / 9000;
        for (let i = 0; i < numParticles; i++) {
          particles.push(new Particle());
        }
      }

      function animateParticles() {
        if (!ctx) return;
        animationFrameId = requestAnimationFrame(animateParticles);
        ctx.clearRect(0, 0, width, height);
        for (let i = 0; i < particles.length; i++) {
          for (let j = i; j < particles.length; j++) {
            const dx = particles[i].x - particles[j].x;
            const dy = particles[i].y - particles[j].y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < 120) {
              ctx.beginPath();
              ctx.strokeStyle = `rgba(190, 170, 255, ${1 - distance / 120})`;
              ctx.lineWidth = 0.6;
              ctx.moveTo(particles[i].x, particles[i].y);
              ctx.lineTo(particles[j].x, particles[j].y);
              ctx.stroke();
            }
          }
        }
        particles.forEach(particle => {
          particle.update();
          particle.draw();
        });
      }

      if (canvas && heroSection) {
        initParticles();
        animateParticles();
        resizeObserver = new ResizeObserver(() => {
          initParticles();
        });
        resizeObserver.observe(heroSection);
      }

      const sections = document.querySelectorAll('section');

      document.querySelectorAll('.scroll-down-arrow').forEach(arrow => {
        arrow.addEventListener('click', (event) => {
          const currentSection = event.target.closest('section');
          const currentIndex = Array.from(sections).indexOf(currentSection);
          if (currentIndex < sections.length - 1) {
            sections[currentIndex + 1].scrollIntoView({ behavior: 'smooth' });
          }
        });
      });

      // Handle scroll-up arrows
      document.querySelectorAll('.scroll-up-arrow').forEach(arrow => {
        arrow.addEventListener('click', (event) => {
          const currentSection = event.target.closest('section');
          const currentIndex = Array.from(sections).indexOf(currentSection);

          // Specific handling for the scroll-up arrow in "ready-section"
          if (currentSection.id === 'ready-section') {
            const howToSection = document.getElementById('how-to-section');
            if (howToSection) {
              howToSection.scrollIntoView({ behavior: 'smooth' });
            }
          } else if (currentIndex > 0) {
            // Default behavior for other scroll-up arrows
            sections[currentIndex - 1].scrollIntoView({ behavior: 'smooth' });
          }
        });
      });

      const navDots = document.querySelectorAll('.nav-dot');
      observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          const targetId = entry.target.id;
          const correspondingDot = document.querySelector(`.nav-dot[data-target-id="${targetId}"]`);
          if (correspondingDot) {
            if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
              navDots.forEach(dot => dot.classList.remove('active'));
              correspondingDot.classList.add('active');
            } else {
              // Optionally remove active class if not intersecting enough
            }
          }
        });
      }, {
        threshold: 0.5
      });

      sections.forEach(section => {
        observer.observe(section);
      });

      navDots.forEach(dot => {
        dot.addEventListener('click', () => {
          const targetSectionId = dot.dataset.targetId;
          const targetSection = document.getElementById(targetSectionId);
          if (targetSection) {
            targetSection.scrollIntoView({ behavior: 'smooth' });
          }
        });
      });
    });

    onBeforeUnmount(() => {
      if (animationFrameId) {
        cancelAnimationFrame(animationFrameId);
      }
      if (resizeObserver) {
        resizeObserver.disconnect();
      }
      if (observer) {
        observer.disconnect();
      }
    });

    return {};
  }
};
</script>
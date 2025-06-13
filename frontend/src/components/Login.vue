<template>
  <div
    class="min-h-screen bg-gradient-to-br from-blue-900 via-purple-900 to-indigo-900 flex flex-col items-center justify-center px-4">
    <div class="text-center mb-8">
      <h1 class="text-4xl font-merriweather font-bold text-white mb-2">stockify.ai</h1>
      <p class="text-xl text-blue-200">Stock Sentiment Tracker</p>
      <p class="text-sm text-blue-300 mt-2">Monitor market sentiment in real-time</p>
    </div>
    <div class="w-full max-w-5xl grid grid-cols-1 md:grid-cols-2 gap-8">
      <div
        class="bg-white/10 backdrop-blur-lg rounded-2xl p-8 shadow-2xl border border-white/20 text-white flex flex-col justify-center space-y-6">
        <div class="flex-grow flex flex-col items-center justify-center">
          <div class=" rounded-2xl -translate-y-4  text-white">
            <h2 class="text-3xl font-semibold mb-4">
              Continue to <span class="font-merriweather">stockify.ai</span>
            </h2>
          </div>
          <button @click="loginWithGoogle"
            class="w-full max-w-xs flex items-center justify-center bg-white text-gray-700 py-3 px-4 rounded-lg font-semibold shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105 mb-4">
            <img src="https://developers.google.com/identity/images/g-logo.png" class="w-5 h-5 mr-3"
              alt="Google logo" />
            Login / Create Account
          </button>
          <a href="http://localhost:3000/stocks"
            class="w-full max-w-xs flex items-center justify-center bg-white text-gray-700 py-3 px-4 rounded-lg font-semibold shadow-md hover:shadow-lg transition-all duration-200 transform hover:scale-105">
            Continue Without an Account
          </a>
          <div class="mt-6 text-center">
            <button type="button" @click="goBack"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors duration-200 inline-flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="size-6">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
              </svg>
              <span>Return Home</span>
            </button>

            <p class="text-xs text-gray-300 mt-4 text-center max-w-full mx-auto">
              By signing in, you agree to our
              <router-link to="/terms" class="underline text-white">Terms of Service</router-link>,
              <router-link to="/privacy" class="underline text-white">Privacy Policy</router-link>, and
              <router-link to="/cookies" class="underline text-white">Cookie Policy</router-link>.
            </p>
          </div>

        </div>
      </div>
      <div class="bg-white/10 backdrop-blur-lg rounded-2xl p-8 shadow-2xl border border-white/20 text-white">
        <h2 class="text-3xl font-semibold mb-4">Why Create an Account?</h2>
        <ul class="space-y-3 text-blue-100 text-lg">
          <ul class="space-y-2 text-grey">
            <li class="flex items-center hover:text-white transition-colors duration-200 space-x-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6  flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M13.5 10.5V6.75a4.5 4.5 0 1 1 9 0v3.75M3.75 21.75h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H3.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" />
              </svg>
              <span>Creating an account is completely free</span>
            </li>
            <li class="flex items-center hover:text-white transition-colors duration-200 space-x-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M4.5 12a7.5 7.5 0 0 0 15 0m-15 0a7.5 7.5 0 1 1 15 0m-15 0H3m16.5 0H21m-1.5 0H12m-8.457 3.077 1.41-.513m14.095-5.13 1.41-.513M5.106 17.785l1.15-.964m11.49-9.642 1.149-.964M7.501 19.795l.75-1.3m7.5-12.99.75-1.3m-6.063 16.658.26-1.477m2.605-14.772.26-1.477m0 17.726-.26-1.477M10.698 4.614l-.26-1.477M16.5 19.794l-.75-1.299M7.5 4.205 12 12m6.894 5.785-1.149-.964M6.256 7.178l-1.15-.964m15.352 8.864-1.41-.513M4.954 9.435l-1.41-.514M12.002 12l-3.75 6.495" />
              </svg>
              <span>Gain access to every feature of&nbsp;<span class="font-merriweather">stockify.ai</span></span>
            </li>
            <li class="flex items-center hover:text-white transition-colors duration-200 space-x-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" />
              </svg>
              <span>View sentiments across our entire stock library</span>
            </li>
            <li class="flex items-center hover:text-white transition-colors duration-200 space-x-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6  flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M3.75 3v11.25A2.25 2.25 0 0 0 6 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0 1 18 16.5h-2.25m-7.5 0h7.5m-7.5 0-1 3m8.5-3 1 3m0 0 .5 1.5m-.5-1.5h-9.5m0 0-.5 1.5m.75-9 3-3 2.148 2.148A12.061 12.061 0 0 1 16.5 7.605" />
              </svg>
              <span>Follow stocks and compare trends</span>
            </li>
            <li class="flex items-center hover:text-white transition-colors duration-200 space-x-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
                stroke="currentColor" class="w-6 h-6 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M15.75 5.25a3 3 0 0 1 3 3m3 0a6 6 0 0 1-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1 1 21.75 8.25Z" />
              </svg>
              <span>Be the first to gain access to updates</span>
            </li>
          </ul>
        </ul>
        <p class="mt-6 text-sm text-blue-300">Without signing in your access to most features of Stockify.ai
          will be limited. Users may sign in at any time to access the entire functionality of the website.
        </p>
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
export default {
  name: 'Login',
  data() {
    return {
      email: '',
      password: ''
    }
  },
  methods: {
    handleLogin() {
      if (this.email && this.password) {
        this.$router.push('/home')
      }
    },
    goBack() {
      this.$router.push('/')
    },
    async loginWithGoogle() {
      this.clearAllLocalData();
      window.location.href = 'http://localhost:8001/auth/google';
    },

    clearAllLocalData() {
      localStorage.clear();
      sessionStorage.clear();
    }
  }
}
</script>
<style scoped />

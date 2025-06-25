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
    <div class="max-w-3xl mx-auto py-12 px-4 text-gray-800">
        <h1 class="text-3xl font-bold mb-6">Cookie Policy</h1>
        <p class="mb-4">
            This Cookie Policy explains how <strong>stockify.ai</strong> uses cookies and similar technologies when you
            visit our website or use our services.
        </p>
        <h2 class="text-xl font-semibold mt-8 mb-2">What are Cookies?</h2>
        <p class="mb-4">
            Cookies are small text files stored on your device by your web browser. They help us improve your experience
            by remembering your preferences and login status.
        </p>

        <h2 class="text-xl font-semibold mt-8 mb-2">How We Use Cookies</h2>
        <ul class="list-disc list-inside mb-4">
            <li><strong>Session Cookies:</strong> To manage your login sessions securely and keep you authenticated.
            </li>
            <li><strong>Analytics Cookies:</strong> We use Google Analytics to understand how users interact with our
                site and improve it.</li>
            <li><strong>Functional Cookies:</strong> To remember your preferences and improve your experience.</li>
        </ul>

        <h2 class="text-xl font-semibold mt-8 mb-2">Your Choices</h2>
        <p class="mb-4">
            You can control or delete cookies through your browser settings. However, disabling cookies may affect the
            functionality of our site.
        </p>

        <h2 class="text-xl font-semibold mt-8 mb-2">More Information</h2>
        <p>
            For more details, please visit our
            <router-link to="/privacy" class="text-purple-600 underline">Privacy Policy</router-link>.
        </p>
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
    name: "CookiePolicy",
    methods: {
        goBack() {
            this.$router.go(-1); // Navigate back to the previous page
        }
    },
    mounted() {
        window.scrollTo(0, 0);
    }
};
</script>
<style scoped>
h1,
h2 {
    color: #4B2E83;
}
</style>

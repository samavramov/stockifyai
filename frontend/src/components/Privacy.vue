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
        <h1 class="text-3xl font-bold mb-6">Privacy Policy</h1>

        <p class="mb-4">
            This Privacy Policy explains how <strong>stockify.ai</strong> ("we", "our", or "us") collects, uses, and
            protects your information when you use our website and services.
        </p>

        <h2 class="text-xl font-semibold mt-8 mb-2">1. Information We Collect</h2>
        <ul class="list-disc list-inside mb-4">
            <li><strong>OAuth Profile Data:</strong> We collect your email address and basic profile information when
                you sign in using Google or Apple.</li>
            <li><strong>Stock Preferences:</strong> We store the stocks you choose to follow and your push notification
                settings.</li>
            <li><strong>Usage Data:</strong> We use Google Analytics to collect anonymized data about how users interact
                with our site.</li>
        </ul>
        <h2 class="text-xl font-semibold mt-8 mb-2">2. How We Use Your Information</h2>
        <ul class="list-disc list-inside mb-4">
            <li>To personalize your experience and display the stocks you follow.</li>
            <li>To send push notifications if you opt in.</li>
            <li>To analyze usage trends and improve our services using Google Analytics.</li>
        </ul>
        <h2 class="text-xl font-semibold mt-8 mb-2">3. Data Sharing</h2>
        <p class="mb-4">
            We do <strong>not</strong> sell, trade, or rent your personal information. We may share anonymized data with
            trusted analytics providers such as Google Analytics.
        </p>
        <h2 class="text-xl font-semibold mt-8 mb-2">4. Data Security</h2>
        <p class="mb-4">
            We implement industry-standard security practices to protect your information. However, no method of
            transmission over the internet is 100% secure.
        </p>
        <h2 class="text-xl font-semibold mt-8 mb-2">5. Your Choices</h2>
        <ul class="list-disc list-inside mb-4">
            <li>You may delete your account by contacting us at <a href="mailto:stockify.ai.inquries@gmail.com"
                    class="text-purple-500 underline">stockify.ai.inquries@gmail.com</a>.</li>
        </ul>
       <h2 class="text-xl font-semibold mt-8 mb-2">6. Changes to This Policy</h2>
        <p class="mb-4">
            We may update this policy from time to time. If we make material changes, we will notify you by email or
            through the app.
        </p>
        <h2 class="text-xl font-semibold mt-8 mb-2">7. Contact Us</h2>
        <p>
            If you have any questions or concerns about this Privacy Policy, please contact us at
            <a href="mailto:stockify.ai.inquries@gmail.com"
                class="text-purple-500 underline">stockify.ai.inquries@gmail.com</a>.
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
    name: "PrivacyPolicy",
    methods: {
        goBack() {
            this.$router.go(-1); // Navigate back to the previous page
        }
    }
};
</script>
<style scoped>
h1,
h2 {
    color: #4B2E83;
}
</style>
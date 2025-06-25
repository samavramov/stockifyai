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
    <h1 class="text-3xl font-bold mb-6">Terms of Service</h1>

    <p class="mb-4">
      These Terms of Service ("Terms") govern your access to and use of <strong>stockify.ai</strong> ("we", "our", or
      "us"). By using our services, you agree to be bound by these Terms.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">1. Use of Service</h2>
    <p class="mb-4">
      You may use our service only if you are at least 13 years old and capable of forming a binding contract. Use of
      our service requires a valid Google or Apple account.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">2. Account Responsibilities</h2>
    <p class="mb-4">
      You are responsible for safeguarding your login credentials and for any activity on your account. We are not
      liable for any loss or damage resulting from unauthorized use of your account.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">3. Stock Data and Notifications</h2>
    <p class="mb-4">
      Our platform provides stock-related sentiment analysis and optional push alerts. All information provided on our
      site is for <strong>informational and educational purposes only</strong>. It does <strong>not constitute financial
        advice</strong>, and you agree not to use the service as a basis for investment or trading decisions. We are not
      licensed financial advisors.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">4. Data Usage and Analytics</h2>
    <p class="mb-4">
      We collect and analyze data about your use of the service through tools like Google Analytics to improve our
      platform. All data is handled in accordance with our Privacy Policy.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">5. Termination</h2>
    <p class="mb-4">
      We reserve the right to suspend or terminate your access to the service at our discretion, without notice, for
      conduct that violates these Terms or is harmful to other users.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">6. Changes to Terms</h2>
    <p class="mb-4">
      We may update these Terms at any time. Continued use of the service after any changes means you accept the revised
      Terms.
    </p>

    <h2 class="text-xl font-semibold mt-8 mb-2">7. Contact</h2>
    <p>
      If you have any questions about these Terms, contact us at
      <a href="mailto:stockify.ai.inquries@gmail.com"
        class="text-purple-600 underline">stockify.ai.inquries@gmail.com</a>.
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
  name: "TOS",
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

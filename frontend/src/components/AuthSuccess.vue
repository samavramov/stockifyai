// AuthSuccess.vue (using the new service)
<script>
import { checkAuthStatus } from '../apiService.js'; // Adjust path as needed

export default {
  name: 'AuthSuccess',
  async mounted() {
    try {
      const user = await checkAuthStatus();
      console.log('User:', user);
      this.$router.replace('/Home');
    } catch (error) {
      console.error('Authentication failed:', error);
      this.$router.replace('/login');
      window.history.replaceState({}, '', '/login');
    }
  }
}
</script>
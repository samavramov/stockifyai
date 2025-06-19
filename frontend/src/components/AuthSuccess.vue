<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-black text-white">
    <p>Authenticating...</p>
  </div>
</template>

<script>
export default {
  name: 'AuthSuccess',
  mounted() {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
    fetch(`${apiBaseUrl}/me`, {
      credentials: 'include',
    })
      .then(res => {
        if (!res.ok) {
          // If not logged in, throw error to trigger catch block
          throw new Error('Not logged in');
        }
        return res.json();
      })
      .then(data => {
        console.log('User:', data);
        // If authentication succeeds, replace with /Home
        this.$router.replace('/Home');
        // Do NOT replace history with '/login' here if successful
      })
      .catch((error) => {
        console.error('Authentication failed:', error);
        // If authentication fails, redirect to /login and update history
        this.$router.replace('/login');
        // Now it's appropriate to replace the history state to '/login'
        // so if they go back, they don't land on a broken auth-success page.
        window.history.replaceState({}, '', '/login'); 
      });
  }
}
</script>
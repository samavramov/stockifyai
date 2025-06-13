/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        royalpurple: {
          500: '#543ade', 
        },
      },
      fontFamily: {
          montserrat: ['Montserrat', 'sans-serif'],
          playfair: ['"Playfair Display"', 'serif'],
          merriweather: ['Merriweather', 'serif'],
      }
    },
    plugins: []
  }
}

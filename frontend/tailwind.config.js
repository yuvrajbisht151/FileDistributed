/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        navy: {
          950: '#070b18',
          900: '#0c1224',
          800: '#121a33',
          700: '#1b2545',
          600: '#26335c',
        },
        accent: {
          DEFAULT: '#8b5cf6',
          soft: '#a78bfa',
          deep: '#6d28d9',
        },
      },
      boxShadow: {
        card: '0 10px 30px -12px rgba(0, 0, 0, 0.6)',
      },
    },
  },
  plugins: [],
};

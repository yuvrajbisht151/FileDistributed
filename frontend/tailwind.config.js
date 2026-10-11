/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Page and panel backgrounds: off-white to white
        navy: {
          950: '#f6f7fb', // page background
          900: '#ffffff', // cards and panels
          800: '#eef1f6',
          700: '#e2e8f0', // scrollbar and tracks
          600: '#cbd5e1',
        },
        // "white" becomes dark ink, so text-white stays readable on a light page.
        // white/5, white/10 etc. still work as subtle tints on light backgrounds.
        white: '#0f172a',
        // Text colors: dark on light
        slate: {
          100: '#0f172a',
          200: '#1e293b',
          300: '#334155',
          400: '#64748b',
          500: '#94a3b8',
        },
        // Accent: indigo for buttons, highlights, focus rings
        accent: {
          DEFAULT: '#4f46e5',
          soft: '#4338ca',
          deep: '#3730a3',
        },
        // Secondary highlights used by the components, remapped to indigo and sky
        violet: {
          300: '#4f46e5',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
        },
        // Status colors darkened so badge text is readable on white
        emerald: { 200: '#065f46', 300: '#047857' },
        amber: { 200: '#92400e', 300: '#b45309' },
        rose: { 200: '#9f1239', 300: '#be123c', 400: '#e11d48' },
        sky: { 300: '#0369a1', 400: '#0ea5e9' },
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        body: ['Manrope', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(15, 23, 42, 0.04), 0 12px 30px -18px rgba(15, 23, 42, 0.18)',
        glow: '0 0 0 1px rgba(79, 70, 229, 0.15), 0 12px 30px -14px rgba(79, 70, 229, 0.35)',
      },
      keyframes: {
        fadeUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-up': 'fadeUp 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) both',
      },
    },
  },
  plugins: [],
};
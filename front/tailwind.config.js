/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        margins: {
          bg: '#f2f0eb',
          paper: '#fffefa',
          ink: '#171717',
          muted: '#66645f',
          line: '#d6d2c8',
        },
      },
      fontFamily: {
        display: ['Newsreader', '"Noto Sans KR"', 'ui-serif', 'Georgia', 'Cambria', '"Times New Roman"', 'serif'],
        sans: ['Inter', '"Noto Sans KR"', 'ui-sans-serif', 'system-ui', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'sans-serif'],
      },
      boxShadow: {
        editorial: '0 24px 80px rgba(23, 23, 23, 0.10)',
        'editorial-sm': '0 12px 36px rgba(23, 23, 23, 0.05)',
      },
    },
  },
  plugins: [],
};

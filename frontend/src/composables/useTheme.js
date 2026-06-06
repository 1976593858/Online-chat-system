import { ref, watchEffect } from 'vue'

const THEME_KEY = 'aurora-theme'
const theme = ref(localStorage.getItem(THEME_KEY) || 'auto')

function applyTheme(value) {
  if (value === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else if (value === 'light') {
    document.documentElement.setAttribute('data-theme', 'light')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

watchEffect(() => applyTheme(theme.value))

export function useTheme() {
  function setTheme(value) {
    theme.value = value
    localStorage.setItem(THEME_KEY, value)
  }

  return {
    theme,
    setTheme,
    modes: [
      { key: 'auto', label: '自动', icon: '◐' },
      { key: 'light', label: '浅色', icon: '☀' },
      { key: 'dark', label: '深色', icon: '☾' }
    ]
  }
}

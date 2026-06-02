import { ref } from 'vue'

const currentTheme = ref('dark')

export function useTheme() {
  const getSystemTheme = () => {
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
  }

  const applyTheme = (theme) => {
    document.documentElement.setAttribute('data-theme', theme)
    currentTheme.value = theme
  }

  const toggleTheme = () => {
    const nextTheme = currentTheme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('ecoswitch_theme', nextTheme)
    applyTheme(nextTheme)
  }

  const initTheme = () => {
    const savedTheme = localStorage.getItem('ecoswitch_theme')
    if (savedTheme === 'light' || savedTheme === 'dark') {
      applyTheme(savedTheme)
    } else {
      applyTheme(getSystemTheme())
    }

    // Live listen to system preference changes (only if no manual selection is stored)
    window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', (e) => {
      if (!localStorage.getItem('ecoswitch_theme')) {
        applyTheme(e.matches ? 'light' : 'dark')
      }
    })
  }

  return {
    currentTheme,
    initTheme,
    toggleTheme
  }
}

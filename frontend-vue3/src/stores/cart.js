import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from './user'

const CART_KEY_PREFIX = 'zoo_cart_'

function getCartKey() {
  const userStore = useUserStore()
  const user = userStore.user
  if (!user) return CART_KEY_PREFIX + 'temp'
  const identifier = user.id !== undefined ? user.id : (user.username || 'temp')
  return CART_KEY_PREFIX + identifier
}

export const useCartStore = defineStore('cart', () => {
  const cart = ref([])

  function loadCart() {
    try {
      const key = getCartKey()
      const data = localStorage.getItem(key)
      cart.value = JSON.parse(data || '[]').map(c => ({ ...c, checked: c.checked ?? true }))
      return cart.value
    } catch {
      cart.value = []
      return []
    }
  }

  function saveCart() {
    const key = getCartKey()
    localStorage.setItem(key, JSON.stringify(cart.value))
  }

  function addToCart(product, quantity = 1) {
    const exist = cart.value.find(c => c.id === product.id)
    if (exist) {
      exist.quantity += quantity
    } else {
      cart.value.push({
        id: product.id,
        name: product.name,
        price: Number(product.price),
        quantity,
        imageUrl: product.imageUrl,
        checked: true
      })
    }
    saveCart()
  }

  function removeFromCart(id) {
    const index = cart.value.findIndex(c => c.id === id)
    if (index > -1) {
      cart.value.splice(index, 1)
      saveCart()
    }
  }

  function updateQuantity(id, quantity) {
    const item = cart.value.find(c => c.id === id)
    if (item) {
      item.quantity = Math.max(1, quantity)
      saveCart()
    }
  }

  function toggleCheck(id) {
    const item = cart.value.find(c => c.id === id)
    if (item) {
      item.checked = !item.checked
      saveCart()
    }
  }

  function toggleAll(checked) {
    cart.value.forEach(item => {
      item.checked = checked
    })
    saveCart()
  }

  const selectedItems = computed(() => cart.value.filter(c => c.checked))
  const totalAmount = computed(() => {
    return selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  })

  return {
    cart,
    loadCart,
    saveCart,
    addToCart,
    removeFromCart,
    updateQuantity,
    toggleCheck,
    toggleAll,
    selectedItems,
    totalAmount
  }
})


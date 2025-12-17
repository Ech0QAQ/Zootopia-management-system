<template>
  <div>
    <div class="card">
      <div class="title">动物商城</div>
      <div class="subtitle">查看已上架商品，支持搜索与快速购买。</div>
      <div class="form-row" style="align-items:flex-end;">
        <div class="form-field" style="flex:1;">
          <label>搜索商品名</label>
          <input v-model="keyword" placeholder="输入名称" />
        </div>
        <div style="display:flex;gap:8px;align-items:center;padding-bottom:6px;">
          <button class="btn" @click="searchProducts">搜索</button>
        </div>
      </div>
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:12px;">
        <div
          v-for="product in products"
          :key="product.id"
          class="card"
          style="padding:12px;cursor:pointer;"
          @click="showProductDetail(product)"
        >
          <img :src="product.imageUrl" style="width:100%;height:140px;object-fit:cover;border-radius:8px;" />
          <div style="margin-top:8px;font-weight:700;">{{ product.name }}</div>
          <div style="color:#d33;font-size:16px;">￥{{ product.price }}</div>
          <div style="font-size:12px;color:#666;">
            库存：<span :style="`font-weight:700;color:${product.stock === 0 ? '#c00' : product.stock <= 10 ? '#d98200' : '#333'};`">{{ product.stock }}</span>
          </div>
        </div>
        <div v-if="!products.length" style="color:#999;">暂无商品</div>
      </div>
    </div>

    <!-- 浮动购物车按钮 -->
    <div
      class="floating-btn"
      style="position:fixed;right:20px;bottom:20px;width:52px;height:52px;border-radius:50%;background:#145a7b;color:#fff;display:flex;align-items:center;justify-content:center;cursor:pointer;box-shadow:0 10px 24px rgba(0,0,0,0.18);z-index:999;"
      @click="showCartDialog = true"
    >
      <span style="font-size:22px;">🛒</span>
    </div>

    <!-- 商品详情弹窗 -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
      <div class="modal-card" style="width:680px;max-height:80vh;overflow:auto;">
        <div style="display:flex;gap:16px;flex-wrap:wrap;">
          <img :src="currentProduct?.imageUrl" style="width:260px;height:260px;object-fit:cover;border-radius:12px;" />
          <div style="flex:1;min-width:220px;">
            <div class="title" style="font-size:22px;margin-bottom:6px;">{{ currentProduct?.name }}</div>
            <div style="color:#d33;font-size:20px;margin-bottom:8px;">￥{{ currentProduct?.price }}</div>
            <div style="margin-bottom:6px;">
              库存：<strong :style="`color:${currentProduct?.stock === 0 ? '#c00' : currentProduct?.stock <= 10 ? '#d98200' : '#333'};`">{{ currentProduct?.stock }}</strong>
            </div>
            <div style="display:flex;align-items:center;gap:8px;margin:8px 0;">
              <span>数量</span>
              <button class="btn secondary" @click="quantity = Math.max(1, quantity - 1)" style="padding:4px 10px;">-</button>
              <span style="margin:0 8px;">{{ quantity }}</span>
              <button class="btn secondary" @click="quantity += 1" style="padding:4px 10px;">+</button>
            </div>
            <div style="display:flex;gap:12px;margin-top:12px;">
              <button class="btn" @click="buyNow">立刻购买</button>
              <button class="btn secondary" @click="addToCart">加入购物车</button>
            </div>
          </div>
        </div>
        <div style="margin-top:12px;font-size:14px;">商品详情：</div>
        <div style="padding:8px;border:1px solid #eef2f7;border-radius:8px;min-height:80px;" v-html="bufferToString(currentProduct?.description)"></div>
      </div>
    </div>

    <!-- 购物车弹窗 -->
    <div v-if="showCartDialog" class="modal-overlay" @click.self="showCartDialog = false">
      <div class="modal-card" style="width:720px;max-height:80vh;overflow:auto;">
        <div class="title" style="font-size:20px;">购物车</div>
        <div v-if="cart.length">
          <div style="margin-bottom:8px;display:flex;align-items:center;gap:8px;">
            <label style="display:flex;align-items:center;gap:6px;font-size:13px;">
              <input type="checkbox" v-model="selectAll" @change="handleSelectAll" /> 全选
            </label>
          </div>
          <table style="width:100%;font-size:13px;border-collapse:collapse;">
            <thead>
              <tr>
                <th style="padding:8px;border-bottom:1px solid #f1f1f1;">选</th>
                <th style="padding:8px;border-bottom:1px solid #f1f1f1;">商品</th>
                <th style="padding:8px;border-bottom:1px solid #f1f1f1;">单价</th>
                <th style="padding:8px;border-bottom:1px solid #f1f1f1;">数量</th>
                <th style="padding:8px;border-bottom:1px solid #f1f1f1;">小计</th>
                <th style="padding:8px;border-bottom:1px solid #f1f1f1;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in cart" :key="idx" style="border-bottom:1px solid #f1f1f1;">
                <td style="padding:8px;text-align:center;">
                  <input type="checkbox" v-model="item.checked" @change="updateCart" />
                </td>
                <td style="padding:8px;display:flex;align-items:center;gap:8px;">
                  <img :src="item.imageUrl" style="width:48px;height:48px;object-fit:cover;border-radius:6px;" />
                  <div>{{ item.name }}</div>
                </td>
                <td style="padding:8px;color:#d33;">￥{{ item.price }}</td>
                <td style="padding:8px;">
                  <button class="btn secondary" @click="decreaseQuantity(idx)" style="padding:4px 10px;">-</button>
                  <span style="margin:0 8px;">{{ item.quantity }}</span>
                  <button class="btn secondary" @click="increaseQuantity(idx)" style="padding:4px 10px;">+</button>
                </td>
                <td style="padding:8px;color:#d33;">￥{{ (item.price * item.quantity).toFixed(2) }}</td>
                <td style="padding:8px;">
                  <button class="btn secondary" @click="removeFromCart(idx)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;">
            <div style="font-size:14px;">已选 {{ selectedCount }} 件</div>
            <div style="display:flex;gap:12px;align-items:center;">
              <div style="font-size:16px;">合计：<span style="color:#d33;font-weight:700;">￥{{ totalAmount.toFixed(2) }}</span></div>
              <button class="btn" @click="showCheckoutDialog = true; showCartDialog = false">去结算</button>
            </div>
          </div>
        </div>
        <div v-else style="color:#999;text-align:center;padding:20px;">购物车空空如也</div>
      </div>
    </div>

    <!-- 结算弹窗 -->
    <div v-if="showCheckoutDialog" class="modal-overlay" @click.self="handleCheckoutOverlayClick">
      <div class="modal-card" style="width:640px;max-height:80vh;overflow:auto;">
        <div class="title" style="font-size:20px;">填写收货信息</div>
        <div class="form-field">
          <label>收货人</label>
          <input v-model="checkoutForm.receiverName" />
        </div>
        <div class="form-field">
          <label>电话</label>
          <input v-model="checkoutForm.receiverPhone" />
        </div>
        <div class="form-field">
          <label>收货地址</label>
          <textarea v-model="checkoutForm.receiverAddress" rows="3"></textarea>
        </div>
        <div style="margin:10px 0;font-size:13px;">商品信息：</div>
        <div
          v-for="item in selectedItems"
          :key="item.id"
          style="border-bottom:1px solid #f1f1f1;padding:6px 0;font-size:13px;"
        >
          {{ item.name }} x {{ item.quantity }} | ￥{{ (item.price * item.quantity).toFixed(2) }}
        </div>
        <div style="margin-top:12px;font-size:15px;">
          订单总价：<span style="color:#d33;font-weight:700;">￥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <div style="margin-top:12px;display:flex;gap:10px;justify-content:flex-end;">
          <button class="btn secondary" @click="showCheckoutDialog = false">取消</button>
          <button class="btn" @click="confirmCheckout">确认支付</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/utils/api'
import { bufferToString } from '@/utils/constants'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const products = ref([])
const keyword = ref('')
const showDetail = ref(false)
const showCartDialog = ref(false)
const showCheckoutDialog = ref(false)
const currentProduct = ref(null)
const quantity = ref(1)
const cart = ref([])
const checkoutForm = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const selectAll = computed({
  get: () => cart.value.length > 0 && cart.value.every(item => item.checked),
  set: (val) => {
    cart.value.forEach(item => { item.checked = val })
    saveCart()
  }
})

const selectedItems = computed(() => cart.value.filter(item => item.checked))
const selectedCount = computed(() => selectedItems.value.length)
const totalAmount = computed(() => selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

onMounted(async () => {
  loadCart()
  await loadProducts()
})

function getCartKey() {
  const user = userStore.user
  if (!user) return 'zoo_cart_temp'
  const identifier = user.id !== undefined ? user.id : (user.username || 'temp')
  return `zoo_cart_${identifier}`
}

function loadCart() {
  try {
    const key = getCartKey()
    const data = localStorage.getItem(key)
    cart.value = JSON.parse(data || '[]').map(c => ({ ...c, checked: c.checked ?? true }))
  } catch {
    cart.value = []
  }
}

function saveCart() {
  const key = getCartKey()
  localStorage.setItem(key, JSON.stringify(cart.value))
}

async function loadProducts() {
  try {
    const params = keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : ''
    const res = await api.get(`/shop/products${params}`)
    products.value = res.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

function searchProducts() {
  loadProducts()
}

function showProductDetail(product) {
  currentProduct.value = product
  quantity.value = 1
  showDetail.value = true
}

function addToCart() {
  const p = currentProduct.value
  if (p.stock <= 0) {
    alert('所选商品库存不足！')
    return
  }
  const exist = cart.value.find(c => c.id === p.id)
  if (exist) {
    exist.quantity += quantity.value
  } else {
    cart.value.push({
      id: p.id,
      name: p.name,
      price: Number(p.price),
      quantity: quantity.value,
      imageUrl: p.imageUrl,
      checked: true
    })
  }
  saveCart()
  showDetail.value = false
  alert('已加入购物车')
}

function buyNow() {
  const p = currentProduct.value
  if (p.stock <= 0) {
    alert('所选商品库存不足！')
    return
  }
  // 清空其他商品的选中状态，只选中当前商品
  cart.value.forEach(c => { c.checked = false })
  const exist = cart.value.find(c => c.id === p.id)
  if (exist) {
    exist.quantity = quantity.value
    exist.checked = true
  } else {
    cart.value.push({
      id: p.id,
      name: p.name,
      price: Number(p.price),
      quantity: quantity.value,
      imageUrl: p.imageUrl,
      checked: true
    })
  }
  saveCart()
  showDetail.value = false
  showCheckoutDialog.value = true
}

function handleSelectAll() {
  cart.value.forEach(item => { item.checked = selectAll.value })
  saveCart()
}

function increaseQuantity(idx) {
  cart.value[idx].quantity += 1
  saveCart()
}

function decreaseQuantity(idx) {
  cart.value[idx].quantity = Math.max(1, cart.value[idx].quantity - 1)
  saveCart()
}

function removeFromCart(idx) {
  cart.value.splice(idx, 1)
  saveCart()
}

function handleCheckoutOverlayClick(e) {
  // 结算页面只能通过取消按钮关闭，不能点击外部关闭
  if (e.target === e.currentTarget) {
    return
  }
}

async function confirmCheckout() {
  const { receiverName, receiverPhone, receiverAddress } = checkoutForm.value
  if (!receiverName || !receiverPhone || !receiverAddress) {
    alert('请完整填写收货信息')
    return
  }
  if (selectedItems.value.length === 0) {
    alert('请先选择商品')
    return
  }
  try {
    await api.post('/shop/orders', {
      items: selectedItems.value.map(s => ({ product_id: s.id, quantity: s.quantity })),
      receiver_name: receiverName,
      receiver_phone: receiverPhone,
      receiver_address: receiverAddress
    })
    alert('支付成功，订单已生成')
    cart.value = cart.value.filter(c => !c.checked)
    saveCart()
    checkoutForm.value = {
      receiverName: '',
      receiverPhone: '',
      receiverAddress: ''
    }
    showCheckoutDialog.value = false
  } catch (error) {
    alert(error.response?.data?.message || '支付失败')
  }
}

watch(showCartDialog, (val) => {
  if (val) {
    loadCart()
  }
})
</script>

<template>
  <div>
    <div class="card">
      <div class="title">商城管理</div>
      <div class="subtitle">新增、编辑、上下架、删除商品，支持搜索。</div>
      <div class="form-row" style="align-items:flex-end;gap:8px;">
        <div class="form-field" style="flex:1;">
          <label>搜索商品名</label>
          <input v-model="keyword" />
        </div>
        <div style="display:flex;gap:8px;align-items:center;padding-bottom:6px;">
          <button class="btn" @click="loadProducts">搜索</button>
          <button class="btn" @click="showAddDialog = true">新增商品</button>
        </div>
      </div>
      <div style="margin-top:12px;">
        <table v-if="products.length" style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
          <thead>
            <tr>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">编号</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">名称</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">价格</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">库存</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">状态</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in products" :key="p.id" style="border-bottom:1px solid #f1f1f1;">
              <td style="padding:8px;">{{ bufferToString(p.code) }}</td>
              <td style="padding:8px;">{{ bufferToString(p.name) }}</td>
              <td style="padding:8px;">￥{{ p.price }}</td>
              <td :style="`padding:8px;${p.stock > 10 ? 'color:#333;' : `font-weight:700;color:${p.stock === 0 ? '#c00' : '#d98200'};`}`">{{ p.stock }}</td>
              <td style="padding:8px;">
                <span v-if="p.status === 'on'">上架</span>
                <span v-else style="color:#c00;">下架</span>
              </td>
              <td style="padding:8px;">
                <button class="btn secondary" @click="editProduct(p)">编辑</button>
                <button
                  v-if="p.status === 'on'"
                  class="btn secondary"
                  style="margin:0 10px;"
                  @click="toggleStatus(p.id, 'off')"
                >
                  下架
                </button>
                <button
                  v-else
                  class="btn secondary"
                  style="margin:0 10px;"
                  @click="toggleStatus(p.id, 'on')"
                >
                  上架
                </button>
                <button class="btn secondary" @click="deleteProduct(p.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else style="color:#999;">暂无商品</div>
      </div>
    </div>

    <!-- 新增/编辑商品弹窗 -->
    <div v-if="showAddDialog" class="modal-overlay" @click.self="handleOverlayClick">
      <div class="modal-card" style="width:720px;max-height:80vh;overflow:auto;">
        <div class="title" style="font-size:20px;">{{ editingProduct ? '编辑商品' : '新增商品' }}</div>
        <div class="form-row">
          <div class="form-field">
            <label>商品编号</label>
            <div style="padding:8px 10px;border:1px solid #d0d7e2;border-radius:8px;background:#f8f9fb;">
              {{ editingProduct ? bufferToString(editingProduct.code) : '系统自动生成' }}
            </div>
          </div>
          <div class="form-field">
            <label>商品名称</label>
            <input v-model="productForm.name" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>价格</label>
            <input v-model.number="productForm.price" type="number" />
          </div>
          <div class="form-field">
            <label>库存</label>
            <input v-model.number="productForm.stock" type="number" />
          </div>
        </div>
        <div class="form-field">
          <label>商品简介（可加粗/斜体）</label>
          <div style="margin-bottom:6px;display:flex;gap:8px;">
            <button class="btn secondary" @click="formatText('bold')" style="padding:4px 10px;">加粗</button>
            <button class="btn secondary" @click="formatText('italic')" style="padding:4px 10px;">斜体</button>
            <button class="btn secondary" @click="formatText('undo')" style="padding:4px 10px;">撤销</button>
            <button class="btn secondary" @click="formatText('redo')" style="padding:4px 10px;">重做</button>
          </div>
          <div
            ref="descEditor"
            contenteditable="true"
            style="min-height:140px;border:1px solid #d0d7e2;border-radius:8px;padding:8px;"
            v-html="productForm.description"
          ></div>
        </div>
        <div class="form-field">
          <label>商品图片（小于500KB）</label>
          <div v-if="editingProduct && (editingProduct.image || editingProduct.image_size)" style="margin-bottom:8px;">
            <span style="color:#666;font-size:13px;">当前图片：</span>
            <a :href="`http://localhost:3000/api/shop/products/${editingProduct.id}/image`" target="_blank" style="color:#1890ff;text-decoration:underline;cursor:pointer;font-size:13px;">查看图片</a>
          </div>
          <input ref="imageInput" type="file" accept="image/*" @change="handleImageChange" />
          <div v-if="editingProduct && (editingProduct.image || editingProduct.image_size) && !productForm.imageBase64" style="font-size:12px;color:#999;margin-top:4px;">上传新图片将替换当前图片</div>
        </div>
        <div style="text-align:right;display:flex;gap:10px;justify-content:flex-end;margin-top:10px;">
          <button class="btn secondary" @click="handleCancel">取消</button>
          <button class="btn" @click="handleSave">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { bufferToString } from '@/utils/constants'

const products = ref([])
const keyword = ref('')
const showAddDialog = ref(false)
const editingProduct = ref(null)
const productForm = ref({
  name: '',
  price: 0,
  stock: 0,
  description: '',
  imageBase64: null
})
const descEditor = ref(null)
const imageInput = ref(null)

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  try {
    const params = keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : ''
    const res = await api.get(`/admin/shop/products${params}`)
    products.value = res.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

function editProduct(product) {
  editingProduct.value = product
  productForm.value = {
    name: bufferToString(product.name),
    price: product.price,
    stock: product.stock,
    description: bufferToString(product.description),
    imageBase64: null
  }
  showAddDialog.value = true
}

function formatText(command) {
  if (descEditor.value) {
    document.execCommand(command, false, null)
    descEditor.value.focus()
  }
}

function handleImageChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 500 * 1024) {
    alert('图片大小不能超过500KB')
    return
  }
  const reader = new FileReader()
  reader.onload = (event) => {
    productForm.value.imageBase64 = event.target.result
  }
  reader.onerror = () => {
    alert('读取图片失败')
  }
  reader.readAsDataURL(file)
}

function handleOverlayClick(e) {
  // 仅允许点击取消关闭，避免误触
  if (e.target === e.currentTarget) {
    return
  }
}

function handleCancel() {
  if (confirm('系统将不会保存您当前编辑的内容，是否确认取消编辑？')) {
    showAddDialog.value = false
    editingProduct.value = null
    productForm.value = {
      name: '',
      price: 0,
      stock: 0,
      description: '',
      imageBase64: null
    }
    if (imageInput.value) imageInput.value.value = ''
  }
}

async function handleSave() {
  const name = productForm.value.name.trim()
  const price = productForm.value.price
  const stock = productForm.value.stock
  const description = descEditor.value ? descEditor.value.innerHTML : productForm.value.description
  
  if (!name || price == null || price < 0 || stock == null || stock < 0 || !description) {
    alert('请完整填写信息')
    return
  }
  
  let imageBase64 = productForm.value.imageBase64
  if (!editingProduct.value || imageBase64) {
    if (!imageBase64) {
      alert('请上传图片')
      return
    }
  }
  
  try {
    if (editingProduct.value) {
      await api.put(`/admin/shop/products/${editingProduct.value.id}`, {
        name,
        price,
        stock,
        description,
        imageBase64
      })
    } else {
      await api.post('/admin/shop/products', {
        name,
        price,
        stock,
        description,
        imageBase64
      })
    }
    showAddDialog.value = false
    editingProduct.value = null
    productForm.value = {
      name: '',
      price: 0,
      stock: 0,
      description: '',
      imageBase64: null
    }
    if (imageInput.value) imageInput.value.value = ''
    await loadProducts()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}

async function toggleStatus(id, status) {
  try {
    await api.post(`/admin/shop/products/${id}/${status}`)
    await loadProducts()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}

async function deleteProduct(id) {
  if (!confirm('确认删除该商品？')) return
  try {
    await api.delete(`/admin/shop/products/${id}`)
    await loadProducts()
  } catch (error) {
    alert(error.response?.data?.message || '删除失败')
  }
}
</script>


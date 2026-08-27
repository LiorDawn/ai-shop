import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCartList,
  addToCart,
  updateCartNum,
  toggleCartCheck,
  checkAllCart,
  deleteCartItem,
  deleteCartBatch,
  settleCheck,
  type CartItemVO,
  type ShopCartGroup,
} from '../api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItemVO[]>([])
  const loading = ref(false)

  const totalNum = computed(() => items.value.reduce((sum, item) => sum + item.num, 0))
  const validItems = computed(() => items.value.filter((item) => item.productStatus === 1))
  const selectedItems = computed(() =>
    validItems.value.filter((item) => item.checked === 1),
  )
  const selectedNum = computed(() => selectedItems.value.reduce((sum, item) => sum + item.num, 0))
  const totalPrice = computed(() =>
    selectedItems.value.reduce((sum, item) => sum + item.price * item.num, 0),
  )
  const isAllChecked = computed(() =>
    validItems.value.length > 0 && validItems.value.every((item) => item.checked === 1),
  )
  const isIndeterminate = computed(() => {
    const checkedCount = validItems.value.filter((item) => item.checked === 1).length
    return checkedCount > 0 && checkedCount < validItems.value.length
  })

  const shopGroups = computed<ShopCartGroup[]>(() => {
    const map = new Map<number, CartItemVO[]>()
    items.value.forEach((item) => {
      const key = item.shopId || 0
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(item)
    })
    const groups: ShopCartGroup[] = []
    map.forEach((list, shopId) => {
      groups.push({
        shopId,
        shopName: list[0].shopName || '未知店铺',
        items: list,
      })
    })
    return groups
  })

  async function fetchCart() {
    loading.value = true
    try {
      const res: any = await getCartList()
      items.value = Array.isArray(res.data) ? res.data : []
    } catch {
      items.value = []
    } finally {
      loading.value = false
    }
  }

  async function addItem(productId: number, num: number) {
    await addToCart({ productId, num })
    await fetchCart()
  }

  async function changeNum(id: number, num: number) {
    await updateCartNum(id, num)
    const item = items.value.find((i) => i.id === id)
    if (item) item.num = num
  }

  async function toggleItem(id: number, checked: number) {
    await toggleCartCheck(id, checked)
    const item = items.value.find((i) => i.id === id)
    if (item) item.checked = checked
  }

  async function toggleAll(checked: number) {
    await checkAllCart(checked)
    items.value.forEach((item) => {
      if (item.productStatus === 1) item.checked = checked
    })
  }

  async function toggleShopAll(shopId: number, checked: number) {
    const group = shopGroups.value.find((g) => g.shopId === shopId)
    if (!group) return
    for (const item of group.items) {
      if (item.productStatus === 1) {
        try {
          await toggleCartCheck(item.id, checked)
          item.checked = checked
        } catch {
          // continue
        }
      }
    }
  }

  async function removeItem(id: number) {
    await deleteCartItem(id)
    items.value = items.value.filter((item) => item.id !== id)
  }

  async function removeBatch(ids: number[]) {
    await deleteCartBatch(ids)
    items.value = items.value.filter((item) => !ids.includes(item.id))
  }

  async function checkSettle() {
    const res = await settleCheck()
    return res.data
  }

  function getShopValidItems(group: ShopCartGroup): CartItemVO[] {
    return group.items.filter((item) => item.productStatus === 1)
  }

  function isShopAllChecked(shopId: number): boolean {
    const group = shopGroups.value.find((g) => g.shopId === shopId)
    if (!group) return false
    const valid = getShopValidItems(group)
    return valid.length > 0 && valid.every((item) => item.checked === 1)
  }

  function isShopIndeterminate(shopId: number): boolean {
    const group = shopGroups.value.find((g) => g.shopId === shopId)
    if (!group) return false
    const valid = getShopValidItems(group)
    const checked = valid.filter((item) => item.checked === 1).length
    return checked > 0 && checked < valid.length
  }

  function clearCart() {
    items.value = []
  }

  return {
    items,
    loading,
    totalNum,
    validItems,
    selectedItems,
    selectedNum,
    totalPrice,
    isAllChecked,
    isIndeterminate,
    shopGroups,
    fetchCart,
    addItem,
    changeNum,
    toggleItem,
    toggleAll,
    toggleShopAll,
    removeItem,
    removeBatch,
    checkSettle,
    getShopValidItems,
    isShopAllChecked,
    isShopIndeterminate,
    clearCart,
  }
})
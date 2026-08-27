import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listCategories } from '../api/category'

export interface Category {
  id: number
  name: string
  parentId: number | null
  sort: number
}

export const useCategoryStore = defineStore('category', () => {
  const categories = ref<Category[]>([])
  const loading = ref(false)

  const topCategories = computed(() =>
    categories.value.filter((c) => c.parentId === 0 || c.parentId == null),
  )

  const subCategoryMap = computed(() => {
    const map = new Map<number, Category[]>()
    categories.value.forEach((c) => {
      if (c.parentId && c.parentId > 0) {
        if (!map.has(c.parentId)) map.set(c.parentId, [])
        map.get(c.parentId)!.push(c)
      }
    })
    return map
  })

  function getSubCategories(parentId: number): Category[] {
    return subCategoryMap.value.get(parentId) || []
  }

  async function fetchCategories() {
    loading.value = true
    try {
      const res: any = await listCategories()
      const list = (res?.data || []).filter((c: Category) => c && c.id)
      if (list.length) {
        categories.value = list
      }
    } catch {
      categories.value = defaultCategories()
    } finally {
      loading.value = false
    }
  }

  return {
    categories,
    loading,
    topCategories,
    subCategoryMap,
    getSubCategories,
    fetchCategories,
  }
})

function defaultCategories(): Category[] {
  return [
    { id: 1, name: '冰洗', parentId: 0, sort: 1 },
    { id: 2, name: '电脑', parentId: 0, sort: 2 },
    { id: 3, name: '手机', parentId: 0, sort: 3 },
    { id: 4, name: '生活电器', parentId: 0, sort: 4 },
    { id: 5, name: '食品', parentId: 0, sort: 5 },
    { id: 6, name: '美妆', parentId: 0, sort: 6 },
    { id: 7, name: '元器件', parentId: 0, sort: 7 },
    { id: 8, name: '家装', parentId: 0, sort: 8 },
    { id: 9, name: '家居日用', parentId: 0, sort: 9 },
    { id: 10, name: '男鞋', parentId: 0, sort: 10 },
    { id: 11, name: '男装', parentId: 0, sort: 11 },
    { id: 101, name: '冰箱', parentId: 1, sort: 1 },
    { id: 102, name: '空调', parentId: 1, sort: 2 },
    { id: 103, name: '电视', parentId: 1, sort: 3 },
    { id: 104, name: '厨卫大电', parentId: 1, sort: 4 },
    { id: 201, name: '电脑', parentId: 2, sort: 1 },
    { id: 202, name: '办公', parentId: 2, sort: 2 },
    { id: 203, name: '文具用品', parentId: 2, sort: 3 },
    { id: 301, name: '手机', parentId: 3, sort: 1 },
    { id: 302, name: '运营商', parentId: 3, sort: 2 },
    { id: 303, name: '数码', parentId: 3, sort: 3 },
    { id: 401, name: '生活电器', parentId: 4, sort: 1 },
    { id: 402, name: '厨房小电', parentId: 4, sort: 2 },
    { id: 403, name: '个护健康', parentId: 4, sort: 3 },
    { id: 501, name: '食品', parentId: 5, sort: 1 },
    { id: 502, name: '酒类', parentId: 5, sort: 2 },
    { id: 503, name: '生鲜', parentId: 5, sort: 3 },
    { id: 504, name: '特产', parentId: 5, sort: 4 },
    { id: 601, name: '美妆', parentId: 6, sort: 1 },
    { id: 602, name: '个护清洁', parentId: 6, sort: 2 },
    { id: 603, name: '宠物', parentId: 6, sort: 3 },
    { id: 701, name: '元器件', parentId: 7, sort: 1 },
    { id: 702, name: '劳防物资', parentId: 7, sort: 2 },
    { id: 703, name: '五金机电', parentId: 7, sort: 3 },
    { id: 801, name: '家装', parentId: 8, sort: 1 },
    { id: 802, name: '建材', parentId: 8, sort: 2 },
    { id: 803, name: '家具', parentId: 8, sort: 3 },
    { id: 901, name: '家居日用', parentId: 9, sort: 1 },
    { id: 902, name: '厨具', parentId: 9, sort: 2 },
    { id: 1001, name: '男鞋', parentId: 10, sort: 1 },
    { id: 1002, name: '运动', parentId: 10, sort: 2 },
    { id: 1003, name: '户外', parentId: 10, sort: 3 },
    { id: 1101, name: '男装', parentId: 11, sort: 1 },
    { id: 1102, name: '女装', parentId: 11, sort: 2 },
    { id: 1103, name: '童装', parentId: 11, sort: 3 },
    { id: 1104, name: '内衣', parentId: 11, sort: 4 },
  ]
}
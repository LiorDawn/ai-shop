<template>
  <div class="mr-page">
    <HeaderUser />

    <div class="mr-container">
      <h2 class="mr-title">我的评价</h2>

      <!-- 筛选 -->
      <div class="mr-filter">
        <span
          v-for="f in filters"
          :key="f.value"
          class="mr-filter-item"
          :class="{ active: activeFilter === f.value }"
          @click="activeFilter = f.value; filterReviews()"
        >{{ f.label }}</span>
      </div>

      <div v-loading="loading">
        <div v-if="reviews.length === 0 && !loading" class="mr-empty">
          <el-empty description="暂无评价" />
        </div>

        <div v-for="r in filteredReviews" :key="r.id" class="mr-card">
          <!-- 商品信息 + 评分 -->
          <div class="mr-top">
            <img :src="r.productImage || defaultImg" class="mr-img" />
            <div class="mr-info">
              <div class="mr-name">{{ r.productName }}</div>
              <el-rate v-model="r.score" disabled size="small" />
            </div>
            <span class="mr-time">{{ r.createTime }}</span>
          </div>

          <!-- 评价内容 -->
          <div class="mr-content" :class="{ 'mr-collapsed': collapsed.has(r.id) }">
            {{ r.content }}
          </div>
          <div
            v-if="r.content && r.content.length > 100"
            class="mr-expand"
            @click="toggleCollapse(r.id)"
          >
            {{ collapsed.has(r.id) ? '展开全部' : '收起' }}
          </div>

          <!-- 晒单图片 -->
          <div v-if="r.imageList && r.imageList.length" class="mr-images">
            <img
              v-for="(img, idx) in r.imageList"
              :key="idx"
              :src="img"
              class="mr-img-item"
              @click="previewImages(r.imageList, idx)"
            />
          </div>

          <!-- 商家回复 -->
          <div v-if="r.reply" class="mr-reply">
            <span class="mr-reply-label">商家回复：</span>{{ r.reply }}
            <span v-if="r.replyTime" class="mr-reply-time">{{ r.replyTime }}</span>
          </div>
        </div>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchReviews"
          class="mr-pager"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getMyComments, type CommentDTO } from '@/api/comment'

const defaultImg = 'https://picsum.photos/seed/default/60/60'
const loading = ref(false)
const reviews = ref<CommentDTO[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const collapsed = ref<Set<number>>(new Set())
const activeFilter = ref(-1)

const filters = [
  { label: '全部', value: -1 },
  { label: '好评', value: 5 },
  { label: '中评', value: 3 },
  { label: '差评', value: 1 },
]

const filteredReviews = computed(() => {
  if (activeFilter.value === -1) return reviews.value
  if (activeFilter.value === 5) return reviews.value.filter((r) => r.score >= 4)
  if (activeFilter.value === 3) return reviews.value.filter((r) => r.score === 3)
  if (activeFilter.value === 1) return reviews.value.filter((r) => r.score <= 2)
  return reviews.value
})

function toggleCollapse(id: number) {
  if (collapsed.value.has(id)) {
    collapsed.value.delete(id)
  } else {
    collapsed.value.add(id)
  }
  // 触发响应式
  collapsed.value = new Set(collapsed.value)
}

function filterReviews() {
  // computed 会自动处理
}

async function fetchReviews(page = 1) {
  loading.value = true
  try {
    const res = await getMyComments(page, pageSize.value)
    const data = res.data
    reviews.value = data.records || []
    total.value = data.total || 0
    currentPage.value = page

    // 初始化折叠：超过100字的默认收起
    reviews.value.forEach((r) => {
      if (r.content && r.content.length > 100) {
        collapsed.value.add(r.id)
      }
    })
  } catch {
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function previewImages(images: string[], idx: number) {
  // 简单预览：点击打开新窗口查看
  window.open(images[idx], '_blank')
}

onMounted(() => {
  fetchReviews()
})
</script>

<style scoped>
.mr-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.mr-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px 20px 40px;
}
.mr-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 16px;
}

/* Filter */
.mr-filter {
  display: flex;
  gap: 4px;
  background: #fff;
  border-radius: 8px;
  padding: 8px 16px;
  margin-bottom: 16px;
}
.mr-filter-item {
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}
.mr-filter-item.active {
  background: #e4393c;
  color: #fff;
}

/* Empty */
.mr-empty {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
}

/* Card */
.mr-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px 20px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.mr-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.mr-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.mr-info {
  flex: 1;
  min-width: 0;
}
.mr-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
}
.mr-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}
.mr-content {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  margin-bottom: 8px;
  white-space: pre-wrap;
}
.mr-collapsed {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.mr-expand {
  font-size: 13px;
  color: #1890ff;
  cursor: pointer;
  margin-bottom: 8px;
}

/* Images */
.mr-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.mr-img-item {
  width: 76px;
  height: 76px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #eee;
}
.mr-img-item:hover {
  opacity: 0.85;
}

/* Reply */
.mr-reply {
  font-size: 13px;
  color: #666;
  background: #f9f9f9;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 6px;
  line-height: 1.6;
}
.mr-reply-label {
  color: #e4393c;
  font-weight: 500;
}
.mr-reply-time {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

/* Pager */
.mr-pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
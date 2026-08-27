<template>
  <div class="comment-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入商品名称" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="searchForm.score" placeholder="全部" clearable style="width:100px">
            <el-option v-for="i in 5" :key="i" :label="i + ' 星'" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:110px">
            <el-option label="正常" :value="1" />
            <el-option label="已隐藏" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="评价时间">
          <el-date-picker
            v-model="searchForm.startTime"
            type="datetime"
            placeholder="开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:170px"
          />
          <span style="margin:0 6px">~</span>
          <el-date-picker
            v-model="searchForm.endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:170px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="评价ID" width="80" />
        <el-table-column label="商品信息" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <el-image :src="row.productImage" style="width:40px;height:40px" fit="cover" />
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="{ row }">{{ row.username }}</template>
        </el-table-column>
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="200">
          <template #default="{ row }">
            <el-tooltip :content="row.content" placement="top" :disabled="!row.content || row.content.length < 30">
              <span class="content-ellipsis">{{ row.content }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="回复内容" min-width="160">
          <template #default="{ row }">
            <span v-if="row.reply" class="content-ellipsis">{{ row.reply }}</span>
            <span v-else style="color:#ccc">未回复</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="!row.reply" type="success" link size="small" @click="handleReply(row)">回复</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="评价详情" width="650px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评价ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="商品">
            <div style="display:flex;align-items:center;gap:8px">
              <el-image :src="detailData.productImage" style="width:30px;height:30px" fit="cover" />
              <span>{{ detailData.productName }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="用户">{{ detailData.username }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate :model-value="detailData.score" disabled size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="评价内容" :span="2">{{ detailData.content }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.imageList && detailData.imageList.length" label="评价图片" :span="2">
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-image
                v-for="(img, idx) in detailData.imageList"
                :key="idx"
                :src="img"
                style="width:60px;height:60px"
                fit="cover"
                :preview-src-list="detailData?.imageList"
                preview-teleported
              />
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="评价时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailData.status === 1 ? 'success' : 'info'" size="small">
              {{ detailData.status === 1 ? '正常' : '隐藏' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.reply" label="回复内容" :span="2">{{ detailData.reply }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.replyTime" label="回复时间">{{ detailData.replyTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <div v-else style="text-align:center;padding:40px;color:#999">暂无数据</div>
      <template #footer>
        <el-button v-if="detailData && !detailData.reply" type="success" @click="handleReplyFromDetail">回复</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复评价" width="500px">
      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="评价内容">
          <div style="color:#666;font-size:13px;padding:4px 0">{{ replyingRow?.content }}</div>
        </el-form-item>
        <el-form-item label="回复内容" prop="reply">
          <el-input v-model="replyForm.reply" type="textarea" :rows="4" placeholder="请输入回复内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCommentPage,
  getCommentDetail,
  replyComment,
  toggleCommentStatus,
  deleteComment,
  type CommentDTO,
} from '../../api/comment'

const loading = ref(false)
const tableData = ref<CommentDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

const searchForm = reactive({
  productName: '',
  score: undefined as number | undefined,
  status: undefined as number | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params: any = { current: current.value, size: size.value }
    if (searchForm.productName) params.productName = searchForm.productName
    if (searchForm.score !== undefined) params.score = searchForm.score
    if (searchForm.status !== undefined) params.status = searchForm.status
    if (searchForm.startTime) params.startTime = searchForm.startTime
    if (searchForm.endTime) params.endTime = searchForm.endTime
    const res: any = await getCommentPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  current.value = 1
  fetchData()
}

function handleReset() {
  searchForm.productName = ''
  searchForm.score = undefined
  searchForm.status = undefined
  searchForm.startTime = undefined
  searchForm.endTime = undefined
  current.value = 1
  fetchData()
}

// 详情
const detailVisible = ref(false)
const detailData = ref<CommentDTO | null>(null)

async function handleDetail(row: CommentDTO) {
  detailVisible.value = true
  detailData.value = null
  try {
    const res: any = await getCommentDetail(row.id)
    detailData.value = res.data
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  }
}

// 回复
const replyVisible = ref(false)
const replying = ref(false)
const replyingRow = ref<CommentDTO | null>(null)
const replyFormRef = ref<FormInstance>()
const replyForm = reactive({ reply: '' })
const replyRules: FormRules = {
  reply: [{ required: true, message: '请输入回复内容', trigger: 'blur' }],
}

function handleReply(row: CommentDTO) {
  replyingRow.value = row
  replyForm.reply = ''
  replyVisible.value = true
}

function handleReplyFromDetail() {
  if (detailData.value) {
    replyingRow.value = detailData.value
    replyForm.reply = ''
    replyVisible.value = true
  }
}

async function submitReply() {
  const valid = await replyFormRef.value?.validate().catch(() => false)
  if (!valid || !replyingRow.value) return
  replying.value = true
  try {
    await replyComment(replyingRow.value.id, replyForm.reply)
    ElMessage.success('回复成功')
    replyVisible.value = false
    detailVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '回复失败')
  } finally {
    replying.value = false
  }
}

// 切换状态
async function handleToggleStatus(row: CommentDTO) {
  const newStatus = row.status === 1 ? 2 : 1
  const label = newStatus === 2 ? '隐藏' : '显示'
  ElMessageBox.confirm(`确定${label}该评价？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await toggleCommentStatus(row.id, newStatus)
        ElMessage.success(`${label}成功`)
        fetchData()
      } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
      }
    })
    .catch(() => {})
}

// 删除
function handleDelete(row: CommentDTO) {
  ElMessageBox.confirm(`确定删除评价 #${row.id}？此操作不可恢复。`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteComment(row.id)
        ElMessage.success('删除成功')
        fetchData()
      } catch (e: any) {
        ElMessage.error(e.message || '删除失败')
      }
    })
    .catch(() => {})
}
</script>

<style scoped>
.comment-manage {
  max-width: 1400px;
  margin: 0 auto;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.content-ellipsis {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
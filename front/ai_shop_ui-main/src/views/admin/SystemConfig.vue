<template>
  <div class="system-config">
    <el-card header="系统配置">
      <el-form label-width="180px">
        <el-form-item label="AI 智能审核">
          <el-switch
            v-model="aiReviewEnabled"
            active-text="已开启"
            inactive-text="已关闭"
            @change="onToggleAiReview"
          />
          <div class="config-tip">
            开启后，评论和商品内容将自动进行 AI 违规检测，违规内容将被拦截或标记为待审核。
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const aiReviewEnabled = ref(true)

onMounted(async () => {
  try {
    const res = await request.get<any>('/admin/system-config/ai-review')
    if (res.data) aiReviewEnabled.value = res.data.enabled
  } catch { /* ignore */ }
})

async function onToggleAiReview(val: boolean) {
  try {
    await request.put('/admin/system-config/ai-review', { enabled: val })
    ElMessage.success(val ? 'AI 审核已开启' : 'AI 审核已关闭')
  } catch {
    aiReviewEnabled.value = !val
    ElMessage.error('设置失败')
  }
}
</script>

<style scoped>
.system-config { padding: 20px; }
.config-tip { font-size: 12px; color: #909399; margin-top: 8px; }
</style>
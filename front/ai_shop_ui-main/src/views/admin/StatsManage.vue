<template>
  <div class="stats-dashboard">
    <h2 style="margin-bottom:20px">📊 数据统计 / 报表中心</h2>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">总销售额</div>
          <div class="stat-value money">¥{{ formatMoney(stats.totalSales) }}</div>
          <div class="stat-desc">已完成订单实付金额</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">总订单量</div>
          <div class="stat-value">{{ stats.totalOrders }}</div>
          <div class="stat-desc">全部订单数量</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">用户数</div>
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-desc">平台注册用户（不含管理员）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">优惠券使用率</div>
          <div class="stat-value rate">{{ stats.couponUsageRate }}</div>
          <div class="stat-desc">已使用 / 已领取</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:16px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">售后率</div>
          <div class="stat-value rate">{{ stats.afterSaleRate }}</div>
          <div class="stat-desc">售后单数 / 总订单数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 商品销量排行 -->
    <el-card shadow="hover" style="margin-top:24px">
      <template #header>
        <span style="font-weight:bold;font-size:16px">🏆 商品销量排行（TOP 10）</span>
      </template>
      <el-table :data="stats.productSalesRank" stripe v-loading="loading">
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <span v-if="$index === 0" style="color:#e6a23c;font-size:18px">🥇</span>
            <span v-else-if="$index === 1" style="color:#909399;font-size:18px">🥈</span>
            <span v-else-if="$index === 2" style="color:#cd7f32;font-size:18px">🥉</span>
            <span v-else>{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品图片" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.productImage"
              :src="row.productImage"
              style="width:48px;height:48px;border-radius:4px"
              fit="cover"
            />
            <el-tag v-else type="info" size="small">无图</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="200" />
        <el-table-column prop="totalSales" label="总销量" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="success">{{ row.totalSales }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="(!stats.productSalesRank || stats.productSalesRank.length === 0) && !loading" description="暂无销量数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStatsSummary, type StatsDTO } from '../../api/stats'

const loading = ref(false)
const stats = reactive<StatsDTO>({
  totalSales: 0,
  totalOrders: 0,
  totalUsers: 0,
  couponUsageRate: '0.00%',
  afterSaleRate: '0.00%',
  productSalesRank: [],
})

function formatMoney(val: number): string {
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchStats() {
  loading.value = true
  try {
    const res: any = await getStatsSummary()
    const data = res.data
    stats.totalSales = data.totalSales ?? 0
    stats.totalOrders = data.totalOrders ?? 0
    stats.totalUsers = data.totalUsers ?? 0
    stats.couponUsageRate = data.couponUsageRate ?? '0.00%'
    stats.afterSaleRate = data.afterSaleRate ?? '0.00%'
    stats.productSalesRank = data.productSalesRank ?? []
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.stats-dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

.stat-card {
  text-align: center;
  min-height: 140px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.stat-value.money {
  color: #e6a23c;
}

.stat-value.rate {
  color: #409eff;
}

.stat-desc {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
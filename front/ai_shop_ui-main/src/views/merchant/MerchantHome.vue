<template>
  <div class="md-page">
    <div v-loading="loading" class="md-content">
      <!-- 统计卡片 -->
      <el-row :gutter="16" class="md-row">
        <el-col :span="6">
          <div class="md-card md-card-blue">
            <div class="md-card-value">{{ overview.todayOrderCount }}</div>
            <div class="md-card-label">今日订单</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="md-card md-card-green">
            <div class="md-card-value">¥{{ fmtPrice(overview.todayRevenue) }}</div>
            <div class="md-card-label">今日成交额</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="md-card md-card-orange">
            <div class="md-card-value">{{ overview.pendingShipCount }}</div>
            <div class="md-card-label">待发货</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="md-card md-card-purple">
            <div class="md-card-value">{{ overview.onSaleProductCount }}</div>
            <div class="md-card-label">在售商品</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="md-row">
        <el-col :span="6">
          <div class="md-card md-card-cyan">
            <div class="md-card-value">{{ overview.yesterdayOrderCount }}</div>
            <div class="md-card-label">昨日订单</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="md-card md-card-cyan">
            <div class="md-card-value">¥{{ fmtPrice(overview.yesterdayRevenue) }}</div>
            <div class="md-card-label">昨日成交额</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="md-card md-card-red">
            <div class="md-card-value">{{ overview.pendingAfterSaleCount }}</div>
            <div class="md-card-label">待处理售后</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="md-card md-card-purple">
            <div class="md-card-value">{{ overview.followerCount }}</div>
            <div class="md-card-label">关注人数</div>
          </div>
        </el-col>
      </el-row>

      <!-- 订单趋势 & 热销排行 -->
      <el-row :gutter="16" class="md-row">
        <el-col :span="14">
          <div class="md-chart-card">
            <div class="md-chart-title">
              <span>近7天订单趋势</span>
              <el-tag size="small" type="info">近7日总订单 {{ overview.weekOrderCount }}，总成交额 ¥{{ fmtPrice(overview.weekRevenue) }}</el-tag>
            </div>
            <div class="md-chart-body">
              <div v-if="trend.length === 0" class="md-chart-empty">暂无数据</div>
              <div v-else class="md-trend-chart">
                <div class="md-trend-bars">
                  <div v-for="(item, idx) in trend" :key="idx" class="md-trend-col">
                    <div class="md-trend-bar-wrapper">
                      <div
                        class="md-trend-bar"
                        :style="{ height: barHeight(item.count) + '%' }"
                        :title="item.date + ' 订单数: ' + item.count"
                      >
                      </div>
                    </div>
                    <div class="md-trend-label">{{ item.date }}</div>
                    <div class="md-trend-value">{{ item.count }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="10">
          <div class="md-chart-card">
            <div class="md-chart-title">热销商品排行</div>
            <div class="md-chart-body">
              <div v-if="ranking.length === 0" class="md-chart-empty">暂无销售数据</div>
              <div v-else class="md-ranking-list">
                <div v-for="(item, idx) in ranking" :key="idx" class="md-ranking-item">
                  <span class="md-ranking-num" :class="{ 'md-ranking-top': idx < 3 }">{{ idx + 1 }}</span>
                  <img :src="item.productImage || ''" class="md-ranking-img" />
                  <div class="md-ranking-info">
                    <div class="md-ranking-name">{{ item.productName }}</div>
                    <div class="md-ranking-meta">销量 {{ item.totalSold }} | 金额 ¥{{ fmtPrice(item.totalAmount) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMerchantStatsOverview, getMerchantSalesRanking, getMerchantOrderTrend } from '@/api/merchant'

const loading = ref(false)
const overview = ref<any>({
  todayOrderCount: 0,
  todayRevenue: 0,
  yesterdayOrderCount: 0,
  yesterdayRevenue: 0,
  weekOrderCount: 0,
  weekRevenue: 0,
  pendingShipCount: 0,
  pendingAfterSaleCount: 0,
  onSaleProductCount: 0,
  followerCount: 0,
})
const trend = ref<any[]>([])
const ranking = ref<any[]>([])

function fmtPrice(v: any) {
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}

function barHeight(count: number): number {
  const max = Math.max(...trend.value.map((i: any) => i.count), 1)
  return Math.max((count / max) * 100, 2)
}

async function fetchData() {
  loading.value = true
  try {
    const [overviewRes, rankingRes, trendRes] = await Promise.all([
      getMerchantStatsOverview(),
      getMerchantSalesRanking(),
      getMerchantOrderTrend(),
    ])
    overview.value = { ...overview.value, ...overviewRes.data }
    ranking.value = rankingRes.data || []
    trend.value = trendRes.data || []
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.md-page { min-height: 100%; }
.md-content { max-width: 1400px; }
.md-row { margin-bottom: 16px !important; }

.md-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.md-card-value { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
.md-card-label { font-size: 13px; color: #999; }
.md-card-blue .md-card-value { color: #409eff; }
.md-card-green .md-card-value { color: #67c23a; }
.md-card-orange .md-card-value { color: #e6a23c; }
.md-card-purple .md-card-value { color: #7c3aed; }
.md-card-cyan .md-card-value { color: #00bcd4; }
.md-card-red .md-card-value { color: #f56c6c; }

.md-chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.md-chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.md-chart-body { min-height: 260px; }
.md-chart-empty {
  text-align: center;
  color: #999;
  padding: 60px 0;
  font-size: 14px;
}

/* 柱状图 */
.md-trend-chart { padding: 10px 0; }
.md-trend-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 200px;
  padding: 0 10px;
}
.md-trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.md-trend-bar-wrapper {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.md-trend-bar {
  width: 40px;
  background: linear-gradient(to top, #409eff, #6cb4ff);
  border-radius: 4px 4px 0 0;
  min-height: 2px;
  transition: height 0.5s;
}
.md-trend-bar:hover { opacity: 0.8; }
.md-trend-label { font-size: 12px; color: #999; }
.md-trend-value { font-size: 13px; color: #666; font-weight: 600; }

/* 排行榜 */
.md-ranking-list { display: flex; flex-direction: column; gap: 8px; }
.md-ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background 0.2s;
}
.md-ranking-item:hover { background: #f5f7fa; }
.md-ranking-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  background: #f0f0f0;
  flex-shrink: 0;
}
.md-ranking-top { color: #fff !important; }
.md-ranking-item:nth-child(1) .md-ranking-num { background: #e6a23c; }
.md-ranking-item:nth-child(2) .md-ranking-num { background: #909399; }
.md-ranking-item:nth-child(3) .md-ranking-num { background: #cd7f32; }
.md-ranking-img {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}
.md-ranking-info { flex: 1; min-width: 0; }
.md-ranking-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.md-ranking-meta { font-size: 12px; color: #999; margin-top: 2px; }
</style>
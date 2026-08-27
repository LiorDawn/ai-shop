<template>
  <div class="search-bar">
    <div class="container search-bar-inner">
      <div class="sb-left-area">
        <a v-if="showBackHome" class="sb-back-home" @click.prevent="onBackHome">
          <span class="sb-back-arrow">&#8592;</span>
          <span>返回首页</span>
        </a>
        <div class="sb-logo" @click="onLogoClick">
          <div class="sb-logo-inner">
            <img src="../../assets/AiShop.jpg" alt="AiShop" class="sb-logo-img" />
            <div class="sb-logo-text">
              <div class="sb-logo-title">{{ logoText }}</div>
              <div class="sb-logo-sub">{{ logoSub }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="sb-search">
        <div class="sb-search-box">
          <input
            :value="keyword"
            class="sb-search-input"
            placeholder="请输入搜索商品关键词"
            @input="$emit('update:keyword', ($event.target as HTMLInputElement).value)"
            @keyup.enter="onSearch"
          />
          <button class="sb-search-btn" @click="onSearch">搜索</button>
        </div>
        <div class="sb-search-hot">
          <span class="sb-hot-label">热搜：</span>
          <span
            v-for="w in hotWords"
            :key="w"
            class="sb-hot-word"
            @click="onHotWordClick(w)"
          >{{ w }}</span>
        </div>
      </div>

      <div class="sb-right"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  keyword: string
  hotWords: string[]
  logoText?: string
  logoSub?: string
  logoShort?: string
  showBackHome?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  keyword: '',
  hotWords: () => ['夏日T恤', '连衣裙', '运动鞋', '蓝牙耳机', '护肤品', '家居好物', '男士护肤', '休闲鞋靴', '手机配件'],
  logoText: '智汇购',
  logoSub: 'AISHOP',
  logoShort: 'AI',
  showBackHome: false,
})

const emit = defineEmits<{
  (e: 'update:keyword', val: string): void
  (e: 'search'): void
  (e: 'logoClick'): void
  (e: 'backHomeClick'): void
}>()

function onSearch() {
  emit('search')
}

function onLogoClick() {
  emit('logoClick')
}

function onBackHome() {
  emit('backHomeClick')
}

function onHotWordClick(w: string) {
  emit('update:keyword', w)
  emit('search')
}
</script>

<style scoped>
.container {
  width: 90%;
  max-width: 1900px;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
}

.search-bar {
  width: 100%;
  background: #fff;
}

.search-bar-inner {
  height: 145px;
  display: grid;
  grid-template-columns: auto 1fr 240px;
  gap: 16px;
  align-items: center;
  padding-left: 0;
}

.sb-left-area {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  margin-left: 48px;
}
.sb-back-home {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  white-space: nowrap;
  text-decoration: none;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.15s;
}
.sb-back-home:hover {
  color: #ff0f23;
  background: #fff1eb;
}
.sb-back-arrow {
  font-size: 16px;
  line-height: 1;
}

.sb-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  min-width: 0;
}

.sb-logo-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sb-logo-img {
  width: 88px;
  height: 88px;
  border-radius: 14px;
  object-fit: contain;
  flex-shrink: 0;
}

.sb-logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.sb-logo-title {
  font-size: 32px;
  font-weight: 900;
  color: #222;
  letter-spacing: 2px;
  font-family: 'PingFang SC', 'Microsoft YaHei', '黑体', 'SimHei', sans-serif;
}
.sb-logo-sub {
  font-size: 13px;
  color: #ff0f23;
  letter-spacing: 7px;
  margin-top: 4px;
  font-weight: 700;
}

.sb-search {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.sb-search-box {
  display: flex;
  align-items: center;
  border: 2px solid #ff0f23;
  border-radius: 8px;
  overflow: hidden;
  height: 46px;
  background: #fff;
  width: 100%;
  max-width: 620px;
}

.sb-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: #fff;
  padding: 0 18px;
  font-size: 15px;
  color: #1f1f1f;
  height: 100%;
}

.sb-search-input::placeholder {
  color: #999;
  font-size: 13px;
}

.sb-search-btn {
  width: 90px;
  height: 38px;
  background: #ff0f23;
  color: #fff;
  font-size: 17px;
  font-family: PingFang SC, sans-serif;
  text-align: center;
  line-height: 38px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-right: 4px;
  flex-shrink: 0;
  transition: background 0.15s;
}

.sb-search-btn:hover {
  background: #ff6a00;
}

.sb-search-hot {
  margin-top: 10px;
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.sb-hot-label {
  color: #999;
  font-weight: 600;
}

.sb-hot-word {
  color: #999;
  cursor: pointer;
  transition: color 0.15s;
}

.sb-hot-word:hover {
  color: #ff0f23;
  font-weight: 600;
}

.sb-right {
  min-width: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-bar-inner {
    height: auto;
    padding: 12px 16px;
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .sb-left-area {
    justify-content: center;
  }
  .sb-logo-img {
    width: 40px;
    height: 40px;
  }
  .sb-logo-title {
    font-size: 18px;
  }
  .sb-logo-sub {
    font-size: 10px;
    letter-spacing: 3px;
  }
  .sb-right {
    display: none;
  }
  .sb-search-box {
    max-width: 100%;
  }
  .sb-search-hot {
    gap: 8px;
  }
}
</style>

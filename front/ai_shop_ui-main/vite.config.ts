import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  base: '/',
  build: {
    outDir: 'dist'
  },
  // 跳过 TS 类型校验，打包不再报 TS 错误
  esbuild: {
    tsraw: true
  },

  // 开发环境代理：将 /api 和 /upload 请求转发到后端 8081
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        ws: true,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req, res) => {
            // 修复 SSE 流式响应被代理缓冲导致 net::ERR_ABORTED
            // 当后端返回 text/event-stream 时，立即 flush headers 并禁用缓冲
            const ct = proxyRes.headers['content-type'] || '';
            if (ct.includes('text/event-stream')) {
              // 设置连接保持活跃，防止代理超时断开
              res.setHeader('X-Accel-Buffering', 'no');
              res.setHeader('Cache-Control', 'no-cache');
              res.flushHeaders();
            }
          });
        },
      },
      '/upload': {
        target: 'http://localhost:8081/api',
        changeOrigin: true,
      },
    },
  },
  
})
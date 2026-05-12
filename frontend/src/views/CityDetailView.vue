<script setup>
import { ref, onMounted, computed, nextTick, watchEffect } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getCityByName, getCityWeather } from '@/api';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

const route = useRoute();
const router = useRouter();

const city = ref(null);
const loading = ref(true);
const error = ref(null);
const activeTag = ref(0);

// 天气预报数据
const weatherData = ref([]);
const weatherLoading = ref(false);

onMounted(async () => {
  const name = route.params.name;
  try {
    const res = await getCityByName(name);
    const data = res.data;
    if (data && data.length > 0) {
      city.value = data[0];
      // 加载完城市信息后再拉天气，用后端返回的准确城市名
      fetchWeather(city.value.name);
    } else {
      error.value = `未找到城市"${name}"的信息`;
    }
  } catch (e) {
    error.value = '获取城市信息失败，请稍后重试';
    console.error(e);
  } finally {
    loading.value = false;
  }
});

async function fetchWeather(cityName) {
  weatherLoading.value = true;
  try {
    const res = await getCityWeather(cityName);
    weatherData.value = res.data || [];
  } catch (e) {
    console.warn('获取天气数据失败:', e);
  } finally {
    weatherLoading.value = false;
  }
}

// 使用 watchEffect 自动追踪 weatherData 和 weatherLoading
// 当它们变化且条件满足时，DOM 更新后自动初始化图表
watchEffect(() => {
  if (weatherData.value.length > 0 && !weatherLoading.value) {
    nextTick(() => {
      initWeatherChart();
    });
  }
});

const features = computed(() => {
  return city.value?.features || [];
});

// 城市背景图（用城市名作为种子）
const cityBgImage = computed(() => {
  // 根据城市名生成一个稳定的数字 hash
  const name = city.value?.name || 'city';
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i);
    hash |= 0; // 转为32位整数
  }
  const seed = Math.abs(hash) % 200 + 10; // 10~209 之间的 picsum ID
  return `https://picsum.photos/id/${seed}/1920/1080`;
});

// 当前活跃的 section（用于 sticky tab 高亮）
const activeSection = ref('overview');

// 滚动到对应 section
function scrollToSection(section) {
  activeSection.value = section;
  const el = document.getElementById(`section-${section}`);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

const activeTagName = computed(() => {
  if (features.value.length === 0) return '';
  return features.value[activeTag.value]?.tagName || '';
});

const activeAttractions = computed(() => {
  if (features.value.length === 0) return [];
  return features.value[activeTag.value]?.attractions || [];
});

// 今日天气
const todayWeather = computed(() => {
  if (weatherData.value.length === 0) return null;
  return weatherData.value[0];
});

// 温度趋势数据
const chartLabels = computed(() => {
  return weatherData.value.map(w => {
    const d = new Date(w.forecastDate);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  });
});

const chartTempData = computed(() => {
  return weatherData.value.map(w => {
    const max = parseFloat(w.tempMax) || 0;
    const min = parseFloat(w.tempMin) || 0;
    return (max + min) / 2;
  });
});

// 天气图标映射
function weatherIcon(text) {
  if (!text) return 'bi bi-question-circle';
  if (text.includes('暴') || text.includes('雷')) return 'bi bi-cloud-lightning-rain-fill';
  if (text.includes('大雨') || text.includes('暴雨')) return 'bi bi-cloud-rain-heavy-fill';
  if (text.includes('中雨')) return 'bi bi-cloud-rain-fill';
  if (text.includes('小雨') || text.includes('阵雨')) return 'bi bi-cloud-drizzle-fill';
  if (text.includes('雪')) return 'bi bi-snow2';
  if (text.includes('雾') || text.includes('霾') || text.includes('沙尘')) return 'bi bi-cloud-fog2-fill';
  if (text.includes('阴')) return 'bi bi-cloud-fill';
  if (text.includes('多云') || text.includes('少云') || text.includes('晴间多云')) return 'bi bi-cloud-sun-fill';
  if (text.includes('晴')) return 'bi bi-brightness-high-fill';
  return 'bi bi-question-circle';
}

function weatherIconColor(text) {
  if (!text) return '#6c757d';
  if (text.includes('暴') || text.includes('雷')) return '#6b21a8';
  if (text.includes('雨')) return '#2563eb';
  if (text.includes('雪')) return '#0891b2';
  if (text.includes('雾') || text.includes('霾') || text.includes('沙尘')) return '#9ca3af';
  if (text.includes('阴')) return '#6b7280';
  if (text.includes('多云') || text.includes('少云') || text.includes('晴间多云')) return '#f59e0b';
  if (text.includes('晴')) return '#f97316';
  return '#6c757d';
}

// 温度曲线图初始化
let chartInstance = null;

function initWeatherChart() {
  // 清除旧实例
  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }

  const canvas = document.getElementById('weatherChart');
  if (!canvas || weatherData.value.length === 0) {
    // 如果 canvas 还没渲染出来，用 requestAnimationFrame 再试一次
    if (!canvas && weatherData.value.length > 0) {
      requestAnimationFrame(() => {
        const retryCanvas = document.getElementById('weatherChart');
        if (retryCanvas) {
          const ctx = retryCanvas.getContext('2d');
          buildChart(ctx);
        }
      });
    }
    return;
  }

  const ctx = canvas.getContext('2d');
  buildChart(ctx);
}

function buildChart(ctx) {
  const labels = weatherData.value.map(w => {
    const d = new Date(w.forecastDate);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  });

  const highTemps = weatherData.value.map(w => parseFloat(w.tempMax) || 0);
  const lowTemps = weatherData.value.map(w => parseFloat(w.tempMin) || 0);
  const avgTemps = weatherData.value.map((_, i) => (highTemps[i] + lowTemps[i]) / 2);

  renderChart(ctx, labels, highTemps, lowTemps, avgTemps);
}

function renderChart(ctx, labels, highTemps, lowTemps, avgTemps) {
  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '最高温度 (°C)',
          data: highTemps,
          borderColor: '#ef4444',
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          borderWidth: 2,
          tension: 0.3,
          pointBackgroundColor: '#ef4444',
          pointRadius: 4,
          pointHoverRadius: 6
        },
        {
          label: '平均温度 (°C)',
          data: avgTemps,
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.1)',
          borderWidth: 2,
          tension: 0.3,
          pointBackgroundColor: '#2563eb',
          pointRadius: 4,
          pointHoverRadius: 6,
          borderDash: [5, 5]
        },
        {
          label: '最低温度 (°C)',
          data: lowTemps,
          borderColor: '#06b6d4',
          backgroundColor: 'rgba(6, 182, 212, 0.1)',
          borderWidth: 2,
          tension: 0.3,
          pointBackgroundColor: '#06b6d4',
          pointRadius: 4,
          pointHoverRadius: 6
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top',
          labels: { usePointStyle: true, padding: 16 }
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          backgroundColor: 'rgba(255,255,255,0.95)',
          titleColor: '#333',
          bodyColor: '#666',
          borderColor: '#ddd',
          borderWidth: 1,
          padding: 10
        }
      },
      scales: {
        y: {
          beginAtZero: false,
          ticks: {
            callback: (v) => v + '°C'
          },
          grid: { color: 'rgba(0,0,0,0.05)' }
        },
        x: {
          grid: { display: false }
        }
      },
      interaction: {
        mode: 'nearest',
        axis: 'x',
        intersect: false
      }
    }
  });
}

const goBack = () => {
  router.push('/');
};

// 热度颜色映射
const hotnessColor = (score) => {
  if (score >= 80) return 'var(--bs-success)';
  if (score >= 60) return 'var(--bs-warning)';
  return 'var(--bs-secondary)';
};

const hotnessLabel = (score) => {
  if (score >= 80) return '热门';
  if (score >= 60) return '较热';
  if (score >= 40) return '温';
  return '冷门';
};

const hotnessIcon = (score) => {
  if (score >= 80) return 'bi bi-fire';
  if (score >= 60) return 'bi bi-sun';
  if (score >= 40) return 'bi bi-sprout';
  return 'bi bi-snow';
};

// 日期格式化
function formatDate(dateStr) {
  const d = new Date(dateStr);
  const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
  const today = new Date();
  const diff = Math.floor((d - today) / (1000 * 60 * 60 * 24));
  let label = '';
  if (diff === 0) label = '今天';
  else if (diff === 1) label = '明天';
  else label = `${weekdays[d.getDay()]}`;
  return `${d.getMonth() + 1}/${d.getDate()} ${label}`;
}
</script>

<template>
  <div class="city-detail-page">
    <!-- 加载状态（独立于英雄大屏） -->
    <div v-if="loading" class="container my-5 text-center" style="padding-top: 120px;">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
      <p class="mt-2 text-muted">正在加载城市信息...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="container my-5" style="padding-top: 120px;">
      <div class="alert alert-danger d-flex align-items-center" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <div>{{ error }}</div>
      </div>
      <button class="btn btn-outline-primary" @click="goBack">
        <i class="bi bi-arrow-left"></i> 返回首页
      </button>
    </div>

    <!-- 城市详情 -->
    <template v-else-if="city">
      <!-- ====== 英雄大屏 ====== -->
      <section class="hero-section position-relative overflow-hidden">
        <!-- 背景图 -->
        <div class="hero-bg" :style="{ backgroundImage: 'url(' + cityBgImage + ')' }">
          <div class="hero-overlay"></div>
          <!-- 返回按钮 -->
          <div class="position-absolute top-0 start-0 z-3 p-3">
            <button class="btn btn-sm btn-outline-light border-opacity-25 rounded-circle d-flex align-items-center justify-content-center"
                    style="width: 44px; height: 44px;" @click="goBack"
                    aria-label="返回首页">
              <i class="bi bi-arrow-left" aria-hidden="true"></i>
            </button>
          </div>
          <!-- 城市信息 -->
          <div class="container h-100 d-flex flex-column justify-content-end pb-5 position-relative z-2">
            <div class="d-flex flex-wrap align-items-end justify-content-between">
              <div>
                <h1 class="hero-title text-white fw-bold mb-2">{{ city.name }}</h1>
                <div class="d-flex flex-wrap align-items-center gap-3 text-white-50 mb-2">
                  <span><i class="bi bi-geo-alt-fill text-white-75 me-1"></i>{{ city.province }}</span>
                  <span v-if="city.pinyin" class="text-white-50 small">· {{ city.pinyin }}</span>
                </div>
                <p class="hero-desc text-white-75 mb-0 d-none d-md-block">{{ city.description }}</p>
              </div>
              <div class="hero-stats d-flex gap-3 mt-3 mt-md-0">
                <div class="hero-stat text-center px-3 py-2">
                  <div class="fs-4 fw-bold text-white">{{ city.hotnessScore ?? '--' }}</div>
                  <div class="small text-white-50">热度评分</div>
                </div>
                <div class="hero-stat text-center px-3 py-2">
                  <div class="fs-4 fw-bold d-flex align-items-center justify-content-center gap-1" :style="{ color: hotnessColor(city.hotnessScore) }">
                    <i :class="hotnessIcon(city.hotnessScore)"></i>
                    {{ hotnessLabel(city.hotnessScore) }}
                  </div>
                  <div class="small text-white-50">热度等级</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ====== 粘性标签栏 ====== -->
      <nav class="sticky-tabs bg-white shadow-sm sticky-top border-bottom">
        <div class="container">
          <div class="d-flex overflow-auto" style="scrollbar-width: thin;">
            <button class="sticky-tab px-4 py-3 fw-medium small border-0 bg-transparent"
                    :class="{ active: activeSection === 'overview' }"
                    @click="scrollToSection('overview')">
              <i class="bi bi-info-circle me-1"></i>城市概览
            </button>
            <button class="sticky-tab px-4 py-3 fw-medium small border-0 bg-transparent"
                    :class="{ active: activeSection === 'weather' }"
                    @click="scrollToSection('weather')">
              <i class="bi bi-cloud-sun me-1"></i>15天天气
            </button>
            <button class="sticky-tab px-4 py-3 fw-medium small border-0 bg-transparent"
                    :class="{ active: activeSection === 'features' }"
                    @click="scrollToSection('features')">
              <i class="bi bi-tags me-1"></i>城市特色
            </button>
          </div>
        </div>
      </nav>

      <!-- ====== 内容主体 ====== -->
      <div class="container my-4">

        <!-- 城市概览 -->
        <section id="section-overview" class="mb-4">
          <div class="overview-card card">
            <div class="card-body p-4">
              <h5 class="fw-bold mb-3">
                <i class="bi bi-info-circle-fill text-primary me-2"></i>城市概览
              </h5>
              <p class="text-secondary mb-0">{{ city.description }}</p>
              <div class="row g-3 mt-3">
                <div class="col-6 col-md-3">
                  <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-geo-alt"></i></div>
                    <small class="d-block" style="color: var(--color-gray-500); font-size: 0.75rem;">省份</small>
                    <span class="fw-semibold" style="color: var(--color-gray-800);">{{ city.province }}</span>
                  </div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-translate"></i></div>
                    <small class="d-block" style="color: var(--color-gray-500); font-size: 0.75rem;">拼音</small>
                    <span class="fw-semibold" style="color: var(--color-gray-800);">{{ city.pinyin || '--' }}</span>
                  </div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-thermometer-half"></i></div>
                    <small class="d-block" style="color: var(--color-gray-500); font-size: 0.75rem;">热度评分</small>
                    <span class="fw-semibold" :style="{ color: hotnessColor(city.hotnessScore) }">
                      {{ city.hotnessScore ?? '--' }}
                    </span>
                  </div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="stat-card">
                    <div class="stat-icon"><i class="bi bi-star"></i></div>
                    <small class="d-block" style="color: var(--color-gray-500); font-size: 0.75rem;">热度等级</small>
                    <span class="badge mt-1" :style="{ backgroundColor: hotnessColor(city.hotnessScore), color: '#fff' }">
                      {{ hotnessLabel(city.hotnessScore) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

      <!-- ====== 15天天气预报板块 ====== -->
      <section id="section-weather" class="mb-4">
        <div class="card shadow-sm border-0">
        <div class="card-header bg-white border-bottom-0 pt-3 pb-0">
          <h5 class="fw-bold mb-0">
            <i class="bi bi-cloud-sun-fill text-primary me-2"></i>
            15天天气预报
          </h5>
        </div>
        <div class="card-body p-4">
          <!-- 加载中 -->
          <div v-if="weatherLoading" class="text-center py-4">
            <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
            <span class="text-muted">加载天气数据...</span>
          </div>

          <!-- 有数据 -->
          <template v-else-if="weatherData.length > 0">
            <!-- 今日天气重点展示 - 白底卡片布局 -->
            <div class="today-card bg-white border rounded-3 p-4 mb-4 shadow-sm">
              <div class="row g-4 align-items-center">
                <!-- 今日温度 + 天气图标 -->
                <div class="col-md-3 text-center">
                  <h6 class="text-muted small mb-2 fw-semibold">今日</h6>
                  <div class="display-4 fw-bold mb-1 today-temp">
                    {{ todayWeather ? todayWeather.tempMax : '--' }}°
                  </div>
                  <div class="d-flex align-items-center justify-content-center gap-2">
                    <i :class="weatherIcon(todayWeather?.text)" :style="{ color: weatherIconColor(todayWeather?.text) }" class="fs-3"></i>
                    <span class="fw-medium" :style="{ color: weatherIconColor(todayWeather?.text) }">{{ todayWeather?.text || '未知' }}</span>
                  </div>
                </div>
                <!-- 中央详情网格 -->
                <div class="col-md-6">
                  <div class="border-start border-end border-light px-4 py-2">
                    <div class="row g-3">
                      <div class="col-6 text-center">
                        <p class="text-muted small mb-1">最低温度</p>
                        <p class="fw-semibold mb-0">{{ todayWeather?.tempMin || '--' }}°C</p>
                      </div>
                      <div class="col-6 text-center">
                        <p class="text-muted small mb-1">降水量</p>
                        <p class="fw-semibold mb-0">{{ todayWeather?.precipitation || '0' }} mm</p>
                      </div>
                      <div class="col-6 text-center">
                        <p class="text-muted small mb-1">能见度</p>
                        <p class="fw-semibold mb-0">{{ todayWeather?.visibility || '--' }} km</p>
                      </div>
                      <div class="col-6 text-center">
                        <p class="text-muted small mb-1">温差</p>
                        <p class="fw-semibold mb-0">
                          {{ todayWeather ? (parseFloat(todayWeather.tempMax) - parseFloat(todayWeather.tempMin)).toFixed(1) : '--' }}°C
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 旅行建议 -->
                <div class="col-md-3 text-center">
                  <p class="text-muted small mb-2">旅行建议</p>
                  <div class="d-flex flex-column align-items-center">
                    <template v-if="todayWeather?.text?.includes('晴')">
                      <i class="bi bi-emoji-sunglasses fs-2 text-warning mb-1"></i>
                      <span class="fw-medium text-success">适合出行</span>
                    </template>
                    <template v-else-if="todayWeather?.text?.includes('雨') || todayWeather?.text?.includes('雪')">
                      <i class="bi bi-umbrella fs-2 text-primary mb-1"></i>
                      <span class="fw-medium text-primary">建议携带雨具</span>
                    </template>
                    <template v-else-if="todayWeather?.text?.includes('雾') || todayWeather?.text?.includes('霾')">
                      <i class="bi bi-shield-exclamation fs-2 text-secondary mb-1"></i>
                      <span class="fw-medium text-secondary">注意防护</span>
                    </template>
                    <template v-else>
                      <i class="bi bi-info-circle fs-2 text-info mb-1"></i>
                      <span class="fw-medium text-info">天气一般</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>

            <!-- 15天温度趋势曲线图 + 逐日天气卡片网格（合并为一个卡片） -->
            <div class="bg-white border rounded-3 p-4 mb-4 shadow-sm">
              <h6 class="fw-bold mb-3">
                <i class="bi bi-graph-up-arrow text-primary me-1"></i>
                15天温度趋势
              </h6>
              <div class="chart-container mb-3" style="height: 280px;">
                <canvas id="weatherChart"></canvas>
              </div>

              <!-- 15天逐日预报 - 卡片网格（放在图表下方，类似参考的grid-cols-3/5/8/15） -->
              <div class="mt-3 pt-3 border-top">
                <div class="row g-2">
                  <div v-for="(w, idx) in weatherData" :key="idx"
                       class="col-4 col-sm-3 col-md-2 col-lg-1">
                    <div class="weather-day-card text-center p-2 rounded-3 border"
                         :class="{ 'weather-today': idx === 0 }">
                      <div class="small text-muted mb-1 fw-medium" style="font-size: 0.7rem;">
                        {{ formatDate(w.forecastDate) }}
                      </div>
                      <i :class="weatherIcon(w.text)" :style="{ color: weatherIconColor(w.text) }"
                         class="d-block fs-5 my-1"></i>
                      <div class="fw-semibold small" :class="idx === 0 ? 'text-primary' : ''">
                        {{ w.tempMin }}° / {{ w.tempMax }}°
                      </div>
                      <div class="text-muted" style="font-size: 0.6rem;">
                        <i class="bi bi-droplet"></i> {{ w.precipitation || '0' }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 无数据 -->
          <div v-else class="text-center py-4 text-muted">
            <i class="bi bi-cloud-slash display-6"></i>
            <p class="mt-2">暂无天气预报数据</p>
          </div>
        </div> <!-- /.card-body -->
      </div> <!-- /.card -->
    </section>

      <!-- 特色标签 Tab 导航 -->
      <section id="section-features" class="mb-4">
        <div v-if="features.length > 0" class="card shadow-sm border-0">
          <div class="card-header bg-white border-bottom-0 pt-3 pb-0">
            <h5 class="fw-bold mb-0">
              <i class="bi bi-tags-fill text-primary me-2"></i>
              城市特色
            </h5>
          </div>
          <div class="card-body">
          <!-- Tab 导航 -->
          <ul class="nav nav-tabs nav-fill flex-nowrap overflow-auto pb-1" style="scrollbar-width: thin;">
            <li class="nav-item" v-for="(feature, idx) in features" :key="feature.id">
              <button class="nav-link" :class="{ active: activeTag === idx }"
                      @click="activeTag = idx">
                <i class="bi"
                   :class="{
                     'bi-tree-fill': feature.tagName === '自然风光',
                     'bi-building-fill': feature.tagName === '历史文化',
                     'bi-rocket-takeoff-fill': feature.tagName === '主题乐园',
                     'bi-eye-fill': feature.tagName === '城市观光',
                     'bi-cup-hot-fill': feature.tagName === '美食文化',
                     'bi-water-fill': feature.tagName === '海滨休闲',
                     'bi-heart-pulse-fill': feature.tagName === '休闲康养',
                     'bi-bicycle-fill': feature.tagName === '户外运动',
                     'bi-church-fill': feature.tagName === '宗教信仰',
                     'bi-megaphone-fill': feature.tagName === '节庆民俗',
                     'bi-star-fill': true
                   }">
                </i>
                <span class="ms-1 d-none d-sm-inline">{{ feature.tagName }}</span>
              </button>
            </li>
          </ul>

          <!-- 当前标签内容 -->
          <div class="tab-content mt-4">
            <div class="tab-pane fade show active">
              <h5 class="text-primary mb-3">
                <i class="bi bi-geo me-1"></i>
                {{ activeTagName }}
                <span class="badge bg-primary bg-opacity-10 text-primary ms-2">
                  {{ activeAttractions.length }} 个景点
                </span>
              </h5>

              <!-- 景点列表 -->
              <div v-if="activeAttractions.length > 0" class="row g-3">
                <div v-for="attr in activeAttractions" :key="attr.id" class="col-12">
                  <div class="card attraction-card h-100">
                    <div class="card-body py-3">
                      <h6 class="fw-bold mb-2">
                        <i class="bi bi-pin-map-fill text-primary me-1"></i>
                        {{ attr.name }}
                      </h6>
                      <p v-if="attr.description" class="card-text text-secondary small mb-0">
                        {{ attr.description }}
                      </p>
                      <p v-else class="card-text text-muted small mb-0 fst-italic">暂无详细描述</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-else class="text-center py-4 text-muted">
                <i class="bi bi-emoji-neutral display-6"></i>
                <p class="mt-2">该标签下暂无具体景点信息</p>
              </div>
            </div>
          </div>
        </div>
      </div> <!-- /.card (特色标签) -->
      <template v-else>
        <div class="card shadow-sm border-0">
          <div class="card-body text-center py-5 text-muted">
            <i class="bi bi-inbox display-4"></i>
            <p class="mt-2">该城市暂无特色标签数据</p>
          </div>
        </div>
      </template>
  </section>

      <!-- 返回按钮 -->
      <div class="text-center mb-4">
        <button class="btn btn-outline-primary btn-lg px-5" @click="goBack">
          <i class="bi bi-arrow-left me-2"></i>返回首页
        </button>
      </div>
    </div> <!-- /.container -->
    </template>
  </div> <!-- /.city-detail-page -->
</template>

<style scoped>
.city-detail-page {
  min-height: calc(100vh - 56px);
  background: linear-gradient(180deg, var(--color-sky-50) 0%, #FFFFFF 30%);
}

/* ====== Hero Section ====== */
.hero-section {
  height: 55vh;
  min-height: 420px;
}

.hero-bg {
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.2) 0%,
    rgba(0, 0, 0, 0.4) 40%,
    rgba(0, 0, 0, 0.75) 85%,
    rgba(12, 74, 110, 0.9) 100%
  );
  z-index: 1;
}

.hero-title {
  font-size: clamp(2rem, 5vw, 3.5rem);
  text-shadow: 0 2px 12px rgba(0,0,0,0.4);
  letter-spacing: -0.02em;
}

.hero-desc {
  max-width: 600px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ====== Sticky Tabs ====== */
.sticky-tabs {
  z-index: var(--z-sticky);
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.sticky-tab {
  color: var(--color-gray-500);
  cursor: pointer;
  transition: var(--transition-all);
  border-bottom: 3px solid transparent !important;
  white-space: nowrap;
  font-size: 0.85rem;
}
.sticky-tab:hover {
  color: var(--color-sky-600);
}
.sticky-tab.active {
  color: var(--color-sky-600);
  font-weight: 600;
  border-bottom-color: var(--color-sky-500) !important;
}

/* ====== Feature Tabs ====== */
.nav-tabs {
  border-bottom: 2px solid var(--color-border);
}
.nav-tabs .nav-link {
  color: var(--color-gray-500);
  border: none;
  border-bottom: 3px solid transparent;
  transition: var(--transition-all);
  font-size: 0.85rem;
  padding: 10px 16px;
}
.nav-tabs .nav-link:hover {
  color: var(--color-sky-600);
  border-bottom-color: var(--color-sky-300);
  background: none;
}
.nav-tabs .nav-link.active {
  color: var(--color-sky-600);
  font-weight: 600;
  border: none;
  border-bottom: 3px solid var(--color-sky-500);
  background: none;
}

/* ====== Overview Stats ====== */
.stat-card {
  background: linear-gradient(135deg, var(--color-sky-50), #FFFFFF);
  border: 1px solid var(--color-sky-200);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  text-align: center;
  transition: var(--transition-all);
}
.stat-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-sky-300);
}
.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(14, 165, 233, 0.1);
  color: var(--color-sky-500);
  font-size: 1.25rem;
  margin-bottom: 8px;
}

/* ====== Weather Today Card ====== */
.today-card {
  background: linear-gradient(135deg, var(--color-sky-50), #FFFFFF) !important;
  border: 1px solid var(--color-sky-200) !important;
  border-radius: var(--radius-xl) !important;
}
.today-card .border-start,
.today-card .border-end {
  border-color: var(--color-sky-200) !important;
}
.today-temp {
  font-size: 3.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, var(--color-sky-500), var(--color-sky-700));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

/* ====== Weather Day Cards ====== */
.weather-day-card {
  background: #FFFFFF;
  border: 1px solid var(--color-sky-200);
  border-radius: var(--radius-md);
  transition: var(--transition-all);
  cursor: default;
}
.weather-day-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--color-sky-400);
}
.weather-day-card.weather-today {
  background: linear-gradient(135deg, var(--color-sky-100), var(--color-sky-50));
  border-color: var(--color-sky-400) !important;
  box-shadow: 0 2px 12px rgba(14, 165, 233, 0.12);
}

/* ====== Attraction Cards ====== */
.attraction-card {
  border: none !important;
  border-left: 4px solid var(--color-sky-400) !important;
  border-radius: var(--radius-md) !important;
  background: linear-gradient(135deg, #FFFFFF, var(--color-sky-50)) !important;
  transition: var(--transition-all);
}
.attraction-card:hover {
  box-shadow: var(--shadow-md);
  border-left-color: var(--color-sky-500) !important;
}

/* ====== Chart Container ====== */
.chart-container {
  position: relative;
  width: 100%;
}

/* ====== Overview Section Card ====== */
.overview-card {
  background: #FFFFFF;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

/* ====== Hero Stats ====== */
.hero-stat {
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-lg);
}

@media (max-width: 576px) {
  .today-card .border-start,
  .today-card .border-end {
    border: none !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  .weather-day-card:hover {
    transform: none;
  }
}
</style>

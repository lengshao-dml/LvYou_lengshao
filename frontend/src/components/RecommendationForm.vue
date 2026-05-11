<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRecommendationStore } from '@/store/recommendation';
import { useMetaStore } from '@/store/meta';
import { useAuthStore } from '@/store/auth';

const recommendationStore = useRecommendationStore();
const metaStore = useMetaStore();
const authStore = useAuthStore();

// 表单的本地状态
const departureCity = ref(''); // 由城市补全选择
const departureSearchText = ref(''); // 补全用搜索文本
const selectedTags = ref([]);
const travelDate = ref(new Date().toISOString().split('T')[0]); // 默认今天
const distanceScope = ref('ANY'); // 默认"不限"

// 城市自动补全
const filteredCities = computed(() => {
  if (!departureSearchText.value || departureCity.value) {
    return [];
  }
  return metaStore.cities
    .filter(city =>
      city.pinyin.toLowerCase().includes(departureSearchText.value.toLowerCase()) ||
      city.name.includes(departureSearchText.value)
    )
    .slice(0, 5);
});

const selectCity = (city) => {
  departureCity.value = city.name;
  departureSearchText.value = city.name;
};

// 出发城市默认值优先级：上次输入 > 常居地
const setDefaultCity = () => {
  if (recommendationStore.lastDepartureCity) {
    departureCity.value = recommendationStore.lastDepartureCity;
    departureSearchText.value = recommendationStore.lastDepartureCity;
  } else if (authStore.isLoggedIn && authStore.homeCityName) {
    departureCity.value = authStore.homeCityName;
    departureSearchText.value = authStore.homeCityName;
  }
};

// 组件加载时，自动获取标签和城市列表
onMounted(() => {
  metaStore.fetchMeta();
  setDefaultCity();
});

// 监听 authStore.user（异步加载完成时自动填充）
watch(() => authStore.user, (newUser) => {
  if (newUser && newUser.homeCityName && !departureCity.value) {
    setDefaultCity();
  }
});

// 标签图标映射
const tagIcons = {
  '自然风光': 'bi bi-tree',
  '海滨休闲': 'bi bi-water',
  '历史文化': 'bi bi-bank',
  '休闲康养': 'bi bi-cup-hot',
  '主题乐园': 'bi bi-controller',
  '户外运动': 'bi bi-bicycle',
  '城市观光': 'bi bi-building',
  '宗教信仰': 'bi bi-heart',
  '美食文化': 'bi bi-cup-straw',
  '节庆民俗': 'bi bi-gift',
};
const tagIcon = (name) => tagIcons[name] || 'bi bi-tag';

// 距离范围选项
const distanceOptions = [
  { value: 'ANY', label: '不限', desc: '探索无限可能' },
  { value: 'PROVINCE', label: '省内', desc: '发现身边的美好' },
  { value: 'NEARBY_500KM', label: '周边 (500km)', desc: '短途出行好选择' },
];

// 处理标签点击
const toggleTag = (tagName) => {
  const index = selectedTags.value.indexOf(tagName);
  if (index > -1) {
    selectedTags.value.splice(index, 1);
  } else {
    selectedTags.value.push(tagName);
  }
};

// 提交表单
const handleSubmit = async () => {
  const request = {
    departureCity: departureCity.value,
    interestTags: selectedTags.value,
    travelDate: travelDate.value,
    distanceScope: distanceScope.value,
  };

  // 记住本次出发城市
  recommendationStore.lastDepartureCity = departureCity.value;
  localStorage.setItem('lastDepartureCity', departureCity.value);

  await recommendationStore.fetchRecommendations(request);

  // 推荐完成后滚动到结果区域
  const el = document.getElementById('results-section');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <!-- 标题区域 -->
    <div class="text-center mb-5">
      <h2 class="fw-bold text-dark mb-2" style="font-size: clamp(1.8rem, 4vw, 2.5rem);">智能推荐</h2>
      <p class="text-muted">根据您的偏好，为您量身定制旅行方案</p>
    </div>

    <form @submit.prevent="handleSubmit">
      <!-- 推荐表单卡片 -->
      <div class="bg-white rounded-4 shadow-sm p-4 p-md-5 mb-4" style="box-shadow: 0 2px 12px 0 rgba(0,0,0,0.08);">
        <div class="row g-4">
          <!-- 01 出发信息 -->
          <div class="col-lg-4 col-md-12">
            <div class="rec-card h-100">
              <!-- 装饰圆角 -->
              <div class="rec-card-deco"></div>

              <!-- 编号标题 -->
              <div class="d-flex align-items-center mb-4 position-relative" style="z-index: 1;">
                <div class="rec-step-num" style="background: #4080FF;">01</div>
                <h3 class="fs-6 fw-semibold text-dark mb-0 ms-2">出发信息</h3>
                <i class="bi bi-airplane-engines ms-2 text-primary"></i>
              </div>

              <!-- 出发城市 -->
              <div class="mb-4 position-relative" style="z-index: 1;">
                <label class="form-label small text-secondary mb-2">出发城市</label>
                <div class="dropdown">
                  <input type="text"
                    class="form-control rec-input dropdown-toggle"
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                    placeholder="搜索城市..."
                    v-model="departureSearchText"
                    @focus="departureCity = ''"
                    @input="departureCity = ''"
                    required>
                  <ul class="dropdown-menu w-100" :class="{'show': filteredCities.length > 0 && !departureCity}">
                    <li v-for="city in filteredCities" :key="city.id">
                      <a class="dropdown-item" href="#" @click.prevent="selectCity(city)">{{ city.name }}</a>
                    </li>
                  </ul>
                </div>
              </div>

              <!-- 出行日期 -->
              <div class="position-relative" style="z-index: 1;">
                <label class="form-label small text-secondary mb-2">出行日期</label>
                <div class="position-relative">
                  <input type="date" class="form-control rec-input" v-model="travelDate" required>
                  <i class="bi bi-calendar-event position-absolute top-50 end-0 translate-middle-y me-3 text-muted pointer-events-none"></i>
                </div>
              </div>
            </div>
          </div>

          <!-- 02 距离范围 -->
          <div class="col-lg-4 col-md-6">
            <div class="rec-card h-100">
              <div class="rec-card-deco" style="background: rgba(0,196,140,0.08);"></div>

              <div class="d-flex align-items-center mb-4 position-relative" style="z-index: 1;">
                <div class="rec-step-num" style="background: #00C48C;">02</div>
                <h3 class="fs-6 fw-semibold text-dark mb-0 ms-2">距离范围</h3>
                <i class="bi bi-wifi ms-2" style="color: #00C48C;"></i>
              </div>

              <div class="d-flex flex-column gap-3 position-relative" style="z-index: 1;">
                <div v-for="opt in distanceOptions" :key="opt.value" class="d-flex align-items-center">
                  <input type="radio"
                    :id="'scope-'+opt.value"
                    :value="opt.value"
                    v-model="distanceScope"
                    class="rec-radio visually-hidden">
                  <label :for="'scope-'+opt.value" class="d-flex align-items-center cursor-pointer w-100">
                    <div class="rec-radio-circle"
                      :class="{'active': distanceScope === opt.value}">
                      <div class="rec-radio-dot"></div>
                    </div>
                    <div class="ms-3">
                      <p class="fw-medium text-dark mb-0">{{ opt.label }}</p>
                      <p class="text-xs text-muted mb-0">{{ opt.desc }}</p>
                    </div>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- 03 兴趣标签 -->
          <div class="col-lg-4 col-md-6">
            <div class="rec-card h-100">
              <div class="rec-card-deco" style="background: rgba(151,101,245,0.08);"></div>

              <div class="d-flex align-items-center mb-4 position-relative" style="z-index: 1;">
                <div class="rec-step-num" style="background: #9765F5;">03</div>
                <h3 class="fs-6 fw-semibold text-dark mb-0 ms-2">兴趣标签</h3>
              </div>

              <div class="row g-2 position-relative" style="z-index: 1;">
                <div class="col-6" v-for="tag in metaStore.tags" :key="tag.id">
                  <button type="button"
                    class="rec-tag-btn w-100"
                    :class="selectedTags.includes(tag.name) ? 'rec-tag-active' : 'rec-tag-default'"
                    @click="toggleTag(tag.name)">
                    <i :class="tagIcon(tag.name)" class="me-1"></i>
                    {{ tag.name }}
                  </button>
                </div>
              </div>

              <p class="text-xs text-muted text-center mt-3 mb-0 position-relative" style="z-index: 1;">
                <i class="bi bi-info-circle me-1"></i>可多选，推荐更精准
              </p>
            </div>
          </div>
        </div>

        <!-- 提交按钮 -->
        <div class="mt-4">
          <button type="submit" class="btn rec-submit-btn w-100" :disabled="recommendationStore.isLoading">
            <span v-if="recommendationStore.isLoading" class="spinner-border spinner-border-sm me-2" role="status"></span>
            <i v-else class="bi bi-magic me-2"></i>
            {{ recommendationStore.isLoading ? '计算中...' : '获取智能推荐' }}
          </button>
        </div>

        <!-- 底部提示 -->
        <div class="mt-3 text-center">
          <span class="text-xs text-muted me-3"><i class="bi bi-person me-1"></i>个性化推荐</span>
          <span class="text-xs text-muted me-3"><i class="bi bi-map me-1"></i>精选行程</span>
          <span class="text-xs text-muted"><i class="bi bi-shield-check me-1"></i>安心出行</span>
        </div>
      </div>
    </form>
  </div>
</template>

<style scoped>
/* 卡片容器 */
.rec-card {
  background: #f5f7fa;
  border-radius: 0.75rem;
  padding: 1.25rem;
  position: relative;
  overflow: hidden;
}

/* 装饰元素 */
.rec-card-deco {
  position: absolute;
  top: 0;
  left: 0;
  width: 4rem;
  height: 4rem;
  background: rgba(64,128,255,0.05);
  border-bottom-right-radius: 100%;
}

/* 步骤编号 */
.rec-step-num {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 0.875rem;
  flex-shrink: 0;
}

/* 输入框 */
.rec-input {
  border: 1px solid #e5e9f2;
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
}
.rec-input:focus {
  border-color: #4080FF;
  box-shadow: 0 0 0 3px rgba(64,128,255,0.15);
  outline: none;
}

/* 单选按钮 */
.rec-radio-circle {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  border: 2px solid #d0d5dd;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.rec-radio-circle.active {
  border-color: #00C48C;
  background: #00C48C;
}
.rec-radio-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: #fff;
}
.cursor-pointer {
  cursor: pointer;
}

/* 标签按钮 */
.rec-tag-btn {
  border-radius: 0.5rem;
  padding: 0.5rem 0.5rem;
  font-size: 0.85rem;
  font-weight: 500;
  border: 1px solid #e5e9f2;
  background: #fff;
  text-align: center;
  transition: all 0.15s ease;
}
.rec-tag-btn:hover {
  background: #f0f2f5;
}
.rec-tag-active {
  background: rgba(64,128,255,0.1);
  border-color: #4080FF;
  color: #4080FF;
}
.rec-tag-default {
  color: #555;
}

/* 渐变色提交按钮 */
.rec-submit-btn {
  background: linear-gradient(90deg, #4080FF 0%, #9765F5 100%);
  color: #fff;
  border: none;
  border-radius: 0.75rem;
  padding: 1rem;
  font-size: 1.1rem;
  font-weight: 500;
  box-shadow: 0 4px 14px rgba(64,128,255,0.25);
  transition: all 0.2s ease;
}
.rec-submit-btn:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(64,128,255,0.35);
  transform: translateY(-1px);
  color: #fff;
}
.rec-submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 工具类 */
.text-xs {
  font-size: 0.75rem;
}
.max-w-4xl {
  max-width: 56rem;
}
.pointer-events-none {
  pointer-events: none;
}
</style>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRecommendationStore } from '@/store/recommendation';
import { useMetaStore } from '@/store/meta';
import { useAuthStore } from '@/store/auth';

const recommendationStore = useRecommendationStore();
const metaStore = useMetaStore();
const authStore = useAuthStore();

const departureCity = ref('');
const departureSearchText = ref('');
const selectedTags = ref([]);
const travelDate = ref(new Date().toISOString().split('T')[0]);
const distanceScope = ref('ANY');

const filteredCities = computed(() => {
  if (!departureSearchText.value || departureCity.value) return [];
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

const setDefaultCity = () => {
  if (recommendationStore.lastDepartureCity) {
    departureCity.value = recommendationStore.lastDepartureCity;
    departureSearchText.value = recommendationStore.lastDepartureCity;
  } else if (authStore.isLoggedIn && authStore.homeCityName) {
    departureCity.value = authStore.homeCityName;
    departureSearchText.value = authStore.homeCityName;
  }
};

onMounted(() => {
  metaStore.fetchMeta();
  setDefaultCity();
});

watch(() => authStore.user, (newUser) => {
  if (newUser && newUser.homeCityName && !departureCity.value) {
    setDefaultCity();
  }
});

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

const distanceOptions = [
  { value: 'ANY', label: '不限', desc: '探索无限可能' },
  { value: 'PROVINCE', label: '省内', desc: '发现身边的美好' },
  { value: 'NEARBY_500KM', label: '周边 (500km)', desc: '短途出行好选择' },
];

const toggleTag = (tagName) => {
  const index = selectedTags.value.indexOf(tagName);
  if (index > -1) {
    selectedTags.value.splice(index, 1);
  } else {
    selectedTags.value.push(tagName);
  }
};

const handleSubmit = async () => {
  const request = {
    departureCity: departureCity.value,
    interestTags: selectedTags.value,
    travelDate: travelDate.value,
    distanceScope: distanceScope.value,
  };
  recommendationStore.lastDepartureCity = departureCity.value;
  localStorage.setItem('lastDepartureCity', departureCity.value);
  await recommendationStore.fetchRecommendations(request);
  const el = document.getElementById('results-section');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};
</script>

<template>
  <div style="max-width: 56rem; margin: 0 auto;">
    <!-- Section Header -->
    <div class="text-center mb-5">
      <h2 class="fw-bold mb-2" style="font-size: clamp(1.6rem, 4vw, 2.2rem); color: var(--color-gray-800);">智能推荐</h2>
      <p style="color: var(--color-gray-500);">根据您的偏好，为您量身定制旅行方案</p>
    </div>

    <form @submit.prevent="handleSubmit">
      <!-- Form Card -->
      <div class="form-card">
        <div class="row g-4">
          <!-- Step 1: Departure -->
          <div class="col-lg-4 col-md-12">
            <div class="step-card">
              <div class="step-header">
                <span class="step-num" style="background: var(--color-sky-500);">01</span>
                <h3 class="step-title">出发信息</h3>
              </div>

              <div class="mb-4">
                <label class="form-label-sm">出发城市</label>
                <div class="dropdown">
                  <input type="text"
                    class="form-input dropdown-toggle"
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

              <div>
                <label class="form-label-sm">出行日期</label>
                <div class="position-relative">
                  <input type="date" class="form-input" v-model="travelDate" required>
                  <i class="bi bi-calendar-event position-absolute top-50 end-0 translate-middle-y me-3" style="color: var(--color-gray-400); pointer-events: none;"></i>
                </div>
              </div>
            </div>
          </div>

          <!-- Step 2: Distance -->
          <div class="col-lg-4 col-md-6">
            <div class="step-card">
              <div class="step-header">
                <span class="step-num" style="background: var(--color-green-600);">02</span>
                <h3 class="step-title">距离范围</h3>
              </div>

              <div class="d-flex flex-column gap-3">
                <div v-for="opt in distanceOptions" :key="opt.value" class="d-flex align-items-center">
                  <input type="radio"
                    :id="'scope-'+opt.value"
                    :value="opt.value"
                    v-model="distanceScope"
                    class="visually-hidden">
                  <label :for="'scope-'+opt.value" class="radio-label w-100" :class="{ 'radio-active': distanceScope === opt.value }">
                    <div class="radio-circle" :class="{ active: distanceScope === opt.value }">
                      <div class="radio-dot"></div>
                    </div>
                    <div class="ms-3">
                      <p class="fw-medium mb-0" style="color: var(--color-gray-800); font-size: 0.9rem;">{{ opt.label }}</p>
                      <p style="color: var(--color-gray-500); font-size: 0.75rem; margin: 0;">{{ opt.desc }}</p>
                    </div>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- Step 3: Interests -->
          <div class="col-lg-4 col-md-6">
            <div class="step-card">
              <div class="step-header">
                <span class="step-num" style="background: #9765F5;">03</span>
                <h3 class="step-title">兴趣标签</h3>
              </div>

              <div class="row g-2">
                <div class="col-6" v-for="tag in metaStore.tags" :key="tag.id">
                  <button type="button"
                    class="tag-btn w-100"
                    :class="{ 'tag-active': selectedTags.includes(tag.name) }"
                    @click="toggleTag(tag.name)">
                    <i :class="tagIcon(tag.name)" class="me-1"></i>
                    {{ tag.name }}
                  </button>
                </div>
              </div>

              <p class="text-center mt-3 mb-0" style="color: var(--color-gray-400); font-size: 0.75rem;">
                <i class="bi bi-info-circle me-1"></i>可多选，推荐更精准
              </p>
            </div>
          </div>
        </div>

        <!-- Submit -->
        <div class="mt-4">
          <button type="submit" class="submit-btn w-100" :disabled="recommendationStore.isLoading">
            <span v-if="recommendationStore.isLoading" class="spinner-border spinner-border-sm me-2" role="status"></span>
            <i v-else class="bi bi-magic me-2"></i>
            {{ recommendationStore.isLoading ? '计算中...' : '获取智能推荐' }}
          </button>
        </div>

        <!-- Footer -->
        <div class="mt-3 text-center" style="font-size: 0.75rem; color: var(--color-gray-400);">
          <span class="me-3"><i class="bi bi-person me-1"></i>个性化推荐</span>
          <span class="me-3"><i class="bi bi-map me-1"></i>精选行程</span>
          <span><i class="bi bi-shield-check me-1"></i>安心出行</span>
        </div>
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-card {
  background: #FFFFFF;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  padding: var(--space-8);
  box-shadow: 0 4px 24px rgba(14, 165, 233, 0.06);
}

.step-card {
  background: var(--color-sky-50);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  height: 100%;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: var(--space-5);
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 0.8rem;
  flex-shrink: 0;
}

.step-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-gray-800);
  margin: 0;
}

.form-label-sm {
  display: block;
  font-size: 0.8rem;
  color: var(--color-gray-500);
  margin-bottom: 6px;
  font-weight: 500;
}

.form-input {
  width: 100%;
  border: 1px solid var(--color-sky-200);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  font-size: 0.9rem;
  color: var(--color-gray-800);
  background: #fff;
  transition: var(--transition-all);
  outline: none;
}
.form-input:focus {
  border-color: var(--color-sky-400);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.12);
}

/* Radio */
.radio-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.radio-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--color-gray-300);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition-all);
}
.radio-circle.active {
  border-color: var(--color-green-600);
  background: var(--color-green-600);
}
.radio-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
}

/* Tags */
.tag-btn {
  border: 1px solid var(--color-sky-200);
  background: #fff;
  border-radius: var(--radius-md);
  padding: 8px 6px;
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--color-gray-600);
  text-align: center;
  transition: var(--transition-all);
  cursor: pointer;
}
.tag-btn:hover {
  background: var(--color-sky-100);
}
.tag-active {
  background: rgba(14, 165, 233, 0.1);
  border-color: var(--color-sky-400);
  color: var(--color-sky-600);
}

/* Submit */
.submit-btn {
  background: linear-gradient(135deg, var(--color-sky-500), #9765F5);
  color: #fff;
  border: none;
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  font-size: 1.05rem;
  font-weight: var(--font-weight-semibold);
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.25);
  cursor: pointer;
  transition: var(--transition-all);
}
.submit-btn:hover:not(:disabled) {
  box-shadow: 0 8px 30px rgba(14, 165, 233, 0.35);
  transform: translateY(-1px);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-card {
    padding: var(--space-5);
  }
}
</style>

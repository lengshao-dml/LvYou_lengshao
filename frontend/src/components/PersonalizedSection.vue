<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { useRecommendationStore } from '@/store/recommendation';
import { getPersonalizedData } from '@/api';
import WordCloudView from '@/components/WordCloudView.vue';

const router = useRouter();
const authStore = useAuthStore();
const recommendationStore = useRecommendationStore();

const personalized = ref(null);
const loading = ref(false);
const error = ref(null);

const fetchPersonalized = async () => {
  loading.value = true;
  error.value = null;
  try {
    const res = await getPersonalizedData();
    personalized.value = res.data;
  } catch (err) {
    console.error('获取个性化推荐失败', err);
    error.value = '获取推荐数据失败';
  } finally {
    loading.value = false;
  }
};

const handleCityClick = (city) => {
  authStore.logClick(city.cityId);
  router.push({ name: 'city-detail', params: { name: city.name } });
};

const handleTagClick = (item) => {
  console.log('点击标签:', item.name);
};

const handleCityCloudClick = (item) => {
  router.push({ name: 'city-detail', params: { name: item.name } });
};

// 生成城市背景图 URL（与 CityDetailView 相同的 hash 算法）
const cityBgImage = (city) => {
  const name = city.name || 'city';
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = ((hash << 5) - hash) + name.charCodeAt(i);
    hash |= 0;
  }
  const seed = Math.abs(hash) % 200 + 10;
  return `https://picsum.photos/id/${seed}/800/600`;
};

onMounted(() => {
  fetchPersonalized();
});

watch(() => authStore.token, () => {
  if (authStore.token) {
    fetchPersonalized();
  }
});

watch(() => recommendationStore.personalizedRefreshKey, () => {
  fetchPersonalized();
});
</script>

<template>
  <div class="personalized-section">
    <!-- Loading -->
    <div v-if="loading" class="d-flex justify-content-center py-5">
      <div class="spinner-border" style="color: var(--color-sky-500);" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
    </div>

    <!-- Loaded -->
    <div v-else-if="personalized" class="row g-4">
      <!-- Left: Recommended Cities -->
      <div class="col-lg-8">
        <div class="d-flex align-items-center gap-2 mb-4">
          <div class="section-icon" style="background: rgba(234, 88, 12, 0.12); color: var(--color-accent);">
            <i class="bi bi-heart-fill"></i>
          </div>
          <h4 class="fw-bold mb-0" style="color: var(--color-gray-800);">
            {{ authStore.isLoggedIn ? '猜你喜欢' : '热门推荐' }}
          </h4>
          <span v-if="authStore.isLoggedIn" class="badge rounded-pill px-3 py-2"
                style="background: rgba(14, 165, 233, 0.1); color: var(--color-sky-600); font-size: 0.75rem;">
            <i class="bi bi-magic me-1"></i>基于您的画像
          </span>
        </div>

        <p v-if="!authStore.isLoggedIn" class="mb-4" style="color: var(--color-gray-500); font-size: var(--font-size-sm);">
          <i class="bi bi-info-circle me-1"></i>
          <router-link to="/login" class="fw-medium" style="color: var(--color-primary);">登录</router-link> 后可获得基于您的兴趣的个性化推荐
        </p>

        <div v-if="personalized.recommendedCities && personalized.recommendedCities.length > 0" class="row g-3">
          <div
            v-for="city in personalized.recommendedCities"
            :key="city.cityId"
            class="col-md-6"
          >
            <div
              class="person-card card-hover"
              role="button"
              :aria-label="'查看 ' + city.name + ' 详情'"
              :style="{ backgroundImage: 'url(' + cityBgImage(city) + ')' }"
              @click="handleCityClick(city)"
            >
              <div class="person-card-overlay"></div>
              <div class="d-flex justify-content-between align-items-center p-3 position-relative">
                <div class="overflow-hidden">
                  <h6 class="fw-bold mb-1" style="color: #fff; text-shadow: 0 1px 3px rgba(0,0,0,0.5);">{{ city.name }}</h6>
                  <small style="color: rgba(255,255,255,0.8); text-shadow: 0 1px 2px rgba(0,0,0,0.4);">{{ city.province }}</small>
                  <div v-if="city.matchedTags && city.matchedTags.length > 0" class="mt-2">
                    <span
                      v-for="tag in city.matchedTags"
                      :key="tag"
                      class="badge me-1 px-2 py-1"
                      style="background: rgba(255,255,255,0.2); backdrop-filter: blur(4px); color: #fff; font-size: 0.7rem; font-weight: 500;"
                    >{{ tag }}</span>
                  </div>
                </div>
                <div class="text-end ms-3">
                  <div class="score-badge"
                    :class="{
                      'score-high': city.score >= 80,
                      'score-mid': city.score >= 60 && city.score < 80,
                      'score-low': city.score < 60,
                    }">
                    {{ city.score ? city.score.toFixed(1) : 'N/A' }}
                  </div>
                  <div v-if="city.distanceKm !== undefined" style="color: var(--color-gray-500); font-size: var(--font-size-xs); margin-top: 4px;">
                    {{ city.distanceKm > 1000 ? (city.distanceKm / 1000).toFixed(1) + 'k' : city.distanceKm.toFixed(0) }} km
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="text-center py-5" style="color: var(--color-gray-400);">
          <i class="bi bi-emoji-neutral fs-1 d-block mb-2"></i>
          <p class="mb-0">暂无推荐数据，<router-link to="/register" style="color: var(--color-primary);">注册</router-link> 后设置兴趣偏好即可获取个性化推荐</p>
        </div>
      </div>

      <!-- Right: Word Clouds -->
      <div class="col-lg-4 d-flex flex-column gap-4">
        <WordCloudView
          v-if="personalized.hotTags && personalized.hotTags.length > 0"
          :items="personalized.hotTags"
          title="热门标签"
          icon="bi bi-tag-fill"
          :clickable="true"
          empty-text="暂无热门标签"
          @item-click="handleTagClick"
        />

        <WordCloudView
          v-if="personalized.hotCities && personalized.hotCities.length > 0"
          :items="personalized.hotCities"
          title="热门城市"
          icon="bi bi-geo-alt-fill"
          :colorScheme="['#0EA5E9', '#38BDF8', '#7DD3FC', '#0284C7', '#0369A1', '#EA580C', '#F97316', '#C2410C']"
          :clickable="true"
          empty-text="暂无热门城市"
          @item-click="handleCityCloudClick"
        />
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="alert d-flex align-items-center gap-3 py-3" role="alert"
         style="background: rgba(220, 38, 38, 0.08); border: 1px solid rgba(220, 38, 38, 0.2); border-radius: var(--radius-md);">
      <i class="bi bi-exclamation-triangle-fill" style="color: var(--color-error);"></i>
      <div>
        <strong>加载失败</strong> — {{ error }}
        <button type="button" class="btn btn-sm ms-2" style="background: rgba(220, 38, 38, 0.1); color: var(--color-error);" @click="fetchPersonalized">
          <i class="bi bi-arrow-clockwise"></i> 重试
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.section-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
}

.person-card {
  position: relative;
  background-size: cover;
  background-position: center;
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  transition: var(--transition-all);
  overflow: hidden;
  min-height: 90px;
}
.person-card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0,0,0,0.55) 0%, rgba(0,0,0,0.3) 100%);
  z-index: 0;
  transition: var(--transition-all);
}
.person-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-sky-300);
}
.person-card:hover .person-card-overlay {
  background: linear-gradient(135deg, rgba(0,0,0,0.45) 0%, rgba(0,0,0,0.2) 100%);
}

.score-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-lg);
  color: #fff;
  background: var(--color-sky-500);
}
.score-badge.score-high {
  background: linear-gradient(135deg, #00C48C, #16A34A);
}
.score-badge.score-mid {
  background: linear-gradient(135deg, var(--color-sky-500), var(--color-sky-600));
}
.score-badge.score-low {
  background: linear-gradient(135deg, var(--color-orange-500), var(--color-orange-600));
}

@media (prefers-reduced-motion: reduce) {
  .person-card, .person-card:hover {
    transition: none;
    transform: none;
  }
}
</style>

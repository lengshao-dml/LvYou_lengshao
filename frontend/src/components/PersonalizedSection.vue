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

// 获取个性化推荐数据
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

// 点击城市跳转到详情
const handleCityClick = (city) => {
  authStore.logClick(city.cityId);
  router.push({ name: 'city-detail', params: { name: city.name } });
};

// 标签词云点击
const handleTagClick = (item) => {
  // 点击标签后可以触发搜索或筛选，这里简单展示提示
  console.log('点击标签:', item.name);
};

// 城市词云点击
const handleCityCloudClick = (item) => {
  router.push({ name: 'city-detail', params: { name: item.name } });
};

onMounted(() => {
  fetchPersonalized();
});

// 登录状态变化时重新获取（显示个性化内容）
watch(() => authStore.token, () => {
  if (authStore.token) {
    fetchPersonalized();
  }
});

// 搜索/推荐/搜索城市后实时刷新
watch(() => recommendationStore.personalizedRefreshKey, () => {
  fetchPersonalized();
});
</script>

<template>
  <div class="personalized-section">
    <!-- 加载中 -->
    <div v-if="loading" class="d-flex justify-content-center py-5">
      <div class="spinner-border text-primary" role="status" style="width: 2.5rem; height: 2.5rem;">
        <span class="visually-hidden">加载中...</span>
      </div>
    </div>

    <!-- 数据已加载 -->
    <div v-else-if="personalized" class="row g-4">
      <!-- 左侧：猜你喜欢 -->
      <div class="col-lg-8">
        <div class="d-flex align-items-center gap-2 mb-3">
          <i class="bi bi-heart-fill fs-5" style="color: #F97316;"></i>
          <h4 class="fw-bold mb-0">
            {{ authStore.isLoggedIn ? '猜你喜欢' : '热门推荐' }}
          </h4>
          <span class="badge rounded-pill bg-orange-subtle text-orange ms-2">
            <i class="bi bi-magic me-1"></i>基于您的画像
          </span>
        </div>

        <!-- 未登录提示 -->
        <p v-if="!authStore.isLoggedIn" class="text-muted small mb-3">
          <i class="bi bi-info-circle me-1"></i>
          <router-link to="/login" class="text-decoration-none">登录</router-link> 后可获得基于您的兴趣的个性化推荐
        </p>

        <!-- 推荐城市卡片 -->
        <div v-if="personalized.recommendedCities && personalized.recommendedCities.length > 0" class="row g-3">
          <div
            v-for="city in personalized.recommendedCities"
            :key="city.cityId"
            class="col-md-6"
          >
            <div
              class="card personalized-card h-100 overflow-hidden shadow-sm"
              role="button"
              @click="handleCityClick(city)"
            >
              <div class="card-body d-flex justify-content-between align-items-center p-3">
                <div>
                  <h6 class="fw-bold mb-1">{{ city.name }}</h6>
                  <small class="text-muted">{{ city.province }}</small>
                  <div v-if="city.matchedTags && city.matchedTags.length > 0" class="mt-2">
                    <span
                      v-for="tag in city.matchedTags"
                      :key="tag"
                      class="badge bg-light text-secondary me-1 fw-normal px-2 py-1"
                      style="font-size: 0.7rem;"
                    >{{ tag }}</span>
                  </div>
                </div>
                <div class="text-end ms-3">
                  <div
                    class="badge rounded-pill fs-6 px-3 py-2"
                    :style="{
                      backgroundColor: city.score >= 80 ? '#00C48C' : city.score >= 60 ? '#4080FF' : '#F97316',
                      color: '#fff',
                    }"
                  >
                    {{ city.score ? city.score.toFixed(1) : 'N/A' }}
                  </div>
                  <div v-if="city.distanceKm !== undefined" class="text-muted small mt-1">
                    {{ city.distanceKm > 1000 ? (city.distanceKm / 1000).toFixed(1) + 'k' : city.distanceKm.toFixed(0) }} km
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="text-center text-muted py-4">
          <i class="bi bi-emoji-neutral fs-1 d-block mb-2"></i>
          <p class="mb-0">暂无推荐数据，<router-link to="/register" class="text-decoration-none">注册</router-link> 后设置兴趣偏好即可获取个性化推荐</p>
        </div>
      </div>

      <!-- 右侧：词云区域 -->
      <div class="col-lg-4 d-flex flex-column gap-4">
        <!-- 热门标签词云 -->
        <WordCloudView
          v-if="personalized.hotTags && personalized.hotTags.length > 0"
          :items="personalized.hotTags"
          title="热门标签"
          icon="bi bi-tag-fill"
          :clickable="true"
          empty-text="暂无热门标签"
          @item-click="handleTagClick"
        />

        <!-- 热门城市词云 -->
        <WordCloudView
          v-if="personalized.hotCities && personalized.hotCities.length > 0"
          :items="personalized.hotCities"
          title="热门城市"
          icon="bi bi-geo-alt-fill"
          :colorScheme="['#00C48C', '#1ABC9C', '#2ECC71', '#27AE60', '#16A085', '#3498DB', '#2980B9', '#9B59B6']"
          :clickable="true"
          empty-text="暂无热门城市"
          @item-click="handleCityCloudClick"
        />
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="alert alert-warning d-flex align-items-center gap-2 py-3" role="alert">
      <i class="bi bi-exclamation-triangle-fill"></i>
      <div>
        <strong>加载失败</strong> — {{ error }}
        <button type="button" class="btn btn-sm btn-outline-secondary ms-2" @click="fetchPersonalized">
          <i class="bi bi-arrow-clockwise"></i> 重试
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.personalized-section {
  /* wrapper */
}

.personalized-card {
  border: 1px solid #e9ecef;
  border-radius: 0.75rem;
  transition: all 0.2s ease;
  cursor: pointer;
}

.personalized-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 0.5rem 1.5rem rgba(0, 0, 0, 0.1) !important;
  border-color: #4080FF;
}

.bg-orange-subtle {
  background-color: rgba(249, 115, 22, 0.1);
}

.text-orange {
  color: #F97316;
}
</style>

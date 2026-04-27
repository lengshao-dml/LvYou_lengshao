<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { useRecommendationStore } from '@/store/recommendation';
import RecommendationCard from './RecommendationCard.vue';
import CityDetailCard from './CityDetailCard.vue';

const router = useRouter();
const recommendationStore = useRecommendationStore();
const authStore = useAuthStore();

const handleCitySelection = (city) => {
  // 记录点击行为
  authStore.logClick(city.id);
  // 跳转到城市详情页
  router.push({ name: 'city-detail', params: { name: city.name } });
};
</script>

<template>
  <div class="results-container">
    <!-- 加载状态 -->
    <div v-if="recommendationStore.isLoading || recommendationStore.isSearching" class="d-flex justify-content-center my-5">
      <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="recommendationStore.error" class="alert alert-danger" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {{ recommendationStore.error }}
    </div>

    <!-- 内容区域 -->
    <div v-else>
      <transition name="fade" mode="out-in">
        <!-- 单个城市精确搜索结果 -->
        <div v-if="recommendationStore.searchedCities.length === 1" key="searchedCity">
           <h3 class="mb-4">城市信息查询结果</h3>
           <CityDetailCard :city="recommendationStore.searchedCities[0]" />
        </div>

        <!-- 多个城市模糊搜索结果 -->
        <div v-else-if="recommendationStore.searchedCities.length > 1" key="searchedCitiesList">
            <h3 class="mb-4">我们找到了多个相关城市，请选择：</h3>
            <div class="list-group">
                <a 
                  href="#" 
                  v-for="city in recommendationStore.searchedCities" 
                  :key="city.id" 
                  class="list-group-item list-group-item-action"
                  @click.prevent="handleCitySelection(city)"
                >
                    {{ city.name }} ({{ city.province }})
                </a>
            </div>
        </div>

        <!-- 推荐结果列表：第一个放大 + 其余网格 -->
        <div v-else-if="recommendationStore.recommendations.length > 0" class="results" key="recommendations">
          <h3 class="mb-4 fw-bold">为您智能推荐</h3>
          <div class="row g-4">
            <!-- 第一个推荐：大卡片，全宽，顶部 -->
            <div class="col-12">
              <div class="card recommendation-card recommendation-card-featured overflow-hidden" role="button" @click="authStore.logClick(recommendationStore.recommendations[0].cityId); router.push({ name: 'city-detail', params: { name: recommendationStore.recommendations[0].name } })">
                <div class="row g-0">
                  <div class="col-md-6">
                    <div class="overflow-hidden h-100" style="min-height: 220px; max-height: 320px;">
                      <img :src="`https://picsum.photos/seed/${recommendationStore.recommendations[0].cityId}/1200/600`" class="w-100 h-100" style="object-fit: cover;" alt="City image">
                    </div>
                  </div>
                  <div class="col-md-6">
                    <div class="card-body d-flex flex-column h-100 p-4">
                      <div class="d-flex justify-content-between align-items-start mb-3">
                        <div>
                          <h3 class="card-title mb-1 fw-bold fs-2">{{ recommendationStore.recommendations[0].name }}</h3>
                          <h6 class="card-subtitle text-body-secondary fs-6">{{ recommendationStore.recommendations[0].province }}</h6>
                        </div>
                        <span 
                          class="badge rounded-pill fs-5" 
                          style="color: #f97316; background-color: rgba(249, 115, 22, 0.1);"
                        >{{ recommendationStore.recommendations[0].score.toFixed(1) }} 分</span>
                      </div>

                      <div class="d-flex align-items-center text-secondary mb-3">
                        <i class="bi bi-broadcast-pin me-2 fs-5"></i>
                        <span class="fs-6">距离您约 {{ recommendationStore.recommendations[0].distanceKm }} 公里</span>
                      </div>
                      <div class="d-flex align-items-center text-secondary mb-4">
                        <i class="bi bi-cloud-sun-fill me-2 fs-5"></i>
                        <span class="fs-6">天气: {{ recommendationStore.recommendations[0].weatherForecast }}</span>
                      </div>

                      <div class="mt-auto pt-3 border-top border-light">
                        <p class="mb-2 small text-muted">匹配的兴趣点:</p>
                        <div>
                          <span v-for="tag in recommendationStore.recommendations[0].matchedTags" :key="tag" class="badge fw-normal text-bg-light me-1 fs-6 px-3 py-2">
                            {{ tag }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 其余推荐：标准卡片网格 -->
            <div v-for="rec in recommendationStore.recommendations.slice(1)" :key="rec.cityId" class="col-md-6 col-lg-4">
              <RecommendationCard :recommendation="rec" />
            </div>
          </div>
        </div>
        
        <!-- 初始或空状态 -->
        <div v-else class="text-center text-muted py-5" key="initial">
          <i class="bi bi-compass fs-1 mb-3"></i>
          <h4>准备好探索世界了吗？</h4>
          <p>您可以在上方搜索框精确查找城市，或在下方表单中获取智能推荐。</p>
        </div>
      </transition>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.recommendation-card {
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
  border: 1px solid #e9ecef;
  cursor: pointer;
}
.recommendation-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 0.5rem 1.5rem rgba(0, 0, 0, 0.1) !important;
}
.recommendation-card-featured:hover {
  transform: translateY(-3px);
  box-shadow: 0 0.75rem 2rem rgba(0, 0, 0, 0.12) !important;
}
</style>

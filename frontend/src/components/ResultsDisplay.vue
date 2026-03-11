<script setup>
import { useAuthStore } from '@/store/auth';
import { useRecommendationStore } from '@/store/recommendation';
import RecommendationCard from './RecommendationCard.vue';
import CityDetailCard from './CityDetailCard.vue';

const recommendationStore = useRecommendationStore();
const authStore = useAuthStore();

const handleCitySelection = (city) => {
  // 1. 记录点击行为
  authStore.logClick(city.id);
  // 2. 用完整的城市名发起一次新的精确查找
  recommendationStore.fetchCityByName(city.name);
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

        <!-- 推荐结果列表 -->
        <div v-else-if="recommendationStore.recommendations.length > 0" class="results" key="recommendations">
          <h3 class="mb-4">为您智能推荐</h3>
          <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
            <div v-for="rec in recommendationStore.recommendations" :key="rec.cityId" class="col">
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
</style>

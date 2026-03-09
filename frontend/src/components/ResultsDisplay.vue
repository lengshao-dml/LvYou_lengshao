<script setup>
import { useRecommendationStore } from '@/store/recommendation';
import RecommendationCard from './RecommendationCard.vue';

const store = useRecommendationStore();
</script>

<template>
  <div class="results-container mt-5">
    <!-- 加载状态 -->
    <div v-if="store.isLoading" class="d-flex justify-content-center my-5">
      <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="store.error" class="alert alert-danger" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {{ store.error }}
    </div>

    <!-- 内容区域 -->
    <div v-else>
      <transition name="fade" mode="out-in">
        <!-- 结果展示 -->
        <div v-if="store.recommendations.length > 0" class="results">
          <h3 class="mb-4">为您推荐以下目的地</h3>
          <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
            <div v-for="rec in store.recommendations" :key="rec.cityId" class="col">
              <RecommendationCard :recommendation="rec" />
            </div>
          </div>
        </div>
        
        <!-- 初始或空状态 -->
        <div v-else class="text-center text-muted py-5">
          <i class="bi bi-compass fs-1 mb-3"></i>
          <h4>准备好探索世界了吗？</h4>
          <p>请在左侧填写您的偏好，我们将为您发现最合适的旅行地。</p>
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

<script setup>
import { useAuthStore } from '@/store/auth';

defineProps({
  recommendation: {
    type: Object,
    required: true
  }
});

const authStore = useAuthStore();
const handleClick = (cityId) => {
  authStore.logClick(cityId);
  // 未来可以加入跳转到城市详情页的逻辑
  // e.g., router.push(`/city/${cityId}`);
};
</script>

<template>
  <div class="card h-100 shadow-sm recommendation-card overflow-hidden" @click="handleClick(recommendation.cityId)">
    <div class="overflow-hidden" style="height: 180px;">
      <img :src="`https://picsum.photos/seed/${recommendation.cityId}/800/600`" class="card-img-top" alt="City image">
    </div>
    <div class="card-body d-flex flex-column">
      <div class="d-flex justify-content-between align-items-start mb-2">
        <h5 class="card-title mb-0 fw-bold">{{ recommendation.name }}</h5>
        <span 
          class="badge rounded-pill fs-6" 
          style="color: #f97316; background-color: rgba(249, 115, 22, 0.1);"
        >{{ recommendation.score.toFixed(1) }} 分</span>
      </div>
      <h6 class="card-subtitle mb-3 text-body-secondary">{{ recommendation.province }}</h6>

      <div class="d-flex align-items-center text-secondary mb-2 small">
        <i class="bi bi-broadcast-pin me-2"></i>
        <span>距离您约 {{ recommendation.distanceKm }} 公里</span>
      </div>
      <div class="d-flex align-items-center text-secondary mb-3 small">
        <i class="bi bi-cloud-sun-fill me-2"></i>
        <span>天气: {{ recommendation.weatherForecast }}</span>
      </div>

      <div class="mt-auto pt-3 border-top border-light">
        <p class="mb-2 small text-muted">匹配的兴趣点:</p>
        <div>
          <span v-for="tag in recommendation.matchedTags" :key="tag" class="badge fw-normal text-bg-light me-1">
            {{ tag }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recommendation-card {
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
  border: 1px solid #e9ecef;
}
.recommendation-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 0.5rem 1.5rem rgba(0, 0, 0, 0.1) !important;
}
.card-title {
  color: #343a40;
}
.card-img-top {
  transition: transform 0.5s ease;
}
.recommendation-card:hover .card-img-top {
  transform: scale(1.1);
}
</style>

<script setup>
defineProps({
  recommendation: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="card h-100 shadow-sm recommendation-card">
    <div class="card-body d-flex flex-column">
      <div class="d-flex justify-content-between align-items-start mb-2">
        <h5 class="card-title mb-0 fw-bold">{{ recommendation.name }}</h5>
        <span class="badge bg-success-subtle text-success-emphasis rounded-pill fs-6">{{ recommendation.score.toFixed(1) }} 分</span>
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
</style>

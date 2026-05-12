<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { useRecommendationStore } from '@/store/recommendation';
import HeroSection from '@/components/HeroSection.vue';
import RecommendationForm from '@/components/RecommendationForm.vue';
import ResultsDisplay from '@/components/ResultsDisplay.vue';
import PersonalizedSection from '@/components/PersonalizedSection.vue';

const router = useRouter();
const authStore = useAuthStore();
const recommendationStore = useRecommendationStore();

onMounted(() => {
  recommendationStore.fetchPopularCities();
});

const handlePopularCityClick = (city) => {
  authStore.logClick(city.cityId);
  router.push({ name: 'city-detail', params: { name: city.name } });
};
</script>

<template>
  <div>
    <HeroSection />

    <!-- ===== 猜你喜欢 + 词云板块 (新增) ===== -->
    <div class="container my-5">
      <PersonalizedSection />
    </div>
    
    <div id="recommend-section" class="container my-5">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <RecommendationForm />
        </div>
      </div>
    </div>

    <!-- 热门推荐 Section (保留，挪到下方) -->
    <div v-if="recommendationStore.popularCities.length > 0" class="container my-5">
      <h2 class="text-center mb-4 fw-bold">热门推荐</h2>
      <div class="row g-4">
        <div v-for="city in recommendationStore.popularCities.slice(0, 3)" :key="city.cityId" class="col-md-6 col-lg-4">
          <div class="card h-100 shadow-sm recommendation-card overflow-hidden" role="button" @click="handlePopularCityClick(city)">
            <div class="overflow-hidden" style="height: 180px;">
              <img :src="`https://picsum.photos/seed/pop-${city.cityId}/800/600`" class="card-img-top" alt="City image">
            </div>
            <div class="card-body d-flex flex-column">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <h5 class="card-title mb-0 fw-bold">{{ city.name }}</h5>
                <span class="badge badge-score rounded-pill fs-6">{{ city.score.toFixed(1) }} 分</span>
              </div>
              <h6 class="card-subtitle mb-2 text-body-secondary">{{ city.province }}</h6>
              <p class="text-muted small mb-0 mt-auto">热门城市 · 点击查看详情</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div id="results-section" class="container mb-5">
       <ResultsDisplay />
    </div>

  </div>
</template>

<style scoped>
.recommendation-card {
  transition: all var(--duration-base) var(--easing-smooth);
  border: 1px solid var(--color-border);
  cursor: pointer;
  border-radius: var(--radius-xl);
  overflow: hidden;
}
.recommendation-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg) !important;
  border-color: var(--color-sky-300);
}
.card-img-top {
  transition: transform 0.5s var(--easing-smooth);
}
.recommendation-card:hover .card-img-top {
  transform: scale(1.08);
}
@media (prefers-reduced-motion: reduce) {
  .recommendation-card,
  .recommendation-card:hover {
    transition: none;
    transform: none;
  }
  .recommendation-card:hover .card-img-top {
    transform: none;
  }
}
</style>

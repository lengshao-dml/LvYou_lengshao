<script setup>
import { onMounted } from 'vue';
import { useRecommendationStore } from '@/store/recommendation';
import HeroSection from '@/components/HeroSection.vue';
import RecommendationForm from '@/components/RecommendationForm.vue';
import ResultsDisplay from '@/components/ResultsDisplay.vue';

const recommendationStore = useRecommendationStore();

onMounted(() => {
  recommendationStore.fetchPopularCities();
});
</script>

<template>
  <div>
    <HeroSection />
    
    <div id="recommend-section" class="container my-5">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <RecommendationForm />
        </div>
      </div>
    </div>

    <!-- 热门推荐 Section -->
    <div v-if="recommendationStore.popularCities.length > 0" class="container my-5">
      <h2 class="text-center mb-4">热门推荐</h2>
      <div class="row g-4">
        <div v-for="city in recommendationStore.popularCities" :key="city.cityId" class="col-md-6 col-lg-4 col-xl-3">
          <div class="card h-100 shadow-sm">
            <div class="card-body text-center">
              <h5 class="card-title fw-bold">{{ city.name }}</h5>
              <h6 class="card-subtitle mb-2 text-body-secondary">{{ city.province }}</h6>
              <span class="badge rounded-pill fs-6 mt-2" style="color: #f97316; background-color: rgba(249, 115, 22, 0.1);">
                热度: {{ city.score.toFixed(1) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="container mb-5">
       <ResultsDisplay />
    </div>

  </div>
</template>

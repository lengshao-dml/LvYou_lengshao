<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useRecommendationStore } from '@/store/recommendation';

const router = useRouter();
const recommendationStore = useRecommendationStore();
const searchQuery = ref('');

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ name: 'city-detail', params: { name: searchQuery.value.trim() } });
  }
};

const handleTagClick = (cityName) => {
  router.push({ name: 'city-detail', params: { name: cityName } });
};

const hotCities = computed(() => {
  return recommendationStore.popularCities.slice(0, 3);
});
</script>

<template>
  <section class="hero">
    <div class="hero-bg"></div>
    <div class="hero-content container">
      <div class="col-lg-7 col-md-10">
        <span class="hero-badge">
          <i class="bi bi-sparkles"></i> AI 智能推荐
        </span>
        <h1 class="hero-title">发现你的理想<br>旅行目的地</h1>
        <p class="hero-subtitle">基于你的偏好，结合天气、距离、热度，为你精准推荐最适合的旅游城市</p>

        <form @submit.prevent="handleSearch" class="hero-search">
          <div class="search-box">
            <i class="bi bi-search search-icon"></i>
            <input
              type="text"
              class="search-input"
              placeholder="输入城市名称，例如 成都、黄山 ..."
              v-model="searchQuery"
            >
            <button class="search-btn" type="submit">
              <i class="bi bi-search"></i>
              <span class="d-none d-md-inline ms-2">搜索</span>
            </button>
          </div>
        </form>

        <div v-if="hotCities.length > 0" class="hero-tags">
          <span
            v-for="city in hotCities"
            :key="city.cityId"
            class="hero-tag"
            @click="handleTagClick(city.name)"
          >
            <i class="bi bi-geo-alt"></i> 热门：{{ city.name }}
          </span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  min-height: 75vh;
  display: flex;
  align-items: center;
  overflow: hidden;
}

/* Aurora gradient background */
.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 30% 20%, rgba(14, 165, 233, 0.15), transparent),
    radial-gradient(ellipse 60% 50% at 70% 60%, rgba(234, 88, 12, 0.1), transparent),
    radial-gradient(ellipse 50% 40% at 50% 80%, rgba(56, 189, 248, 0.12), transparent),
    linear-gradient(180deg, #F0F9FF 0%, #E0F2FE 50%, #F0F9FF 100%);
  z-index: 0;
}

/* Floating orbs */
.hero-bg::before {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.12), transparent);
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}
.hero-bg::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(234, 88, 12, 0.08), transparent);
  bottom: -50px;
  left: -50px;
  animation: float 10s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}

.hero-content {
  position: relative;
  z-index: 1;
  padding-top: var(--space-16);
  padding-bottom: var(--space-16);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: var(--radius-full);
  color: var(--color-sky-700);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  margin-bottom: var(--space-6);
}

.hero-title {
  font-size: clamp(2.5rem, 6vw, 4rem);
  font-weight: 800;
  line-height: 1.1;
  background: linear-gradient(135deg, var(--color-sky-800) 0%, var(--color-sky-500) 60%, var(--color-orange-500) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: var(--space-5);
}

.hero-subtitle {
  font-size: var(--font-size-lg);
  color: var(--color-gray-600);
  line-height: 1.6;
  max-width: 520px;
  margin-bottom: var(--space-8);
}

/* Search box */
.hero-search {
  margin-bottom: var(--space-6);
}

.search-box {
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border: 2px solid var(--color-sky-200);
  border-radius: var(--radius-2xl);
  padding: 6px 6px 6px 20px;
  max-width: 600px;
  box-shadow: 0 4px 24px rgba(14, 165, 233, 0.1);
  transition: var(--transition-all);
}
.search-box:focus-within {
  border-color: var(--color-sky-400);
  box-shadow: 0 8px 32px rgba(14, 165, 233, 0.2);
}

.search-icon {
  color: var(--color-gray-400);
  font-size: var(--font-size-lg);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 12px 16px;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
  background: transparent;
}
.search-input::placeholder {
  color: var(--color-gray-400);
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 28px;
  background: var(--btn-cta-bg);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-base);
  white-space: nowrap;
  cursor: pointer;
  transition: var(--transition-all);
  box-shadow: var(--shadow-glow-orange);
}
.search-btn:hover {
  box-shadow: 0 6px 28px rgba(234, 88, 12, 0.4);
  transform: translateY(-1px);
}

/* Tags */
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--color-sky-200);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  color: var(--color-sky-700);
  cursor: pointer;
  transition: var(--transition-all);
}
.hero-tag:hover {
  background: #FFFFFF;
  border-color: var(--color-sky-400);
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .hero {
    min-height: 60vh;
  }
  .search-box {
    flex-direction: column;
    padding: 12px;
    gap: 8px;
  }
  .search-btn {
    width: 100%;
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-bg::before,
  .hero-bg::after {
    animation: none;
  }
}
</style>

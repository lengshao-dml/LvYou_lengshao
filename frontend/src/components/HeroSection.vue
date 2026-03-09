<script setup>
import { ref } from 'vue';
import { useRecommendationStore } from '@/store/recommendation';

const store = useRecommendationStore();
const searchQuery = ref('');

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    store.fetchCityByName(searchQuery.value.trim());
  }
};
</script>

<template>
  <section class="position-relative vh-75 d-flex align-items-center text-white" style="background-image: url('https://picsum.photos/id/1036/1920/1080'); background-size: cover; background-position: center;">
    <div class="position-absolute top-0 start-0 w-100 h-100" style="background-color: rgba(0, 0, 0, 0.5);"></div>
    <div class="position-relative z-1 container">
      <div class="col-lg-7 col-md-10">
        <h1 class="display-3 fw-bold">发现你的理想旅行目的地</h1>
        <p class="lead fs-4 my-4">基于你的偏好，为你精准推荐最适合的旅游城市</p>
        
        <form @submit.prevent="handleSearch" class="d-flex">
          <div class="input-group input-group-lg" style="max-width: 600px;">
            <input 
              type="text" 
              class="form-control" 
              placeholder="输入你想查找的城市，例如“成都”..."
              v-model="searchQuery"
            >
            <button 
              class="btn btn-primary" 
              type="submit" 
              :disabled="store.isSearching"
              style="--bs-btn-color: #fff; --bs-btn-bg: #f97316; --bs-btn-border-color: #f97316; --bs-btn-hover-color: #fff; --bs-btn-hover-bg: #ea580c; --bs-btn-hover-border-color: #dd520b;"
            >
              <span v-if="store.isSearching" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
              <i v-else class="bi bi-search"></i>
              <span class="d-none d-md-inline ms-2">{{ store.isSearching ? '搜索中...' : '搜索' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.vh-75 {
  height: 75vh;
  min-height: 500px;
}
</style>

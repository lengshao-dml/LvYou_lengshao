<script setup>
import { ref, onMounted } from 'vue';
import { useRecommendationStore } from '@/store/recommendation';

const store = useRecommendationStore();

// 表单的本地状态
const departureCity = ref('合肥市'); // 默认值
const selectedTags = ref([]);
const travelDate = ref(new Date().toISOString().split('T')[0]); // 默认今天
const distanceScope = ref('ANY'); // 默认"不限"

// 组件加载时，自动获取标签列表
onMounted(() => {
  store.fetchTags();
});

// 处理标签点击
const toggleTag = (tagName) => {
  const index = selectedTags.value.indexOf(tagName);
  if (index > -1) {
    selectedTags.value.splice(index, 1);
  } else {
    selectedTags.value.push(tagName);
  }
};

// 提交表单
const handleSubmit = () => {
  const request = {
    departureCity: departureCity.value,
    interestTags: selectedTags.value,
    travelDate: travelDate.value,
    distanceScope: distanceScope.value,
  };
  store.fetchRecommendations(request);
};
</script>

<template>
  <div class="card p-4 shadow-sm">
    <h2 class="mb-4">开始您的下一次旅行</h2>
    <form @submit.prevent="handleSubmit">
      <!-- 出发地 -->
      <div class="mb-3">
        <label for="departureCity" class="form-label"><i class="bi bi-geo-alt-fill"></i> 出发城市</label>
        <input type="text" class="form-control" id="departureCity" v-model="departureCity" required>
      </div>

      <!-- 出行日期 -->
      <div class="mb-3">
        <label for="travelDate" class="form-label"><i class="bi bi-calendar-event-fill"></i> 出行日期</label>
        <input type="date" class="form-control" id="travelDate" v-model="travelDate" required>
      </div>

      <!-- 距离范围 -->
      <div class="mb-4">
        <label class="form-label"><i class="bi bi-broadcast-pin"></i> 距离范围</label>
        <div class="d-flex">
          <div class="form-check me-3">
            <input class="form-check-input" type="radio" id="scopeAny" value="ANY" v-model="distanceScope">
            <label class="form-check-label" for="scopeAny">不限</label>
          </div>
          <div class="form-check me-3">
            <input class="form-check-input" type="radio" id="scopeProvince" value="PROVINCE" v-model="distanceScope">
            <label class="form-check-label" for="scopeProvince">省内</label>
          </div>
          <div class="form-check">
            <input class="form-check-input" type="radio" id="scopeNearby" value="NEARBY_500KM" v-model="distanceScope">
            <label class="form-check-label" for="scopeNearby">周边(500km)</label>
          </div>
        </div>
      </div>

      <!-- 兴趣标签 -->
      <div class="mb-4">
        <label class="form-label"><i class="bi bi-tags-fill"></i> 兴趣标签 (可多选)</label>
        <div>
          <button
            v-for="tag in store.tags"
            :key="tag.id"
            type="button"
            class="btn me-2 mb-2"
            :class="selectedTags.includes(tag.name) ? 'btn-primary' : 'btn-outline-secondary'"
            @click="toggleTag(tag.name)"
          >
            {{ tag.name }}
          </button>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="d-grid">
        <button type="submit" class="btn btn-primary btn-lg" :disabled="store.isLoading">
          <span v-if="store.isLoading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
          {{ store.isLoading ? ' 计算中...' : '获取推荐' }}
        </button>
      </div>
    </form>
  </div>
</template>

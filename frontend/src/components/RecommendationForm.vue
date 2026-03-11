<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRecommendationStore } from '@/store/recommendation';
import { useMetaStore } from '@/store/meta';

const recommendationStore = useRecommendationStore();
const metaStore = useMetaStore();

// 表单的本地状态
const departureCity = ref('合肥市'); // 默认值
const selectedTags = ref([]);
const travelDate = ref(new Date().toISOString().split('T')[0]); // 默认今天
const distanceScope = ref('ANY'); // 默认"不限"

// 组件加载时，自动获取标签和城市列表
onMounted(() => {
  metaStore.fetchMeta();
});

// 将标签分为两列的计算属性
const tagColumns = computed(() => {
  const midpoint = Math.ceil(metaStore.tags.length / 2);
  return [
    metaStore.tags.slice(0, midpoint),
    metaStore.tags.slice(midpoint)
  ];
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
  recommendationStore.fetchRecommendations(request);
};
</script>

<template>
  <div>
    <div class="text-center mb-4">
        <h2 class="fw-bold d-inline">智能推荐</h2>
        <p class="text-muted d-inline ms-2">根据您的偏好，为您量身定制旅行方案</p>
    </div>
    <form @submit.prevent="handleSubmit">
      <div class="row g-4">
        <!-- Part 1: Departure and Date -->
        <div class="col-lg-4 col-md-12">
          <div class="filter-card p-3 h-100">
            <div class="mb-3">
              <label for="departureCity" class="form-label fw-medium"><i class="bi bi-geo-alt-fill me-2"></i>出发城市</label>
              <input type="text" class="form-control" id="departureCity" v-model="departureCity" required>
            </div>
            <div>
              <label for="travelDate" class="form-label fw-medium"><i class="bi bi-calendar-event-fill me-2"></i>出行日期</label>
              <input type="date" class="form-control" id="travelDate" v-model="travelDate" required>
            </div>
          </div>
        </div>

        <!-- Part 2: Distance Scope -->
        <div class="col-lg-4 col-md-6">
          <div class="filter-card p-3 h-100 d-flex flex-column">
            <label class="form-label fw-medium d-block mb-3"><i class="bi bi-broadcast-pin me-2"></i>距离范围</label>
            <div class="d-flex flex-column justify-content-center flex-grow-1">
              <div class="form-check mb-2">
                <input class="form-check-input" type="radio" id="scopeAny" value="ANY" v-model="distanceScope">
                <label class="form-check-label" for="scopeAny">不限</label>
              </div>
              <div class="form-check mb-2">
                <input class="form-check-input" type="radio" id="scopeProvince" value="PROVINCE" v-model="distanceScope">
                <label class="form-check-label" for="scopeProvince">省内</label>
              </div>
              <div class="form-check">
                <input class="form-check-input" type="radio" id="scopeNearby" value="NEARBY_500KM" v-model="distanceScope">
                <label class="form-check-label" for="scopeNearby">周边(500km)</label>
              </div>
            </div>
          </div>
        </div>

        <!-- Part 3: Interest Tags -->
        <div class="col-lg-4 col-md-6">
          <div class="filter-card p-3 h-100">
            <label class="form-label fw-medium d-block mb-3"><i class="bi bi-tags-fill me-2"></i>兴趣标签</label>
            <div class="row">
              <div class="col-6">
                <button
                  v-for="tag in tagColumns[0]"
                  :key="tag.id"
                  type="button"
                  class="btn btn-sm w-100 mb-2 text-start"
                  :class="selectedTags.includes(tag.name) ? 'btn-primary' : 'btn-outline-secondary'"
                  @click="toggleTag(tag.name)"
                >
                  {{ tag.name }}
                </button>
              </div>
              <div class="col-6">
                <button
                  v-for="tag in tagColumns[1]"
                  :key="tag.id"
                  type="button"
                  class="btn btn-sm w-100 mb-2 text-start"
                  :class="selectedTags.includes(tag.name) ? 'btn-primary' : 'btn-outline-secondary'"
                  @click="toggleTag(tag.name)"
                >
                  {{ tag.name }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="d-grid mt-4">
        <button type="submit" class="btn btn-primary btn-lg" :disabled="recommendationStore.isLoading">
          <span v-if="recommendationStore.isLoading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
          {{ recommendationStore.isLoading ? ' 计算中...' : '获取智能推荐' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.filter-card {
  background-color: #f8fafc;
  border-radius: 0.75rem;
  border: 1px solid #e9ecef;
}
</style>

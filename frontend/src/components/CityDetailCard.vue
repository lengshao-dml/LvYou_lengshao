<script setup>
defineProps({
  city: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="card shadow-lg">
    <div class="card-header bg-primary text-white">
      <h2 class="mb-0">{{ city.name }} - {{ city.province }}</h2>
    </div>
    <div class="card-body">
      <p v-if="city.pinyin" class="text-muted">拼音: {{ city.pinyin }}</p>
      
      <div v-for="feature in city.features" :key="feature.id" class="mb-4">
        <h5 class="fw-bold border-bottom pb-2 mb-3">{{ feature.tagName }}</h5>
        <div v-if="feature.attractions && feature.attractions.length > 0">
          <div v-for="attraction in feature.attractions" :key="attraction.id" class="mb-2">
            <strong>{{ attraction.name }}</strong>
            <p class="text-secondary small mb-0">{{ attraction.description }}</p>
          </div>
        </div>
        <p v-else class="text-muted small">暂无具体景点信息。</p>
      </div>
    </div>
  </div>
</template>

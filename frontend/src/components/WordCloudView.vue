<script setup>
/**
 * WordCloudView.vue — 词云图组件
 * 支持展示热门标签词云和热门城市词云
 * 使用纯 CSS + flex 布局实现词云效果，无额外依赖
 */
import { computed } from 'vue';

const props = defineProps({
  items: {
    type: Array,
    required: true,
    // [{ name: string, weight: number, count?: number }]
  },
  title: {
    type: String,
    default: '热门词云',
  },
  icon: {
    type: String,
    default: 'bi bi-cloud',
  },
  colorScheme: {
    type: Array,
    default: () => ['#4080FF', '#9765F5', '#00C48C', '#F97316', '#E74C3C', '#F39C12', '#1ABC9C', '#9B59B6'],
  },
  clickable: {
    type: Boolean,
    default: false,
  },
  emptyText: {
    type: String,
    default: '暂无数据',
  },
});

const emit = defineEmits(['itemClick']);

// 计算字号映射：weight 范围 0.2-1.0 → 字号 0.75rem-2rem
const fontSizeMap = computed(() => {
  if (!props.items || props.items.length === 0) return {};
  const weights = props.items.map(i => i.weight);
  const minW = Math.min(...weights);
  const maxW = Math.max(...weights);
  const range = maxW - minW || 1;

  const map = {};
  props.items.forEach(item => {
    const ratio = (item.weight - minW) / range;
    const fontSize = 0.75 + ratio * 1.25; // 0.75rem ~ 2.0rem
    map[item.name] = `${fontSize.toFixed(2)}rem`;
  });
  return map;
});

// 颜色分配：按权重高低分配不同颜色
const colorMap = computed(() => {
  if (!props.items || props.items.length === 0) return {};
  const map = {};
  props.items.forEach((item, idx) => {
    map[item.name] = props.colorScheme[idx % props.colorScheme.length];
  });
  return map;
});

const handleClick = (item) => {
  if (props.clickable) {
    emit('itemClick', item);
  }
};
</script>

<template>
  <div class="wordcloud-wrapper">
    <div class="d-flex align-items-center gap-2 mb-3">
      <i :class="icon" class="fs-5" style="color: #4080FF;"></i>
      <h5 class="fw-bold mb-0">{{ title }}</h5>
      <span class="badge rounded-pill bg-light text-secondary ms-2">{{ items?.length || 0 }}</span>
    </div>

    <div v-if="items && items.length > 0" class="wordcloud-container">
      <span
        v-for="item in items"
        :key="item.name"
        class="wordcloud-item d-inline-block px-2 py-1 m-1 rounded-3"
        :style="{
          fontSize: fontSizeMap[item.name],
          color: colorMap[item.name],
          backgroundColor: colorMap[item.name] + '15',
          cursor: clickable ? 'pointer' : 'default',
        }"
        :title="`${item.name}${item.count !== undefined ? ' (' + item.count + ')' : ''}`"
        @click="handleClick(item)"
      >
        {{ item.name }}
      </span>
    </div>

    <div v-else class="text-center text-muted py-4">
      <i class="bi bi-inbox fs-2 d-block mb-2"></i>
      <span>{{ emptyText }}</span>
    </div>
  </div>
</template>

<style scoped>
.wordcloud-wrapper {
  background: #f8f9fc;
  border-radius: 1rem;
  padding: 1.25rem;
}

.wordcloud-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  line-height: 1.6;
}

.wordcloud-item {
  transition: all 0.2s ease;
  white-space: nowrap;
  font-weight: 500;
  opacity: 0.85;
}

.wordcloud-item:hover {
  transform: scale(1.08);
  opacity: 1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>

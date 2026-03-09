import { defineStore } from 'pinia';
import { getTags, getRecommendations } from '@/api';

export const useRecommendationStore = defineStore('recommendation', {
  state: () => ({
    tags: [],
    recommendations: [],
    isLoading: false,
    error: null,
  }),

  actions: {
    async fetchTags() {
      this.isLoading = true;
      this.error = null;
      try {
        const response = await getTags();
        this.tags = response.data;
      } catch (err) {
        this.error = '获取标签失败，请稍后重试。';
        console.error(err);
      } finally {
        this.isLoading = false;
      }
    },

    async fetchRecommendations(request) {
      this.isLoading = true;
      this.error = null;
      this.recommendations = []; // 清空旧结果
      try {
        const response = await getRecommendations(request);
        this.recommendations = response.data;
      } catch (err) {
        this.error = '获取推荐失败，请检查您的输入或稍后重试。';
        console.error(err);
      } finally {
        this.isLoading = false;
      }
    },
  },
});

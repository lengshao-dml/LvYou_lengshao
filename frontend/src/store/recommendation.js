import { defineStore } from 'pinia';
import { getTags, getRecommendations, getCityByName } from '@/api';

export const useRecommendationStore = defineStore('recommendation', {
  state: () => ({
    tags: [],
    // 推荐结果
    recommendations: [],
    // 搜索结果
    searchedCities: [], 
    
    isLoading: false, // 用于推荐
    isSearching: false, // 用于城市搜索
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
      this.recommendations = [];
      this.searchedCities = []; // 清空搜索结果
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

    async fetchCityByName(cityName) {
      this.isSearching = true;
      this.error = null;
      this.recommendations = []; // 清空推荐结果
      this.searchedCities = [];
      try {
        const response = await getCityByName(cityName);
        this.searchedCities = response.data;
        if (this.searchedCities.length === 0) {
            this.error = `未找到与“${cityName}”相关的城市。`;
        }
      } catch (err) {
        if (err.response && err.response.status === 404) {
          this.error = `未找到与“${cityName}”相关的城市。`;
        } else {
          this.error = '搜索城市失败，请稍后重试。';
        }
        console.error(err);
      } finally {
        this.isSearching = false;
      }
    }
  },
});

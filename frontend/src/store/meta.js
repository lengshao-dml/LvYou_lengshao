import { defineStore } from 'pinia';
import { getTags, getCities } from '@/api';

export const useMetaStore = defineStore('meta', {
  state: () => ({
    tags: [],
    cities: [],
    isLoading: false,
    error: null,
  }),

  actions: {
    async fetchMeta() {
      // Only fetch if data is not already loaded
      if (this.tags.length > 0 && this.cities.length > 0) {
        return;
      }

      this.isLoading = true;
      this.error = null;
      try {
        // Fetch tags and cities in parallel
        const [tagsResponse, citiesResponse] = await Promise.all([
          getTags(),
          getCities(),
        ]);
        this.tags = tagsResponse.data;
        this.cities = citiesResponse.data;
      } catch (err) {
        this.error = '获取应用元数据失败。';
        console.error(err);
      } finally {
        this.isLoading = false;
      }
    },
  },
});

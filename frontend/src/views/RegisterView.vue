<script setup>
import { ref, onMounted, computed } from 'vue';
import { useAuthStore } from '@/store/auth';
import { useMetaStore } from '@/store/meta';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const metaStore = useMetaStore();
const router = useRouter();

// 表单数据
const username = ref('');
const password = ref('');
const homeCityName = ref('');
const interests = ref({}); // { '历史文化': 3, '自然风光': 5 }

// 用于城市自动补全
const citySearchText = ref('');
const filteredCities = computed(() => {
  if (!citySearchText.value) {
    return [];
  }
  return metaStore.cities
    .filter(city => city.pinyin.toLowerCase().includes(citySearchText.value.toLowerCase()) || city.name.includes(citySearchText.value))
    .slice(0, 5); // 最多显示5个匹配项
});

const selectCity = (city) => {
  homeCityName.value = city.name;
  citySearchText.value = city.name;
};

// 为兴趣标签打分
const setInterestRating = (tagName, rating) => {
  interests.value[tagName] = rating;
};

// 组件加载时获取元数据
onMounted(() => {
  metaStore.fetchMeta();
});

// 提交注册
const handleRegister = async () => {
  // 将1-5星评级转换为0.2-1.0的权重
  const weightedInterests = {};
  for (const [tag, rating] of Object.entries(interests.value)) {
    if (rating > 0) {
      weightedInterests[tag] = rating * 0.2;
    }
  }

  const registerRequest = {
    username: username.value,
    password: password.value,
    homeCityName: homeCityName.value,
    interests: weightedInterests,
  };

  try {
    await authStore.register(registerRequest);
    // 注册成功后跳转到主页
    router.push('/');
  } catch (error) {
    // 错误信息会由 store 处理，这里可以添加一些UI提示，如果需要
    console.error("注册失败:", authStore.error);
  }
};
</script>

<template>
  <div class="row justify-content-center">
    <div class="col-md-8 col-lg-6">
      <div class="card shadow-lg p-4">
        <div class="card-body">
          <h2 class="card-title text-center fw-bold mb-4">注册新用户</h2>
          <p class="text-center text-muted mb-4">填写您的偏好，以便我们为您提供更精准的推荐（可选）</p>

          <form @submit.prevent="handleRegister">
            <!-- 用户名和密码 -->
            <h5 class="mb-3">基础信息</h5>
            <div class="mb-3">
              <label for="username" class="form-label">用户名</label>
              <input type="text" id="username" class="form-control" v-model="username" required>
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">密码</label>
              <input type="password" id="password" class="form-control" v-model="password" required>
            </div>
            
            <hr class="my-4">

            <!-- 常居地 -->
            <h5 class="mb-3">选择您的常居地</h5>
            <div class="mb-3 dropdown">
              <input 
                type="text" 
                class="form-control dropdown-toggle" 
                data-bs-toggle="dropdown" 
                aria-expanded="false"
                placeholder="搜索城市..." 
                v-model="citySearchText"
                @focus="homeCityName = ''"
                @input="homeCityName = ''"
              >
              <ul class="dropdown-menu w-100" :class="{'show': filteredCities.length > 0 && !homeCityName}">
                <li v-for="city in filteredCities" :key="city.id">
                  <a class="dropdown-item" href="#" @click.prevent="selectCity(city)">{{ city.name }}</a>
                </li>
              </ul>
            </div>

            <hr class="my-4">

            <!-- 兴趣标签打分 -->
            <h5 class="mb-3">为您的兴趣打分 (1-5星)</h5>
            <div class="row">
              <div v-for="tag in metaStore.tags" :key="tag.id" class="col-12 col-md-6 mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <span class="fw-medium">{{ tag.name }}</span>
                  <div class="rating">
                    <i 
                      v-for="star in 5" 
                      :key="star"
                      class="bi"
                      :class="star <= (interests[tag.name] || 0) ? 'bi-star-fill text-warning' : 'bi-star'"
                      @click="setInterestRating(tag.name, star)"
                    ></i>
                  </div>
                </div>
              </div>
            </div>

            <!-- 提交按钮 -->
            <div class="d-grid mt-4">
              <button type="submit" class="btn btn-primary btn-lg" :disabled="authStore.isLoading">
                <span v-if="authStore.isLoading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                {{ authStore.isLoading ? ' 注册中...' : '完成注册' }}
              </button>
              <p v-if="authStore.error" class="text-danger text-center mt-2 small">{{ authStore.error }}</p>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rating i {
  cursor: pointer;
  font-size: 1.25rem;
  padding: 0 0.1rem;
}
</style>

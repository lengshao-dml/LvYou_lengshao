<script setup>
import { ref } from 'vue';
import { useAuthStore } from '@/store/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const username = ref('');
const password = ref('');

const handleLogin = async () => {
  try {
    await authStore.login({
      username: username.value,
      password: password.value,
    });
    // 登录成功后跳转到主页
    router.push('/');
  } catch (error) {
    // 错误信息会由 store 自动处理并显示
    console.error("登录失败:", authStore.error);
  }
};
</script>

<template>
  <div class="row justify-content-center">
    <div class="col-md-6 col-lg-4">
      <div class="card shadow-lg p-4">
        <div class="card-body">
          <h2 class="card-title text-center fw-bold mb-4">用户登录</h2>
          
          <form @submit.prevent="handleLogin">
            <div class="mb-3">
              <label for="username" class="form-label">用户名</label>
              <input type="text" id="username" class="form-control" v-model="username" required>
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">密码</label>
              <input type="password" id="password" class="form-control" v-model="password" required>
            </div>

            <div class="d-grid mt-4">
              <button type="submit" class="btn btn-primary btn-lg" :disabled="authStore.isLoading">
                <span v-if="authStore.isLoading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                {{ authStore.isLoading ? ' 登录中...' : '登录' }}
              </button>
              <p v-if="authStore.error" class="text-danger text-center mt-2 small">{{ authStore.error }}</p>
            </div>
          </form>

        </div>
      </div>
    </div>
  </div>
</template>

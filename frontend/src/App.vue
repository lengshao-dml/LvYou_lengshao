<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '@/store/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <header class="sticky-top">
    <nav class="navbar navbar-expand-lg bg-body-tertiary shadow-sm">
      <div class="container">
        <RouterLink class="navbar-brand" to="/">
          <i class="bi bi-map-fill text-primary"></i>
          <span class="fw-bold ms-2">旅城</span>
        </RouterLink>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item">
              <RouterLink class="nav-link" to="/">主页</RouterLink>
            </li>
            <template v-if="!authStore.token">
              <li class="nav-item">
                <RouterLink class="nav-link" to="/login">登录</RouterLink>
              </li>
              <li class="nav-item">
                <RouterLink class="nav-link" to="/register">注册</RouterLink>
              </li>
            </template>
            <template v-else>
               <li class="nav-item">
                 <a href="#" class="nav-link" @click.prevent="handleLogout">退出</a>
              </li>
            </template>
          </ul>
        </div>
      </div>
    </nav>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style>
/* You can add global styles here if needed */
</style>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
      <div class="container">
        <RouterLink class="navbar-brand" to="/">旅游推荐系统</RouterLink>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item">
              <RouterLink class="nav-link" to="/">主页</RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink class="nav-link" to="/login">登录</RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink class="nav-link" to="/register">注册</RouterLink>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  </header>

  <main class="container mt-4">
    <RouterView />
  </main>
</template>

<style>
/* You can add global styles here if needed */
</style>

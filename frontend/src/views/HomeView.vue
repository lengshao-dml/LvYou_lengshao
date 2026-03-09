<script setup>
import RecommendationForm from '@/components/RecommendationForm.vue';
import ResultsDisplay from '@/components/ResultsDisplay.vue';
</script>

<template>
  <div class="row">
    <div class="col-lg-4">
      <RecommendationForm />
    </div>
    <div class="col-lg-8">
      <ResultsDisplay />
    </div>
  </div>
</template>

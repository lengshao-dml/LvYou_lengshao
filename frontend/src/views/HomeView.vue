<script setup>
import HeroSection from '@/components/HeroSection.vue';
import RecommendationForm from '@/components/RecommendationForm.vue';
import ResultsDisplay from '@/components/ResultsDisplay.vue';
</script>

<template>
  <div>
    <HeroSection />
    
    <div id="recommend-section" class="container my-5">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-8">
          <RecommendationForm />
        </div>
      </div>
    </div>

    <div class="container mb-5">
       <ResultsDisplay />
    </div>

  </div>
</template>

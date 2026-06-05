<script setup>
import { provide } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/users'

const route = useRoute()
const userStore = useUserStore()

provide('getUserName', (id) => userStore.getUserName(id))
</script>

<template>
  <AuthLayout v-if="route.meta.layout === 'auth'">
    <router-view />
  </AuthLayout>
  <MainLayout v-else>
    <router-view />
  </MainLayout>
</template>

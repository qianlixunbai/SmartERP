import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUsers } from '@/api/auth'

export const useUserStore = defineStore('users', () => {
  const users = ref([])
  const loaded = ref(false)

  const userMap = computed(() => {
    const map = {}
    users.value.forEach(u => {
      map[u.id] = u
    })
    return map
  })

  async function fetchUsers() {
    if (loaded.value) return
    users.value = await getUsers()
    loaded.value = true
  }

  function getUserName(id) {
    return userMap.value[id]?.realName || `用户${id}`
  }

  function getUser(id) {
    return userMap.value[id] || null
  }

  return { users, userMap, loaded, fetchUsers, getUserName, getUser }
})

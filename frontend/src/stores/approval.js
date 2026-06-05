import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as leaveApi from '@/api/leave'

export const useApprovalStore = defineStore('approval', () => {
  const allRequests = ref([])
  const myRequests = ref([])
  const pendingRequests = ref([])
  const doneRequests = ref([])
  const currentDetail = ref(null)
  const currentRecords = ref([])
  const currentTasks = ref([])
  const loading = ref(false)

  async function fetchAllRequests() {
    allRequests.value = await leaveApi.getAllRequests()
  }

  async function fetchMyRequests() {
    myRequests.value = await leaveApi.getMyRequests()
  }

  async function fetchPendingRequests() {
    pendingRequests.value = await leaveApi.getPendingRequests()
  }

  async function fetchDoneRequests() {
    doneRequests.value = await leaveApi.getDoneRequests()
  }

  async function fetchDetail(id) {
    currentDetail.value = await leaveApi.getRequestDetail(id)
  }

  async function fetchRecords(id) {
    currentRecords.value = await leaveApi.getApprovalRecords(id)
  }

  async function fetchTasks(id) {
    currentTasks.value = await leaveApi.getPendingTasks(id)
  }

  async function submitLeave(formData) {
    await leaveApi.submitLeave(formData)
    await fetchMyRequests()
  }

  async function approve(requestId, action, comment) {
    await leaveApi.approveLeave(requestId, action, comment)
  }

  return {
    allRequests, myRequests, pendingRequests, doneRequests,
    currentDetail, currentRecords, currentTasks, loading,
    fetchAllRequests, fetchMyRequests, fetchPendingRequests, fetchDoneRequests,
    fetchDetail, fetchRecords, fetchTasks, submitLeave, approve
  }
})

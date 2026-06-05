import api from './index'

export function login(username, password) {
  return api.post('/login', { username, password })
}

export function getCurrentUser() {
  return api.get('/user/current')
}

export function logout() {
  return api.post('/logout')
}

export function getUsers() {
  return api.get('/users/list')
}

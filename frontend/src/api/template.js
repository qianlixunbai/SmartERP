import api from './index'

export function getTemplateList() {
  return api.get('/templates')
}

export function getTemplateById(id) {
  return api.get(`/templates/${id}`)
}

export function createTemplate(data) {
  return api.post('/templates', data)
}

export function updateTemplate(id, data) {
  return api.put(`/templates/${id}`, data)
}

export function deleteTemplate(id) {
  return api.delete(`/templates/${id}`)
}

export function getTemplateNodes(id) {
  return api.get(`/templates/${id}/nodes`)
}

export function saveTemplateNodes(id, nodes) {
  return api.post(`/templates/${id}/nodes`, nodes)
}

export function getTemplateFields(id) {
  return api.get(`/templates/${id}/fields`)
}

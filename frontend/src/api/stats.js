import api from './index'

export function getAvgDuration() {
  return api.get('/stats/avg-duration')
}

export function getTemplateUsage() {
  return api.get('/stats/template-usage')
}

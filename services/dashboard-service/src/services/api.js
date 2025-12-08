import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8083/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const getAlerts = async () => {
  const response = await api.get('/alerts')
  return response.data
}

export const getAlertsByStatus = async (status) => {
  const response = await api.get(`/alerts/status/${status}`)
  return response.data
}

export const getAlertById = async (id) => {
  const response = await api.get(`/alerts/${id}`)
  return response.data
}

export const updateAlertStatus = async (id, status, reviewedBy, notes) => {
  const response = await api.patch(`/alerts/${id}/status`, null, {
    params: { status, reviewedBy, notes }
  })
  return response.data
}

export default api

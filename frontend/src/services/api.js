import axios from 'axios';
import { reactive } from 'vue';

const API_BASE_KEY = 'dorm_repair_api_base';

export const apiState = reactive({
  baseURL: localStorage.getItem(API_BASE_KEY) || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  lastError: ''
});

const http = axios.create({ timeout: 8000, withCredentials: true });
let csrf = null;

http.interceptors.request.use((config) => {
  config.baseURL = apiState.baseURL;

  return config;
});

function unwrap(response) {
  return response?.data?.data ?? response?.data;
}

async function request(method, url, { data, params } = {}) {
  try {
    if (!['get','head','options'].includes(method.toLowerCase())) {
      csrf = unwrap(await http.get('/auth/csrf'));
    }
    const headers = csrf ? { [csrf.headerName]: csrf.token } : {};
    const response = await http.request({ method, url, data, params, headers });
    apiState.lastError = '';
    return unwrap(response);
  } catch (error) {
    apiState.lastError = error?.response?.data?.message || error?.message || '请求失败';
    throw new Error(apiState.lastError);
  }
}

export function setApiBaseURL(url) {
  csrf = null;
  apiState.baseURL = url || 'http://localhost:8080/api';
  localStorage.setItem(API_BASE_KEY, apiState.baseURL);
}

const api = {
  login: (payload) => request('post', '/auth/login', { data: payload }),
  register: (payload) => request('post', '/auth/register', { data: payload }),
  me: () => request('get', '/auth/me'),
  logout: () => request('post', '/auth/logout'),

  publicBuildings: () => request('get', '/public/dorm/buildings'),
  publicRooms: (buildingId) => request('get', '/public/dorm/rooms', { params: { buildingId } }),
  publicCategories: () => request('get', '/public/categories'),
  publicNotices: () => request('get', '/public/notices'),

  listRepairs: (params) => request('get', '/repair-orders', { params }),
  getRepair: (id) => request('get', `/repair-orders/${id}`),
  createRepair: (payload) => request('post', '/repair-orders', { data: payload }),
  cancelRepair: (id) => request('put', `/repair-orders/${id}/cancel`),
  auditRepair: (id, payload) => request('put', `/repair-orders/${id}/audit`, { data: payload }),
  assignRepair: (id, payload) => request('put', `/repair-orders/${id}/assign`, { data: payload }),
  startRepair: (id) => request('put', `/repair-orders/${id}/start`),
  finishRepair: (id, payload) => request('put', `/repair-orders/${id}/finish`, { data: payload }),
  feedbackRepair: (id, payload) => request('put', `/repair-orders/${id}/feedback`, { data: payload }),
  reopenRepair: (id, payload) => request('put', `/repair-orders/${id}/reopen`, { data: payload }),

  listBuildings: () => request('get', '/dorm/buildings'),
  saveBuilding: (payload) => request(payload.id ? 'put' : 'post', payload.id ? `/dorm/buildings/${payload.id}` : '/dorm/buildings', { data: payload }),
  deleteBuilding: (id) => request('delete', `/dorm/buildings/${id}`),
  listRooms: (params) => request('get', '/dorm/rooms', { params }),
  roomOptions: (buildingId) => request('get', '/dorm/rooms/options', { params: { buildingId } }),
  saveRoom: (payload) => request(payload.id ? 'put' : 'post', payload.id ? `/dorm/rooms/${payload.id}` : '/dorm/rooms', { data: payload }),
  deleteRoom: (id) => request('delete', `/dorm/rooms/${id}`),

  listNotices: (params) => request('get', '/notices', { params }),
  saveNotice: (payload) => request(payload.id ? 'put' : 'post', payload.id ? `/notices/${payload.id}` : '/notices', { data: payload }),
  deleteNotice: (id) => request('delete', `/notices/${id}`),

  categories: () => request('get', '/meta/categories'),
  repairers: () => request('get', '/meta/repairers'),
  stats: () => request('get', '/statistics/overview'),

  roles: () => request('get', '/users/roles'),
  listUsers: (params) => request('get', '/users', { params }),
  saveUser: (payload) => request(payload.id ? 'put' : 'post', payload.id ? `/users/${payload.id}` : '/users', { data: payload }),
  disableUser: (id) => request('put', `/users/${id}/disable`),
  resetPassword: (id, newPassword) => request('put', `/users/${id}/reset-password`, { data: { newPassword } }),
  deleteUser: (id) => request('delete', `/users/${id}`),
  logs: (params) => request('get', '/logs', { params })
};

export default api;

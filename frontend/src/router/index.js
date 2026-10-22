import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '../pages/LoginPage.js';
import DashboardPage from '../pages/DashboardPage.js';
import { auth, defaultRouteForRole } from '../store/auth.js';
import { ROLES } from '../data/options.js';

const routes = [
  { path: '/', redirect: () => defaultRouteForRole(auth.user?.roleCode) },
  { path: '/login', component: LoginPage, meta: { public: true } },
  { path: '/student', component: DashboardPage, meta: { roles: [ROLES.STUDENT] } },
  { path: '/admin', component: DashboardPage, meta: { roles: [ROLES.DORM_ADMIN] } },
  { path: '/worker', component: DashboardPage, meta: { roles: [ROLES.REPAIRER] } },
  { path: '/system', component: DashboardPage, meta: { roles: [ROLES.SYS_ADMIN] } },
  { path: '/:pathMatch(.*)*', redirect: () => defaultRouteForRole(auth.user?.roleCode) }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.meta.public) return auth.user ? defaultRouteForRole(auth.user.roleCode) : true;
  if (!auth.user) return { path: '/login', query: { redirect: to.fullPath } };
  if (to.meta.roles && !to.meta.roles.includes(auth.user.roleCode)) return defaultRouteForRole(auth.user.roleCode);
  return true;
});

export default router;

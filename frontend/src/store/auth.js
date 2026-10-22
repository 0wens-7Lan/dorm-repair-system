// AI-assisted: authentication is held in an HttpOnly cookie managed by the server.
import { reactive } from 'vue';
import api from '../services/api.js';
import { ROLES } from '../data/options.js';
localStorage.removeItem('dorm_repair_token');
localStorage.removeItem('dorm_repair_user');
export const auth=reactive({
  user:null, loading:false, error:'',
  async restore() { try { this.user=await api.me(); } catch { this.user=null; } },
  async login(credentials) {
    this.loading=true;this.error='';
    try { const result=await api.login(credentials);this.user=result.user;return result; }
    catch(error) { this.error=error.message;throw error; } finally { this.loading=false; }
  },
  async register(payload) {
    this.loading=true;this.error='';
    try { await api.register(payload);return await this.login({username:payload.username,password:payload.password}); }
    catch(error) {this.error=error.message;throw error;} finally {this.loading=false;}
  },
  async logout() { await api.logout();this.user=null; }
});
export function hasAnyRole(roles=[]) { return !roles.length || Boolean(auth.user && roles.includes(auth.user.roleCode)); }
export function defaultRouteForRole(role) {
  return ({[ROLES.STUDENT]:'/student',[ROLES.DORM_ADMIN]:'/admin',[ROLES.REPAIRER]:'/worker',[ROLES.SYS_ADMIN]:'/system'})[role] || '/login';
}

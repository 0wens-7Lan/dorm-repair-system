import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api, { apiState, setApiBaseURL } from '../services/api.js';
import { auth, defaultRouteForRole } from '../store/auth.js';

export default {
  name: 'LoginPage',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const mode = ref('login');
    const message = ref('');
    const apiBase = ref(apiState.baseURL);
    const buildings = ref([]);
    const rooms = ref([]);

    const loginForm = reactive({ username: 'student', password: 'password' });
    const registerForm = reactive({
      username: '',
      password: '',
      name: '',
      studentNo: '',
      phone: '',
      buildingId: '',
      dormRoomId: ''
    });

    async function loadBuildings() {
      buildings.value = await api.publicBuildings();
      if (buildings.value[0]) registerForm.buildingId = buildings.value[0].id;
      await loadRooms();
    }

    async function loadRooms() {
      rooms.value = await api.publicRooms(registerForm.buildingId || undefined);
      if (rooms.value[0]) registerForm.dormRoomId = rooms.value[0].id;
    }

    watch(() => registerForm.buildingId, () => {
      registerForm.dormRoomId = '';
      loadRooms().catch((error) => { message.value = error.message; });
    });

    async function login() {
      message.value = '';
      try {
        await auth.login(loginForm);
        router.replace(route.query.redirect || defaultRouteForRole(auth.user.roleCode));
      } catch (error) {
        message.value = error.message;
      }
    }

    async function register() {
      message.value = '';
      try {
        await auth.register({
          username: registerForm.username,
          password: registerForm.password,
          name: registerForm.name,
          studentNo: registerForm.studentNo,
          phone: registerForm.phone,
          dormRoomId: Number(registerForm.dormRoomId)
        });
        router.replace(defaultRouteForRole(auth.user.roleCode));
      } catch (error) {
        message.value = error.message;
      }
    }

    function fill(username) {
      loginForm.username = username;
      loginForm.password = 'password';
      mode.value = 'login';
    }

    function saveApiBase() {
      setApiBaseURL(apiBase.value);
      message.value = '后端 API 地址已保存';
      loadBuildings().catch((error) => { message.value = error.message; });
    }

    onMounted(() => {
      loadBuildings().catch((error) => { message.value = `无法连接后端：${error.message}`; });
    });

    return { auth, mode, message, apiBase, buildings, rooms, loginForm, registerForm, login, register, fill, saveApiBase };
  },
  template: `
    <main class="login-page">
      <section class="login-panel">
        <div>
          <p class="eyebrow">高校宿舍智能报修管理系统</p>
          <h1>宿舍报修 Web 平台</h1>
          <p class="muted">学生在线报修，宿舍管理员审核派单，维修人员处理，学生确认评价。</p>
        </div>

        <div class="tabs">
          <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">账号登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">学生注册</button>
        </div>

        <form v-if="mode === 'login'" class="form-grid" @submit.prevent="login">
          <label>账号<input v-model.trim="loginForm.username" autocomplete="username" required /></label>
          <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" required /></label>
          <button class="primary" type="submit" :disabled="auth.loading">{{ auth.loading ? '登录中' : '登录' }}</button>
        </form>

        <form v-else class="form-grid" @submit.prevent="register">
          <label>账号<input v-model.trim="registerForm.username" required /></label>
          <label>密码<input v-model="registerForm.password" type="password" required /></label>
          <label>姓名<input v-model.trim="registerForm.name" required /></label>
          <label>学号<input v-model.trim="registerForm.studentNo" required /></label>
          <label>手机号<input v-model.trim="registerForm.phone" required /></label>
          <label>宿舍楼<select v-model="registerForm.buildingId" required><option v-for="building in buildings" :key="building.id" :value="building.id">{{ building.name }}</option></select></label>
          <label>宿舍号<select v-model="registerForm.dormRoomId" required><option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.roomNo }}</option></select></label>
          <button class="primary" type="submit" :disabled="auth.loading">注册并登录</button>
        </form>

        <div class="api-box">
          <label>后端 API 地址<input v-model.trim="apiBase" /></label>
          <button type="button" @click="saveApiBase">保存地址</button>
        </div>

        <p v-if="message || auth.error" class="message error">{{ message || auth.error }}</p>

        <div class="demo-accounts">
          <strong>测试账号</strong>
          <button type="button" @click="fill('student')">学生</button>
          <button type="button" @click="fill('admin')">宿舍管理员</button>
          <button type="button" @click="fill('repairer')">维修人员</button>
          <button type="button" @click="fill('sysadmin')">系统管理员</button>
          <span>密码均为 password</span>
        </div>
      </section>
    </main>
  `
};

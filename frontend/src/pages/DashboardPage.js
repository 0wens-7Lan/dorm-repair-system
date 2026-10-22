import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import api from '../services/api.js';
import { auth } from '../store/auth.js';
import { canStudentCancel, roleLabel, ROLES, STATUS, STATUS_OPTIONS, statusLabel } from '../data/options.js';

function newRepairForm() {
  return {
    categoryId: '',
    buildingId: '',
    dormRoomId: '',
    description: '',
    contactPhone: auth.user?.phone || '',
    expectedTime: '',
    imageUrl: ''
  };
}

export default {
  name: 'DashboardPage',
  setup() {
    const router = useRouter();
    const activeTab = ref('');
    const message = ref('');
    const loading = ref(false);

    const categories = ref([]);
    const buildings = ref([]);
    const rooms = ref([]);
    const repairers = ref([]);
    const roles = ref([]);
    const notices = ref([]);
    const repairs = ref([]);
    const selectedRepair = ref(null);
    const stats = ref(null);
    const users = ref([]);
    const logs = ref([]);
    const roomRows = ref([]);

    const filters = reactive({ status: '', categoryId: '', buildingId: '', studentName: '', startDate: '', endDate: '', keyword: '', roleCode: '' });
    const repairPage = reactive({ current: 1, size: 10, total: 0 });
    const userPage = reactive({ current: 1, size: 10, total: 0 });
    const logPage = reactive({ current: 1, size: 10, total: 0 });

    const repairForm = reactive(newRepairForm());
    const auditForm = reactive({ approved: true, rejectReason: '' });
    const assignForm = reactive({ repairerId: '', remark: '' });
    const finishForm = reactive({ faultReason: '', repairMethod: '', replacedParts: '', resultSummary: '', resultImageUrl: '' });
    const feedbackForm = reactive({ speedScore: 5, attitudeScore: 5, qualityScore: 5, overallScore: 5, comment: '', unresolved: false });
    const buildingForm = reactive({ id: null, name: '', floors: 6, address: '' });
    const roomForm = reactive({ id: null, buildingId: '', floor: 1, roomNo: '', capacity: 4, occupied: 0 });
    const noticeForm = reactive({ id: null, title: '', content: '', status: 'PUBLISHED' });
    const userForm = reactive({ id: null, username: '', password: 'password', name: '', studentNo: '', phone: '', roleId: '', dormRoomId: '', status: 'ACTIVE' });

    const navItems = computed(() => {
      if (auth.user?.roleCode === ROLES.STUDENT) return [
        { key: 'home', label: '首页公告' },
        { key: 'submit', label: '提交报修' },
        { key: 'my', label: '我的报修' }
      ];
      if (auth.user?.roleCode === ROLES.DORM_ADMIN) return [
        { key: 'repairs', label: '审核派单' },
        { key: 'dorms', label: '宿舍信息' },
        { key: 'notices', label: '公告管理' },
        { key: 'stats', label: '数据统计' }
      ];
      if (auth.user?.roleCode === ROLES.REPAIRER) return [
        { key: 'tasks', label: '维修任务' }
      ];
      return [
        { key: 'users', label: '用户管理' },
        { key: 'notices', label: '公告管理' },
        { key: 'logs', label: '操作日志' },
        { key: 'stats', label: '数据统计' }
      ];
    });

    const selectedOrder = computed(() => selectedRepair.value?.order || null);
    const selectedHeader = computed(() => selectedRepair.value?.header || {});
    const timeline = computed(() => selectedRepair.value?.timeline || []);

    async function run(task, successText = '') {
      loading.value = true;
      message.value = '';
      try {
        const result = await task();
        if (successText) message.value = successText;
        return result;
      } catch (error) {
        message.value = error.message || '操作失败';
        throw error;
      } finally {
        loading.value = false;
      }
    }

    async function loadBase() {
      categories.value = await api.categories().catch(() => api.publicCategories());
      buildings.value = await api.listBuildings().catch(() => api.publicBuildings());
      if (buildings.value[0]) {
        repairForm.buildingId = buildings.value[0].id;
        roomForm.buildingId = buildings.value[0].id;
        await loadRoomOptions();
      }
      if (categories.value[0]) repairForm.categoryId = categories.value[0].id;
      if (auth.user?.roleCode !== ROLES.STUDENT) {
        repairers.value = await api.repairers().catch(() => []);
        if (repairers.value[0]) assignForm.repairerId = repairers.value[0].id;
      }
      if (auth.user?.roleCode === ROLES.SYS_ADMIN) {
        roles.value = await api.roles();
        if (roles.value[0]) userForm.roleId = roles.value[0].id;
      }
    }

    async function loadRoomOptions() {
      const buildingId = repairForm.buildingId || roomForm.buildingId || undefined;
      rooms.value = await api.roomOptions(buildingId).catch(() => []);
      if (rooms.value[0]) {
        if (!repairForm.dormRoomId) repairForm.dormRoomId = rooms.value[0].id;
        if (!userForm.dormRoomId) userForm.dormRoomId = rooms.value[0].id;
      }
    }

    watch(() => repairForm.buildingId, () => {
      repairForm.dormRoomId = '';
      loadRoomOptions();
    });

    async function loadNotices() {
      if (auth.user?.roleCode === ROLES.STUDENT) {
        notices.value = await api.publicNotices();
      } else {
        const result = await api.listNotices({ current: 1, size: 50 });
        notices.value = result.records || [];
      }
    }

    function repairParams() {
      return {
        current: repairPage.current,
        size: repairPage.size,
        status: filters.status || undefined,
        categoryId: filters.categoryId || undefined,
        buildingId: filters.buildingId || undefined,
        studentName: filters.studentName || undefined,
        repairerId: auth.user?.roleCode === ROLES.REPAIRER ? auth.user.id : undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined
      };
    }

    async function loadRepairs(reset = false) {
      if (reset) repairPage.current = 1;
      const result = await api.listRepairs(repairParams());
      repairs.value = result.records || [];
      repairPage.total = result.total || 0;
    }

    async function selectRepair(row) {
      selectedRepair.value = await api.getRepair(row.id);
      auditForm.approved = true;
      auditForm.rejectReason = '';
      assignForm.remark = '';
      if (!assignForm.repairerId && repairers.value[0]) assignForm.repairerId = repairers.value[0].id;
    }

    async function submitRepair() {
      await run(async () => {
        const created = await api.createRepair({
          categoryId: Number(repairForm.categoryId),
          dormRoomId: Number(repairForm.dormRoomId),
          description: repairForm.description,
          contactPhone: repairForm.contactPhone,
          expectedTime: repairForm.expectedTime || null,
          imageUrl: repairForm.imageUrl
        });
        Object.assign(repairForm, newRepairForm());
        await loadBase();
        await loadRepairs(true);
        selectedRepair.value = await api.getRepair(created.id);
      }, '报修单已提交，初始状态为待审核');
    }

    async function cancelRepair() {
      await run(async () => {
        const id = selectedOrder.value.id;
        await api.cancelRepair(id);
        await loadRepairs();
        await selectRepair({ id });
      }, '报修单已取消');
    }

    async function auditRepair() {
      await run(async () => {
        const id = selectedOrder.value.id;
        await api.auditRepair(id, auditForm);
        await loadRepairs();
        await selectRepair({ id });
      }, auditForm.approved ? '审核已通过，工单进入待派单' : '报修单已驳回');
    }

    async function assignRepair() {
      await run(async () => {
        const id = selectedOrder.value.id;
        await api.assignRepair(id, { repairerId: Number(assignForm.repairerId), remark: assignForm.remark });
        await loadRepairs();
        await selectRepair({ id });
      }, '派单完成，维修人员可查看该任务');
    }

    async function startRepair() {
      await run(async () => {
        const id = selectedOrder.value.id;
        await api.startRepair(id);
        await loadRepairs();
        await selectRepair({ id });
      }, '已开始维修，状态变为维修中');
    }

    async function finishRepair() {
      await run(async () => {
        const id = selectedOrder.value.id;
        await api.finishRepair(id, finishForm);
        Object.assign(finishForm, { faultReason: '', repairMethod: '', replacedParts: '', resultSummary: '', resultImageUrl: '' });
        await loadRepairs();
        await selectRepair({ id });
      }, '维修结果已提交，状态变为待确认');
    }

    async function feedbackRepair() {
      await run(async () => {
        const id = selectedOrder.value.id;
        await api.feedbackRepair(id, feedbackForm);
        await loadRepairs();
        await selectRepair({ id });
      }, feedbackForm.unresolved ? '未解决反馈已提交，工单回到待派单' : '已确认完成并提交评价');
    }

    async function loadDorms() {
      buildings.value = await api.listBuildings();
      const result = await api.listRooms({ current: 1, size: 100, buildingId: filters.buildingId || undefined });
      roomRows.value = result.records || [];
    }

    async function saveBuilding() {
      await run(async () => {
        await api.saveBuilding(buildingForm);
        Object.assign(buildingForm, { id: null, name: '', floors: 6, address: '' });
        await loadDorms();
      }, '宿舍楼已保存');
    }

    function editBuilding(item) {
      Object.assign(buildingForm, { id: item.id, name: item.name, floors: item.floors, address: item.address || '' });
    }

    async function saveRoom() {
      await run(async () => {
        await api.saveRoom(roomForm);
        Object.assign(roomForm, { id: null, buildingId: roomForm.buildingId, floor: 1, roomNo: '', capacity: 4, occupied: 0 });
        await loadDorms();
      }, '宿舍房间已保存');
    }

    function editRoom(item) {
      Object.assign(roomForm, {
        id: item.id,
        buildingId: item.buildingId,
        floor: item.floor,
        roomNo: item.roomNo,
        capacity: item.capacity,
        occupied: item.occupied
      });
    }

    async function saveNotice() {
      await run(async () => {
        await api.saveNotice(noticeForm);
        Object.assign(noticeForm, { id: null, title: '', content: '', status: 'PUBLISHED' });
        await loadNotices();
      }, '公告已保存');
    }

    function editNotice(item) {
      Object.assign(noticeForm, { id: item.id, title: item.title, content: item.content, status: item.status });
    }

    async function removeNotice(id) {
      await run(async () => {
        await api.deleteNotice(id);
        await loadNotices();
      }, '公告已删除');
    }

    async function loadStats() {
      stats.value = await api.stats();
    }

    async function loadUsers(reset = false) {
      if (reset) userPage.current = 1;
      const result = await api.listUsers({
        current: userPage.current,
        size: userPage.size,
        keyword: filters.keyword || undefined,
        roleCode: filters.roleCode || undefined
      });
      users.value = result.records || [];
      userPage.total = result.total || 0;
    }

    function editUser(row) {
      Object.assign(userForm, {
        id: row.id,
        username: row.username,
        password: 'password',
        name: row.name,
        studentNo: row.studentNo || '',
        phone: row.phone || '',
        roleId: roles.value.find((role) => role.code === row.roleCode)?.id || row.roleId || '',
        dormRoomId: row.dormRoomId || '',
        status: row.status || 'ACTIVE'
      });
    }

    async function saveUser() {
      await run(async () => {
        await api.saveUser({ ...userForm, roleId: Number(userForm.roleId), dormRoomId: userForm.dormRoomId ? Number(userForm.dormRoomId) : null });
        Object.assign(userForm, { id: null, username: '', password: 'password', name: '', studentNo: '', phone: '', roleId: roles.value[0]?.id || '', dormRoomId: '', status: 'ACTIVE' });
        await loadUsers();
      }, '用户已保存');
    }

    async function disableUser(row) {
      await run(async () => {
        await api.disableUser(row.id);
        await loadUsers();
      }, '用户已禁用');
    }

    async function resetPassword(row) {
      await run(() => api.resetPassword(row.id, 'password'), '密码已重置为 password');
    }

    async function removeUser(row) {
      await run(async () => {
        await api.deleteUser(row.id);
        await loadUsers();
      }, '用户已删除');
    }

    async function loadLogs(reset = false) {
      if (reset) logPage.current = 1;
      const result = await api.logs({ current: logPage.current, size: logPage.size, username: filters.keyword || undefined });
      logs.value = result.records || [];
      logPage.total = result.total || 0;
    }

    function nextPage(page, loader) {
      page.current += 1;
      loader();
    }

    function prevPage(page, loader) {
      page.current = Math.max(1, page.current - 1);
      loader();
    }

    async function logout() {
      await auth.logout();
      router.replace('/login');
    }

    async function loadTab(tab) {
      selectedRepair.value = null;
      if (tab === 'home') await Promise.all([loadNotices(), loadRepairs(true)]);
      if (['my', 'repairs', 'tasks'].includes(tab)) await loadRepairs(true);
      if (tab === 'dorms') await loadDorms();
      if (tab === 'notices') await loadNotices();
      if (tab === 'stats') await loadStats();
      if (tab === 'users') await loadUsers(true);
      if (tab === 'logs') await loadLogs(true);
    }

    watch(activeTab, (tab) => {
      if (tab) loadTab(tab).catch((error) => { message.value = error.message; });
    });

    onMounted(async () => {
      try {
        activeTab.value = navItems.value[0]?.key || '';
        await loadBase();
        await loadTab(activeTab.value);
      } catch (error) {
        message.value = error.message;
      }
    });

    return {
      auth, activeTab, navItems, message, loading, categories, buildings, rooms, repairers, roles,
      notices, repairs, selectedRepair, selectedOrder, selectedHeader, timeline, stats, users, logs, roomRows,
      filters, repairPage, userPage, logPage, repairForm, auditForm, assignForm, finishForm, feedbackForm,
      buildingForm, roomForm, noticeForm, userForm, ROLES, STATUS, STATUS_OPTIONS,
      roleLabel, statusLabel, canStudentCancel, loadRepairs, selectRepair, submitRepair, cancelRepair,
      auditRepair, assignRepair, startRepair, finishRepair, feedbackRepair, saveBuilding, editBuilding,
      saveRoom, editRoom, saveNotice, editNotice, removeNotice, loadUsers, editUser, saveUser,
      disableUser, resetPassword, removeUser, loadLogs, nextPage, prevPage, logout
    };
  },
  template: `
    <div class="app-layout">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">修</span>
          <div>
            <strong>宿舍报修系统</strong>
            <small>{{ roleLabel(auth.user?.roleCode) }}</small>
          </div>
        </div>
        <nav>
          <button v-for="item in navItems" :key="item.key" :class="{ active: activeTab === item.key }" @click="activeTab = item.key">
            {{ item.label }}
          </button>
        </nav>
        <div class="account">
          <span>{{ auth.user?.name }}</span>
          <small>{{ auth.user?.username }}</small>
          <button @click="logout">退出登录</button>
        </div>
      </aside>

      <main class="workspace">
        <header class="topbar">
          <div>
            <h1>{{ navItems.find(item => item.key === activeTab)?.label }}</h1>
            <p>当前登录角色：{{ roleLabel(auth.user?.roleCode) }}</p>
          </div>
        </header>
        <p v-if="message" class="message" :class="{ error: message.includes('失败') || message.includes('无权') || message.includes('不能') }">{{ message }}</p>

        <section v-if="activeTab === 'home'" class="section-grid">
          <div class="panel">
            <h2>公告</h2>
            <article v-for="notice in notices" :key="notice.id" class="list-card">
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.content }}</p>
              <small>{{ notice.publishTime }}</small>
            </article>
          </div>
          <div class="panel">
            <h2>最近报修</h2>
            <table>
              <thead><tr><th>编号</th><th>类型</th><th>状态</th><th>提交时间</th></tr></thead>
              <tbody>
                <tr v-for="row in repairs" :key="row.id" @click="selectRepair(row)">
                  <td>{{ row.orderNo }}</td><td>{{ row.categoryName }}</td><td><span class="badge">{{ statusLabel(row.status) }}</span></td><td>{{ row.submitTime }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="activeTab === 'submit'" class="panel">
          <h2>提交宿舍报修</h2>
          <form class="form-grid wide" @submit.prevent="submitRepair">
            <label>报修类型<select v-model="repairForm.categoryId" required><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
            <label>宿舍楼<select v-model="repairForm.buildingId" required><option v-for="building in buildings" :key="building.id" :value="building.id">{{ building.name }}</option></select></label>
            <label>宿舍号<select v-model="repairForm.dormRoomId" required><option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.roomNo }}</option></select></label>
            <label>联系电话<input v-model.trim="repairForm.contactPhone" required /></label>
            <label>期望维修时间<input v-model="repairForm.expectedTime" type="datetime-local" /></label>
            <label>故障图片地址<input v-model.trim="repairForm.imageUrl" placeholder="可填图片 URL" /></label>
            <label class="full">故障描述<textarea v-model.trim="repairForm.description" required rows="4"></textarea></label>
            <button class="primary">提交报修</button>
          </form>
        </section>

        <section v-if="['my','repairs','tasks'].includes(activeTab)" class="split">
          <div class="panel">
            <h2>{{ activeTab === 'tasks' ? '我的维修任务' : '报修工单' }}</h2>
            <div class="filters">
              <select v-model="filters.status" @change="loadRepairs(true)"><option value="">全部状态</option><option v-for="item in STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option></select>
              <select v-if="activeTab === 'repairs'" v-model="filters.categoryId" @change="loadRepairs(true)"><option value="">全部类型</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select>
              <select v-if="activeTab === 'repairs'" v-model="filters.buildingId" @change="loadRepairs(true)"><option value="">全部宿舍楼</option><option v-for="building in buildings" :key="building.id" :value="building.id">{{ building.name }}</option></select>
              <input v-if="activeTab === 'repairs'" v-model.trim="filters.studentName" placeholder="学生姓名" @keyup.enter="loadRepairs(true)" />
              <input v-if="activeTab === 'repairs'" v-model="filters.startDate" type="date" @change="loadRepairs(true)" />
              <input v-if="activeTab === 'repairs'" v-model="filters.endDate" type="date" @change="loadRepairs(true)" />
              <button @click="loadRepairs(true)">查询</button>
            </div>
            <table>
              <thead><tr><th>编号</th><th>学生</th><th>类型</th><th>宿舍</th><th>状态</th><th>维修人员</th></tr></thead>
              <tbody>
                <tr v-for="row in repairs" :key="row.id" :class="{ selected: selectedOrder?.id === row.id }" @click="selectRepair(row)">
                  <td>{{ row.orderNo }}</td>
                  <td>{{ row.studentName || '-' }}</td>
                  <td>{{ row.categoryName || '-' }}</td>
                  <td>{{ row.buildingName }} {{ row.roomNo }}</td>
                  <td><span class="badge">{{ statusLabel(row.status) }}</span></td>
                  <td>{{ row.repairerName || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div class="pager">
              <button :disabled="repairPage.current <= 1" @click="prevPage(repairPage, loadRepairs)">上一页</button>
              <span>第 {{ repairPage.current }} 页 / 共 {{ repairPage.total }} 条</span>
              <button :disabled="repairPage.current * repairPage.size >= repairPage.total" @click="nextPage(repairPage, loadRepairs)">下一页</button>
            </div>
          </div>

          <div class="panel detail-panel" v-if="selectedOrder">
            <h2>{{ selectedOrder.orderNo }} <span class="badge">{{ statusLabel(selectedOrder.status) }}</span></h2>
            <dl class="kv">
              <dt>学生</dt><dd>{{ selectedHeader.studentName }}</dd>
              <dt>宿舍</dt><dd>{{ selectedHeader.buildingName }} {{ selectedHeader.roomNo }}</dd>
              <dt>类型</dt><dd>{{ selectedHeader.categoryName }}</dd>
              <dt>描述</dt><dd>{{ selectedOrder.description }}</dd>
              <dt>联系电话</dt><dd>{{ selectedOrder.contactPhone }}</dd>
              <dt>维修人员</dt><dd>{{ selectedHeader.repairerName || '-' }}</dd>
              <dt>处理结果</dt><dd>{{ selectedOrder.resultSummary || '-' }}</dd>
              <dt>驳回/反馈</dt><dd>{{ selectedOrder.rejectReason || selectedOrder.reopenReason || '-' }}</dd>
            </dl>

            <div class="timeline">
              <div v-for="item in timeline" :key="item.id">
                <span></span>
                <div><strong>{{ item.action }}</strong><p>{{ item.detail }}</p><small>{{ item.createdAt }}</small></div>
              </div>
            </div>

            <div v-if="auth.user.roleCode === ROLES.STUDENT" class="actions">
              <button v-if="canStudentCancel(selectedOrder.status)" @click="cancelRepair">取消报修</button>
              <form v-if="selectedOrder.status === STATUS.PENDING_CONFIRM" class="form-grid" @submit.prevent="feedbackRepair">
                <label>维修速度评分<input v-model.number="feedbackForm.speedScore" type="number" min="1" max="5" /></label>
                <label>服务态度评分<input v-model.number="feedbackForm.attitudeScore" type="number" min="1" max="5" /></label>
                <label>维修质量评分<input v-model.number="feedbackForm.qualityScore" type="number" min="1" max="5" /></label>
                <label>总体评分<input v-model.number="feedbackForm.overallScore" type="number" min="1" max="5" /></label>
                <label class="full">文字评价/未解决说明<textarea v-model.trim="feedbackForm.comment" rows="3"></textarea></label>
                <label class="checkbox"><input v-model="feedbackForm.unresolved" type="checkbox" /> 未解决，需要重新处理</label>
                <button class="primary">{{ feedbackForm.unresolved ? '提交未解决反馈' : '确认完成并评价' }}</button>
              </form>
            </div>

            <div v-if="auth.user.roleCode === ROLES.DORM_ADMIN" class="actions">
              <form v-if="selectedOrder.status === STATUS.PENDING_REVIEW" class="form-grid" @submit.prevent="auditRepair">
                <label>审核结果<select v-model="auditForm.approved"><option :value="true">通过</option><option :value="false">驳回</option></select></label>
                <label class="full">驳回原因<textarea v-model.trim="auditForm.rejectReason" rows="2"></textarea></label>
                <button class="primary">提交审核</button>
              </form>
              <form v-if="selectedOrder.status === STATUS.PENDING_ASSIGN" class="form-grid" @submit.prevent="assignRepair">
                <label>维修人员<select v-model="assignForm.repairerId" required><option v-for="worker in repairers" :key="worker.id" :value="worker.id">{{ worker.name }}</option></select></label>
                <label>派单备注<input v-model.trim="assignForm.remark" /></label>
                <button class="primary">派单</button>
              </form>
            </div>

            <div v-if="auth.user.roleCode === ROLES.REPAIRER" class="actions">
              <button v-if="selectedOrder.status === STATUS.PENDING_ASSIGN" class="primary" @click="startRepair">开始维修</button>
              <form v-if="selectedOrder.status === STATUS.REPAIRING" class="form-grid" @submit.prevent="finishRepair">
                <label>故障原因<input v-model.trim="finishForm.faultReason" required /></label>
                <label>维修方式<input v-model.trim="finishForm.repairMethod" required /></label>
                <label>更换配件<input v-model.trim="finishForm.replacedParts" /></label>
                <label>凭证图片地址<input v-model.trim="finishForm.resultImageUrl" /></label>
                <label class="full">处理结果<textarea v-model.trim="finishForm.resultSummary" rows="3" required></textarea></label>
                <button class="primary">提交维修完成</button>
              </form>
            </div>
          </div>
        </section>

        <section v-if="activeTab === 'dorms'" class="section-grid">
          <div class="panel">
            <h2>宿舍楼</h2>
            <form class="form-grid" @submit.prevent="saveBuilding">
              <label>名称<input v-model.trim="buildingForm.name" required /></label>
              <label>楼层<input v-model.number="buildingForm.floors" type="number" min="1" /></label>
              <label class="full">位置<input v-model.trim="buildingForm.address" /></label>
              <button class="primary">保存宿舍楼</button>
            </form>
            <table><tbody><tr v-for="item in buildings" :key="item.id"><td>{{ item.name }}</td><td>{{ item.floors }} 层</td><td><button @click="editBuilding(item)">编辑</button></td></tr></tbody></table>
          </div>
          <div class="panel">
            <h2>宿舍房间</h2>
            <form class="form-grid" @submit.prevent="saveRoom">
              <label>宿舍楼<select v-model="roomForm.buildingId"><option v-for="building in buildings" :key="building.id" :value="building.id">{{ building.name }}</option></select></label>
              <label>楼层<input v-model.number="roomForm.floor" type="number" min="1" /></label>
              <label>房间号<input v-model.trim="roomForm.roomNo" required /></label>
              <label>入住上限<input v-model.number="roomForm.capacity" type="number" min="1" /></label>
              <label>已入住<input v-model.number="roomForm.occupied" type="number" min="0" /></label>
              <button class="primary">保存房间</button>
            </form>
            <table><tbody><tr v-for="item in roomRows" :key="item.id"><td>{{ item.buildingName }}</td><td>{{ item.roomNo }}</td><td>{{ item.occupied }}/{{ item.capacity }}</td><td><button @click="editRoom(item)">编辑</button></td></tr></tbody></table>
          </div>
        </section>

        <section v-if="activeTab === 'notices'" class="section-grid">
          <div class="panel">
            <h2>公告编辑</h2>
            <form class="form-grid" @submit.prevent="saveNotice">
              <label>标题<input v-model.trim="noticeForm.title" required /></label>
              <label>状态<select v-model="noticeForm.status"><option value="PUBLISHED">已发布</option><option value="DRAFT">草稿</option></select></label>
              <label class="full">内容<textarea v-model.trim="noticeForm.content" required rows="5"></textarea></label>
              <button class="primary">保存公告</button>
            </form>
          </div>
          <div class="panel">
            <h2>公告列表</h2>
            <article v-for="notice in notices" :key="notice.id" class="list-card">
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.content }}</p>
              <small>{{ notice.status }} · {{ notice.publishTime }}</small>
              <div class="row-actions"><button @click="editNotice(notice)">编辑</button><button @click="removeNotice(notice.id)">删除</button></div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'stats'" class="panel">
          <h2>数据统计</h2>
          <div v-if="stats" class="stats-grid">
            <div><strong>{{ stats.overview?.total || 0 }}</strong><span>报修总数</span></div>
            <div><strong>{{ stats.overview?.pending || 0 }}</strong><span>待处理工单</span></div>
            <div><strong>{{ stats.overview?.completed || 0 }}</strong><span>已完成工单</span></div>
            <div><strong>{{ Number(stats.overview?.avgHours || 0).toFixed(1) }}</strong><span>平均维修时长(小时)</span></div>
          </div>
          <div v-if="stats" class="section-grid">
            <div><h3>类型占比</h3><table><tbody><tr v-for="item in stats.category" :key="item.name"><td>{{ item.name }}</td><td>{{ item.value }}</td></tr></tbody></table></div>
            <div><h3>月度数量</h3><table><tbody><tr v-for="item in stats.monthly" :key="item.month"><td>{{ item.month }}</td><td>{{ item.value }}</td></tr></tbody></table></div>
            <div><h3>维修人员完成数</h3><table><tbody><tr v-for="item in stats.repairer" :key="item.repairerName"><td>{{ item.repairerName || '未分配' }}</td><td>{{ item.completed }}</td></tr></tbody></table></div>
          </div>
        </section>

        <section v-if="activeTab === 'users'" class="split">
          <div class="panel">
            <h2>用户管理</h2>
            <div class="filters"><input v-model.trim="filters.keyword" placeholder="账号/姓名/手机号" @keyup.enter="loadUsers(true)" /><select v-model="filters.roleCode" @change="loadUsers(true)"><option value="">全部角色</option><option v-for="role in roles" :key="role.id" :value="role.code">{{ role.name }}</option></select><button @click="loadUsers(true)">查询</button></div>
            <table><thead><tr><th>账号</th><th>姓名</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="row in users" :key="row.id"><td>{{ row.username }}</td><td>{{ row.name }}</td><td>{{ row.roleName }}</td><td>{{ row.status }}</td><td><button @click="editUser(row)">编辑</button><button @click="resetPassword(row)">重置密码</button><button @click="disableUser(row)">禁用</button><button @click="removeUser(row)">删除</button></td></tr></tbody></table>
          </div>
          <div class="panel">
            <h2>{{ userForm.id ? '编辑用户' : '新增用户' }}</h2>
            <form class="form-grid" @submit.prevent="saveUser">
              <label>账号<input v-model.trim="userForm.username" :disabled="Boolean(userForm.id)" required /></label>
              <label v-if="!userForm.id">初始密码<input v-model="userForm.password" required /></label>
              <label>姓名<input v-model.trim="userForm.name" required /></label>
              <label>角色<select v-model="userForm.roleId" required><option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</option></select></label>
              <label>手机号<input v-model.trim="userForm.phone" /></label>
              <label>学号<input v-model.trim="userForm.studentNo" /></label>
              <label>宿舍<select v-model="userForm.dormRoomId"><option value="">不绑定</option><option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.roomNo }}</option></select></label>
              <label>状态<select v-model="userForm.status"><option value="ACTIVE">启用</option><option value="DISABLED">禁用</option></select></label>
              <button class="primary">保存用户</button>
            </form>
          </div>
        </section>

        <section v-if="activeTab === 'logs'" class="panel">
          <h2>操作日志</h2>
          <div class="filters"><input v-model.trim="filters.keyword" placeholder="账号" @keyup.enter="loadLogs(true)" /><button @click="loadLogs(true)">查询</button></div>
          <table><thead><tr><th>时间</th><th>账号</th><th>角色</th><th>操作</th><th>详情</th><th>IP</th></tr></thead><tbody><tr v-for="row in logs" :key="row.id"><td>{{ row.createdAt }}</td><td>{{ row.username }}</td><td>{{ row.roleCode }}</td><td>{{ row.action }}</td><td>{{ row.detail }}</td><td>{{ row.ip }}</td></tr></tbody></table>
          <div class="pager"><button :disabled="logPage.current <= 1" @click="prevPage(logPage, loadLogs)">上一页</button><span>第 {{ logPage.current }} 页 / 共 {{ logPage.total }} 条</span><button :disabled="logPage.current * logPage.size >= logPage.total" @click="nextPage(logPage, loadLogs)">下一页</button></div>
        </section>
      </main>
    </div>
  `
};

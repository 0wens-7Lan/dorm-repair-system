import { createApp } from 'vue';
import router from './router/index.js';
import App from './App.js';

import { auth } from './store/auth.js';
auth.restore().finally(() => createApp(App).use(router).mount('#app'));

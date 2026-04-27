import { defineStore } from 'pinia';
import axios from 'axios';
import { logClickEvent } from '@/api';

// A new auth API client might be needed if we don't use the proxy for it
// For now, we assume the same proxy works.
const authApiClient = axios.create({
    baseURL: '/api/auth',
    headers: {
        'Content-Type': 'application/json'
    }
});

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('token') || null,
        user: JSON.parse(localStorage.getItem('user') || 'null'),
        isLoading: false,
        error: null,
    }),
    getters: {
        homeCityName: (state) => state.user?.homeCityName || '',
        isLoggedIn: (state) => !!state.token,
    },
    actions: {
        async register(registerRequest) {
            this.isLoading = true;
            this.error = null;
            try {
                const response = await authApiClient.post('/register', registerRequest);
                const token = response.data.token;
                this.token = token;
                localStorage.setItem('token', token);
                // 注册成功后获取用户信息
                await this.fetchProfile();
            } catch (err) {
                if (err.response && err.response.status === 409) {
                    this.error = err.response.data.message || '注册失败，信息冲突。';
                } else {
                    this.error = '注册失败，请检查您的输入或网络连接。';
                }
                console.error(err);
                throw err; // re-throw to be caught by the component
            } finally {
                this.isLoading = false;
            }
        },

        async login(loginRequest) {
            this.isLoading = true;
            this.error = null;
            try {
                const response = await authApiClient.post('/login', loginRequest);
                const token = response.data.token;
                this.token = token;
                localStorage.setItem('token', token);
                // 登录成功后获取用户信息
                await this.fetchProfile();
            } catch (err) {
                this.error = '登录失败，用户名或密码错误。';
                console.error(err);
                throw err;
            } finally {
                this.isLoading = false;
            }
        },

        async fetchProfile() {
            if (!this.token) return;
            try {
                const response = await authApiClient.get('/me');
                this.user = response.data;
                localStorage.setItem('user', JSON.stringify(response.data));
            } catch (err) {
                console.error('获取用户信息失败:', err);
            }
        },

        logout() {
            this.token = null;
            this.user = null;
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        },

        async logClick(cityId) {
            if (!this.token) {
                // 如果用户未登录，则不执行任何操作
                return;
            }
            try {
                // We need to get the logClickEvent function into this store
                // For now, let's just assume it's available globally or refactor later
                await logClickEvent(cityId, this.token);
            } catch (err) {
                // 在后台默默地失败，不打扰用户
                console.error("Failed to log click event:", err);
            }
        }
    },
});

import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：添加JWT令牌
api.interceptors.request.use(
  (config) => {
    const authData = localStorage.getItem('auth-storage');
    if (authData) {
      try {
        const { state } = JSON.parse(authData);
        if (state.user?.token) {
          config.headers.Authorization = `Bearer ${state.user.token}`;
        }
      } catch (error) {
        console.error('Failed to parse auth data:', error);
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：统一处理响应格式
api.interceptors.response.use(
  (response) => {
    // 如果后端返回统一格式 { success, data, message }
    if (response.data && typeof response.data === 'object' && 'success' in response.data) {
      if (response.data.success) {
        return { ...response, data: response.data.data };
      } else {
        return Promise.reject(new Error(response.data.message || '请求失败'));
      }
    }
    return response;
  },
  (error) => {
    if (error.response?.data?.message) {
      error.message = error.response.data.message;
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (username: string, password: string) =>
    api.post('/auth/login', { username, password }),

  logout: () => api.post('/auth/logout'),

  register: (username: string, password: string) =>
    api.post('/auth/register', { username, password, role: 'USER' }),

  getCurrentUser: () => api.get('/auth/me'),

  refreshToken: (refreshToken: string) =>
    api.post('/auth/refresh', { refreshToken }),

  changePassword: (oldPassword: string, newPassword: string) =>
    api.put('/auth/password', { oldPassword, newPassword }),

  updateProfile: (avatarUrl?: string) =>
    api.put('/auth/profile', { avatarUrl }),

  addUser: (username: string, password: string, role: string) => {
    return api.post('/users', {
      username,
      password,
      role
    });
  },
};

// Users API (管理员功能)
export const usersAPI = {
  getUsers: (page = 0, size = 10, role?: string, keyword?: string) => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (role) params.append('role', role);
    if (keyword) params.append('keyword', keyword);
    return api.get(`/users?${params}`);
  },

  createUser: (data: {
    username: string;
    password: string;
    role: string;
  }) => api.post('/users', data),

  updateUser: (id: number, data: {
    username?: string;
    role?: string;
  }) => api.put(`/users/${id}`, data),

  deleteUser: (id: number) => api.delete(`/users/${id}`),

  getUserById: (id: number) => api.get(`/users/${id}`),
};

// Jokes API  
export const jokesAPI = {
  getPublic: (page = 0, size = 5, sort = 'created') => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', sort);
    return api.get(`/jokes?${params}`);
  },

  getByTheme: (themeId: string, sort = 'created', page = 0, size = 5, showMyJokes = false, statusFilter = '') => {
    const params = new URLSearchParams();
    params.append('themeId', themeId);
    
    // 映射前端排序参数到后端API参数
    let sortField = 'createdAt';
    if (sort === 'score') {
      sortField = 'finalScore';
    } else if (sort === 'likes') {
      sortField = 'likeCount';
    }
    
    params.append('sort', sortField);
    params.append('order', 'desc');
    params.append('page', page.toString());
    params.append('size', size.toString());
    
    // 添加状态筛选参数
    if (statusFilter) {
      params.append('status', statusFilter);
    }
    
    // 如果需要筛选我投稿的笑话，使用不同的API端点
    if (showMyJokes) {
      return api.get(`/jokes/my?${params}`);
    }
    
    return api.get(`/jokes?${params}`);
  },

  getRandom: (themeId?: string, count = 5) => {
    const params = new URLSearchParams();
    if (themeId) params.append('themeId', themeId);
    params.append('count', count.toString());
    return api.get(`/jokes/random?${params}`);
  },

  search: (keyword: string, themeId?: string, page = 0, size = 5) => {
    const params = new URLSearchParams();
    params.append('keyword', keyword);
    if (themeId) params.append('themeId', themeId);
    params.append('page', page.toString());
    params.append('size', size.toString());
    return api.get(`/jokes/search?${params}`);
  },

  getMyJokes: (page = 0, size = 5, sort = 'createdAt', order = 'desc', status?: string, themeId?: string) => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', sort);
    params.append('order', order);
    if (status) params.append('status', status);
    if (themeId) params.append('themeId', themeId);
    return api.get(`/jokes/my?${params}`);
  },

  create: (data: {
    title?: string;
    content: string;
    themeId: number;
  }) => api.post('/jokes', data),

  update: (id: number, data: {
    title?: string;
    content: string;
  }) => api.put(`/jokes/my/${id}`, data),

  delete: (id: number) => {
    return api.delete(`/jokes/${id}`);
  },

  score: (id: number, score: number) => {
    return api.put(`/jokes/${id}/score`, { manualScore: score });
  },

  like: (id: number) => {
    return api.put(`/jokes/${id}/like`);
  },

  checkSimilarity: (content: string, themeId: number, threshold?: number) => {
    return api.post('/jokes/similarity-check', { content, themeId, threshold });
  },

  // 管理员功能
  getPending: (themeId: number, page = 0, size = 10) => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return api.get(`/jokes/themes/${themeId}/pending?${params}`);
  },

  approve: (id: number, manualScore?: number) => {
    const body = manualScore ? { manualScore } : {};
    return api.put(`/jokes/${id}/approve`, body);
  },

  reject: (id: number, reason?: string) => {
    return api.put(`/jokes/${id}/status`, {
      status: 'REJECTED',
      reason
    });
  },

  updateStatus: (id: number, status: string) => {
    return api.put(`/jokes/${id}/status`, { status });
  },
};

// Comments API (Theme Comments)
export const commentsAPI = {
  // 主题评论API
  getByTheme: (themeId: number, page = 0, size = 5) => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return api.get(`/themes/${themeId}/comments?${params}`);
  },

  createThemeComment: (themeId: number, data: {
    content: string;
    authorName?: string;
  }) => api.post(`/themes/${themeId}/comments`, data),



  deleteThemeComment: (themeId: number, commentId: number) => {
    return api.delete(`/themes/${themeId}/comments/${commentId}`);
  },

  // 笑话评论API
  getByJoke: (jokeId: number, page = 0, size = 5) => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return api.get(`/jokes/${jokeId}/comments?${params}`);
  },

  create: (data: {
    content: string;
    jokeId: number;
    authorName?: string;
  }) => api.post(`/jokes/${data.jokeId}/comments`, {
    content: data.content,
    authorName: data.authorName
  }),

  update: (jokeId: number, commentId: number, data: {
    content: string;
  }) => api.put(`/jokes/${jokeId}/comments/${commentId}`, data),

  delete: (jokeId: number, commentId: number) => {
    return api.delete(`/jokes/${jokeId}/comments/${commentId}`);
  },


};

// Themes API
export const themesAPI = {
  getThemes: () => api.get('/themes'),

  createTheme: (data: {
    name: string;
    prompt?: string;
    icon?: string;
  }) => api.post('/themes', data),

  updateTheme: (id: number, data: {
    name?: string;
    prompt?: string;
    icon?: string;
  }) => api.put(`/themes/${id}`, data),

  deleteTheme: (id: number) => {
    return api.delete(`/themes/${id}`);
  },

  // 权限管理
  grantPermission: (themeId: number, data: {
    userId: number;
    permissionType: string;
  }) => api.post(`/themes/${themeId}/permissions`, data),

  revokePermission: (themeId: number, userId: number) => {
    return api.delete(`/themes/${themeId}/permissions/${userId}`);
  },

  getThemeComments: (themeId: number, page = 0, size = 5) => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return api.get(`/themes/${themeId}/comments?${params}`);
  },

  addThemeComment: (themeId: number, data: {
    content: string;
    authorName?: string;
  }) => commentsAPI.createThemeComment(themeId, data),

  deleteThemeComment: (themeId: number, commentId: number) => 
    commentsAPI.deleteThemeComment(themeId, commentId),
};

// AI API
export const aiAPI = {
  // AI评分单个笑话
  scoreJoke: (data: {
    jokeId: number;
    apiKey: string;
    modelName: string;
    baseUrl: string;
  }) => api.post('/ai/score', data),

  // 保留旧的 generate 接口
  generateJokes: (data: {
    themeId: number;
    prompt: string;
    count: number;
    apiKey: string;
    modelName: string;
    baseUrl: string;
  }) => api.post('/ai/generate', data),
};

// Knowledge API
export const knowledgeAPI = {
  // 重建向量数据库
  rebuildVectorStore: (data: {
    themeId: number;
    apiKey?: string;
    modelName?: string;
    baseUrl?: string;
  }) => api.post('/knowledge/rebuild', data),

  // 查询向量状态
  getStatus: (themeId: number) => {
    const params = new URLSearchParams();
    params.append('themeId', themeId.toString());
    return api.get(`/knowledge/status?${params}`);
  },
};
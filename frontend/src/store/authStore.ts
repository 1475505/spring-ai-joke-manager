import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface ThemePermission {
  themeId: number;
  themeName: string;
  permissionLevel: 'write' | 'admin';
  createdAt: string;
}

interface User {
  id: number;
  username: string;
  role: 'ROOT' | 'USER';
  avatarUrl?: string;
  email?: string;
  token?: string;
  refreshToken?: string;
  themePermissions?: ThemePermission[];
}

interface AuthState {
  user: User | null;
  isLoggedIn: boolean;
  login: (user: User, accessToken: string, refreshToken?: string) => void;
  logout: () => void;
  updateUser: (user: Partial<User>) => void;
  updateTokens: (accessToken: string, refreshToken?: string) => void;
  hasPermission: (operation: string, themeId?: number) => boolean;
  isRoot: () => boolean;
  loadUserPermissions: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null as User | null,
      isLoggedIn: false,
      
      login: (user: User, accessToken: string, refreshToken?: string) => {
        const updatedUser = {
          ...user,
          token: accessToken,
          refreshToken: refreshToken
        };
        set({ user: updatedUser, isLoggedIn: true });
      },
      
      logout: () => {
        set({ user: null, isLoggedIn: false });
        // 清除localStorage中的数据
        localStorage.removeItem('auth-storage');
      },
      
      updateUser: (userData: Partial<User>) => {
        const { user } = get();
        if (user) {
          set({ user: { ...user, ...userData } });
        }
      },
      
      updateTokens: (accessToken: string, refreshToken?: string) => {
        const { user } = get();
        if (user) {
          const updatedUser = {
            ...user,
            token: accessToken,
            ...(refreshToken && { refreshToken })
          };
          set({ user: updatedUser });
        }
      },
      
      hasPermission: (operation: string, themeId?: number) => {
        const { user, isLoggedIn } = get();
        if (!isLoggedIn || !user) {
          // 未登录用户只能查看和评论
          return ['READ', 'COMMENT'].includes(operation);
        }
        
        // ROOT用户拥有所有权限
        if (user.role === 'ROOT') {
          return true;
        }
        
        // 检查主题级别的权限
        if (themeId && user.themePermissions) {
          const themePermission = user.themePermissions.find(p => p.themeId === themeId);
          if (themePermission) {
            const level = themePermission.permissionLevel;
            
            // admin权限：管理权限，包含所有操作
            if (level === 'admin') {
              return true;
            }
            
            // write权限：写入权限，包含创建、更新、删除、评分、评论、审批等操作
            if (level === 'write') {
              return ['READ', 'CREATE', 'ADD', 'COMMENT', 'SCORE', 'UPDATE', 'DELETE'].includes(operation);
            }
            
            // 如果有主题权限但级别不匹配，只允许基本操作
            return ['READ', 'COMMENT'].includes(operation);
          }
        }
        
        // 普通USER的权限（无主题权限时）
        if (user.role === 'USER') {
          return ['READ', 'CREATE', 'COMMENT', 'SCORE', 'UPDATE', 'DELETE'].includes(operation);
        }
        
        return false;
      },
      
      isRoot: () => {
        const { user } = get();
        return user?.role === 'ROOT';
      },
      
      loadUserPermissions: async () => {
        const { user } = get();
        if (!user || !user.token) {
          return;
        }
        
        try {
          const response = await fetch(`/api/users/${user.id}/permissions`, {
            headers: {
              'Authorization': `Bearer ${user.token}`,
              'Content-Type': 'application/json'
            }
          });
          
          if (response.ok) {
            const result = await response.json();
            if (result.success && result.data) {
              const updatedUser = {
                ...user,
                themePermissions: result.data.themePermissions
              };
              set({ user: updatedUser });
            }
          }
        } catch (error) {
          console.error('Failed to load user permissions:', error);
        }
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);

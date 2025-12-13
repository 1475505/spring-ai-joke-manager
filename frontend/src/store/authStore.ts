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
  hasAnyThemeAdminPermission: () => boolean;
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
        // 登录成功后自动获取用户权限
        setTimeout(() => {
          get().loadUserPermissions();
        }, 100);
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
          return ['read', 'comment'].includes(operation.toLowerCase());
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
            
            // write权限：写入权限，包含新增、审批、评分、删除笑话等操作
            if (level === 'write') {
              return ['read', 'write', 'create', 'update', 'delete', 'score', 'approve', 'comment'].includes(operation.toLowerCase());
            }
            
            // read权限：只读权限，只能查看和评论
            if (level === 'read') {
              return ['read', 'comment'].includes(operation.toLowerCase());
            }
          }
        }
        
        // 普通USER的权限（无主题权限时）- 可以浏览、投稿、评论
        if (user.role === 'USER') {
          return ['read', 'create', 'comment'].includes(operation.toLowerCase());
        }
        
        return false;
      },
      
      // 检查用户是否有任何主题的admin权限
      hasAnyThemeAdminPermission: () => {
        const { user } = get();
        if (!user) {
          return false;
        }
        // ROOT用户拥有所有权限
        if (user.role === 'ROOT') {
          return true;
        }
        return user.themePermissions?.some(p => p.permissionLevel === 'admin') || false;
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
          const response = await fetch(`http://localhost:8080/users/${user.id}/permissions`, {
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

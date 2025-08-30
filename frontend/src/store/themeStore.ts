import { create } from 'zustand';
import { themesAPI } from '../services/api';

interface Theme {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  createdBy: {
    id: number;
    username: string;
  };
  createdAt: string;
  updatedAt: string;
}

interface Comment {
  id: number;
  content: string;
  author?: {
    id: number;
    username: string;
    avatarUrl?: string;
  };
  authorName?: string;
  theme: {
    id: number;
    name: string;
  };
  statistics: {
    likeCount: number;
    replyCount: number;
  };
  canEdit: boolean;
  canDelete: boolean;
  createdAt: string;
}

interface ThemeState {
  currentTheme: string;
  currentThemeId: number | null;
  themes: Theme[];
  themeComments: Record<number, Comment[]>;
  loading: boolean;
  error: string | null;
  
  // Theme management
  setCurrentTheme: (theme: string) => void;
  loadThemes: () => Promise<void>;
  createTheme: (data: { name: string; description?: string; icon?: string }) => Promise<void>;
  
  // Comment management
  loadThemeComments: (themeId: number) => Promise<void>;
  addThemeComment: (themeId: number, content: string, authorName?: string) => Promise<void>;
  deleteThemeComment: (themeId: number, commentId: number) => Promise<void>;
  
  clearError: () => void;
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  currentTheme: '鸡煲笑话',
  currentThemeId: null,
  themes: [],
  themeComments: {},
  loading: false,
  error: null,

  setCurrentTheme: (theme) => {
    set({ currentTheme: theme });
    const themeObj = get().themes.find(t => t.name === theme);
    if (themeObj) {
      set({ currentThemeId: themeObj.id });
      get().loadThemeComments(themeObj.id);
    }
  },

  loadThemes: async () => {
    try {
      set({ loading: true, error: null });
      const response = await themesAPI.getThemes();
      const themes = Array.isArray(response.data) ? response.data : response.data.content || [];
      set({ themes });
      
      // 设置默认主题ID
      const currentTheme = get().currentTheme;
      const themeObj = themes.find((t: Theme) => t.name === currentTheme);
      if (themeObj) {
        set({ currentThemeId: themeObj.id });
      }
    } catch (error: any) {
      set({ error: error.message || 'Failed to load themes' });
      console.error(error);
    } finally {
      set({ loading: false });
    }
  },

  createTheme: async (data) => {
    try {
      set({ loading: true, error: null });
      
      await themesAPI.createTheme(data);
      // 重新加载主题列表
      await get().loadThemes();
    } catch (error: any) {
      set({ error: error.message || 'Failed to create theme' });
      console.error(error);
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  loadThemeComments: async (themeId) => {
    try {
      set({ loading: true, error: null });
      const response = await themesAPI.getThemeComments(themeId);
      const comments = Array.isArray(response.data) ? response.data : response.data.content || [];
      set((state) => ({
        themeComments: {
          ...state.themeComments,
          [themeId]: comments,
        },
      }));
    } catch (error: any) {
      set({ error: error.message || 'Failed to load theme comments' });
      console.error(error);
    } finally {
      set({ loading: false });
    }
  },

  addThemeComment: async (themeId, content, authorName) => {
    try {
      set({ loading: true, error: null });
      
      const commentData: any = {
        content
      };
      
      // 只有匿名用户才传递authorName
      if (authorName) {
        commentData.authorName = authorName;
      }
      
      await themesAPI.addThemeComment(themeId, commentData);
      // 重新加载评论
      await get().loadThemeComments(themeId);
    } catch (error: any) {
      set({ error: error.message || 'Failed to add comment' });
      console.error(error);
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  deleteThemeComment: async (themeId, commentId) => {
    try {
      set({ loading: true, error: null });
      
      await themesAPI.deleteThemeComment(themeId, commentId);
      // 重新加载评论
      await get().loadThemeComments(themeId);
    } catch (error: any) {
      set({ error: error.message || 'Failed to delete comment' });
      console.error(error);
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  clearError: () => set({ error: null }),
}));

import { ThemeComment, CreateThemeCommentRequest } from '../types/themeComment';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/theme-comments';

export const themeCommentApi = {
  async getByTheme(theme: string): Promise<ThemeComment[]> {
    try {
      const response = await axios.get(`${API_BASE_URL}/theme/${encodeURIComponent(theme)}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching theme comments:', error);
      throw error;
    }
  },

  async create(comment: CreateThemeCommentRequest): Promise<ThemeComment> {
    try {
      const response = await axios.post(API_BASE_URL, comment);
      return response.data;
    } catch (error) {
      console.error('Error creating theme comment:', error);
      throw error;
    }
  },

  async delete(id: number): Promise<void> {
    try {
      await axios.delete(`${API_BASE_URL}/${id}`);
    } catch (error) {
      console.error('Error deleting theme comment:', error);
      throw error;
    }
  }
};

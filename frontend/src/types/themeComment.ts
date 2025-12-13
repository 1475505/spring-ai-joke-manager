export interface ThemeComment {
  id: number;
  content: string;
  theme: string;
  authorName: string;
  authorAvatar?: string;
  createdAt: string;
}

export interface CreateThemeCommentRequest {
  content: string;
  theme: string;
  authorName?: string;
}

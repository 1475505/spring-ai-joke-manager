import React, { useState, useEffect } from 'react';
import { List, Input, Button, Avatar, message, Typography, Card } from 'antd';
import { UserOutlined, SendOutlined, DeleteOutlined } from '@ant-design/icons';
import { useThemeStore } from '../../store/themeStore';
import { useAuthStore } from '../../store/authStore';

const { Text } = Typography;

interface ThemeCommentsProps {
  theme: string;
}

export const ThemeComments: React.FC<ThemeCommentsProps> = ({ theme: _ }) => {
  const [commentText, setCommentText] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [authorName, setAuthorName] = useState('');
  const { user } = useAuthStore();

  // 当用户状态改变时，更新默认昵称
  useEffect(() => {
    if (user?.username) {
      setAuthorName(user.username);
    } else {
      setAuthorName('');
    }
  }, [user]);
  const { 
    currentThemeId,
    themeComments, 
    loading, 
    error,
    loadThemes,
    loadThemeComments, 
    addThemeComment, 
    deleteThemeComment,
    clearError
  } = useThemeStore();

  // 获取当前主题的评论
  const comments = currentThemeId ? themeComments[currentThemeId] || [] : [];

  useEffect(() => {
    // 先加载主题列表以获取主题ID
    loadThemes();
  }, [loadThemes]);

  useEffect(() => {
    // 当有主题ID时加载评论
    if (currentThemeId) {
      loadThemeComments(currentThemeId);
    }
  }, [currentThemeId, loadThemeComments]);

  useEffect(() => {
    if (error) {
      message.error(error);
      clearError();
    }
  }, [error, clearError]);

  const handleSubmit = async () => {
    if (!commentText.trim() || !currentThemeId) return;
    
    if (commentText.trim().length < 3) {
      message.error('评论内容至少需要3个字符');
      return;
    }
    
    if (commentText.trim().length > 500) {
      message.error('评论内容不能超过500个字符');
      return;
    }
    
    setIsSubmitting(true);
    try {
      // 传递用户输入的昵称，如果为空则由后端处理默认值
      const authorNameToUse = authorName.trim() || (!user ? '匿名用户' : undefined);
      
      await addThemeComment(currentThemeId, commentText, authorNameToUse);
      message.success('评论发布成功！');
      setCommentText('');
    } catch (error) {
      message.error('评论发布失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    if (!currentThemeId) return;
    
    try {
      await deleteThemeComment(currentThemeId, commentId);
      message.success('评论删除成功');
    } catch (error) {
      console.error('Error deleting comment:', error);
      message.error('删除评论失败');
    }
  };

  return (
    <div className="theme-comments">
      <Card title="发表评论" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', gap: 12 }}>
          <Avatar 
            icon={<UserOutlined />}
            style={{ backgroundColor: '#1890ff' }}
          >
            {(authorName || user?.username)?.charAt(0)?.toUpperCase() || 'A'}
          </Avatar>
          <div style={{ flex: 1 }}>
            <Input
              placeholder={user ? "昵称（默认为用户名）" : "请输入昵称"}
              value={authorName}
              onChange={(e) => setAuthorName(e.target.value)}
              style={{ marginBottom: 8 }}
            />
            <Input.TextArea
              rows={3}
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="对这个主题发表评论..."
              style={{ marginBottom: 8 }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleSubmit}
                loading={isSubmitting}
                disabled={!commentText.trim()}
              >
                {isSubmitting ? '发布中...' : '发布评论'}
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <div className="comment-list">
        <List
          dataSource={comments}
          loading={loading}
          locale={{ emptyText: '还没有评论，来发表第一条评论吧！' }}
          renderItem={(item) => (
            <List.Item>
              <Card size="small" style={{ width: '100%' }}>
                <div style={{ display: 'flex', gap: 12 }}>
                  <Avatar icon={<UserOutlined />}>
                    {item.authorName?.charAt(0)?.toUpperCase() || 'A'}
                  </Avatar>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                      <Text strong>{item.authorName || '匿名用户'}</Text>
                      <Text type="secondary" style={{ fontSize: '0.8em' }}>
                        {new Date(item.createdAt).toLocaleString()}
                      </Text>
                    </div>
                    <p style={{ marginBottom: 0 }}>{item.content}</p>
                    {user?.role === 'ROOT' && (
                      <div style={{ textAlign: 'right', marginTop: 8 }}>
                        <Button 
                          type="text" 
                          danger 
                          icon={<DeleteOutlined />}
                          onClick={() => handleDelete(item.id)}
                          size="small"
                        >
                          删除
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </Card>
            </List.Item>
          )}
        />
      </div>
    </div>
  );
};

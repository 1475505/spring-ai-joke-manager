import React, { useState, useEffect } from 'react';
import { List, Input, Button, Space, message, Popconfirm, Typography, Grid } from 'antd';
import { DeleteOutlined, SendOutlined } from '@ant-design/icons';
import { commentsAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { TextArea } = Input;
const { Text } = Typography;
const { useBreakpoint } = Grid;

interface Comment {
  id: number;
  content: string;
  createdAt: string;
  user?: {
    id: number;
    username: string;
  };
  authorName?: string;
}

interface CommentListProps {
  jokeId: number;
}

const CommentList: React.FC<CommentListProps> = ({ jokeId }) => {
  const { user } = useAuthStore();
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [authorName, setAuthorName] = useState('');
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  // 当用户状态改变时，更新默认昵称
  useEffect(() => {
    if (user?.username) {
      setAuthorName(user.username);
    } else {
      setAuthorName('');
    }
  }, [user]);

  const loadComments = async () => {
    setLoading(true);
    try {
      const response = await commentsAPI.getByJoke(jokeId);
      const comments = Array.isArray(response.data) ? response.data : response.data?.content || [];
      setComments(comments);
    } catch (error) {
      message.error('加载评论失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitComment = async () => {
    if (!newComment.trim()) {
      message.error('请输入评论内容');
      return;
    }
    
    if (newComment.trim().length < 3) {
      message.error('评论内容至少需要3个字符');
      return;
    }
    
    if (newComment.trim().length > 500) {
      message.error('评论内容不能超过500个字符');
      return;
    }

    setSubmitting(true);
    try {
      const commentData: any = {
        content: newComment,
        jokeId: jokeId,
      };

      // 传递用户输入的昵称，如果为空则由后端处理默认值
      if (authorName.trim()) {
        commentData.authorName = authorName.trim();
      } else if (!user) {
        commentData.authorName = '匿名用户';
      }
      // 登录用户如果没有输入昵称，后端会使用用户名

      await commentsAPI.create(commentData);
      message.success('评论发布成功');
      setNewComment('');
      if (!user) {
        setAuthorName('');
      }
      loadComments();
    } catch (error) {
      message.error('评论发布失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    try {
      await commentsAPI.delete(jokeId, commentId);
      message.success('评论删除成功');
      loadComments();
    } catch (error) {
      message.error('评论删除失败');
    }
  };

  const canDeleteComment = (comment: Comment) => {
    if (!user) return false;
    if (user.role === 'ROOT') return true;
    return comment.user?.id === user.id;
  };

  useEffect(() => {
    loadComments();
  }, [jokeId]);

  return (
    <div>
      {/* 评论列表 */}
      <List
        loading={loading}
        dataSource={comments}
        renderItem={(comment) => (
          <List.Item
            actions={[
              ...(canDeleteComment(comment) ? [
                <Popconfirm
                  key="delete"
                  title="确定要删除这条评论吗？"
                  onConfirm={() => handleDeleteComment(comment.id)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    type="text"
                    icon={<DeleteOutlined />}
                    size="small"
                    danger
                  />
                </Popconfirm>
              ] : [])
            ]}
          >
            <List.Item.Meta
              title={
                <Space 
                  direction={isMobile ? 'vertical' : 'horizontal'}
                  size={isMobile ? 'small' : 'middle'}
                  style={{ alignItems: isMobile ? 'flex-start' : 'center' }}
                >
                  <Text strong style={{ fontSize: isMobile ? 13 : 14 }}>
                    {comment.user?.username || comment.authorName || '匿名用户'}
                  </Text>
                  <Text type="secondary" style={{ fontSize: isMobile ? 11 : 12 }}>
                    {new Date(comment.createdAt).toLocaleString()}
                  </Text>
                </Space>
              }
              description={
                <div style={{ fontSize: isMobile ? 13 : 14 }}>
                  {comment.content}
                </div>
              }
            />
          </List.Item>
        )}
        locale={{ emptyText: '暂无评论，快来抢沙发吧！' }}
      />

      {/* 发表评论 */}
      <div style={{ 
        marginTop: isMobile ? 12 : 16, 
        padding: isMobile ? 12 : 16, 
        backgroundColor: '#fafafa', 
        borderRadius: 8 
      }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            placeholder={user ? "昵称（默认为用户名）" : "请输入您的昵称（可选）"}
            value={authorName}
            onChange={(e) => setAuthorName(e.target.value)}
            style={{ marginBottom: 8 }}
            size={isMobile ? 'small' : 'middle'}
          />
          <TextArea
            placeholder={user ? `以 ${user.username} 身份发表评论...` : '发表评论...'}
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            rows={isMobile ? 2 : 3}
            maxLength={500}
            showCount
            size={isMobile ? 'small' : 'middle'}
          />
          <div style={{ textAlign: 'right' }}>
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSubmitComment}
              loading={submitting}
              disabled={!newComment.trim()}
              size={isMobile ? 'small' : 'middle'}
            >
              {isMobile ? '发表' : '发表评论'}
            </Button>
          </div>
        </Space>
      </div>
    </div>
  );
};

export default CommentList;

import React, { useState } from 'react';
import { Card, Button, Space, Typography, Modal, Form, Input, Rate, message, Grid, Popconfirm, Tooltip } from 'antd';
import { EditOutlined, DeleteOutlined, StarOutlined, CommentOutlined, LikeOutlined, LikeFilled, InfoCircleOutlined, SettingOutlined, RobotOutlined } from '@ant-design/icons';
import { jokesAPI, aiAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';
import CommentList from './CommentList';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;
const { useBreakpoint } = Grid;

interface Joke {
  id: number;
  title: string;
  content: string;
  theme: {
    id: number;
    name: string;
    icon: string;
  };
  scores: {
    aiScore: number;
    manualScore: number | null;
    finalScore: number;
    qualityLevel: string;
  };
  statistics: {
    viewCount: number;
    likeCount: number;
  };
  status: string;
  isAiGenerate: boolean;
  author: {
    id: number;
    username: string;
    avatarUrl: string;
  };
  createdAt: string;
  updatedAt: string;
}

interface JokeCardProps {
  joke: Joke;
  onUpdate: () => void;
  showStatus?: boolean;
}

const JokeCard: React.FC<JokeCardProps> = ({ joke, onUpdate, showStatus = false }) => {
  const { user, hasPermission } = useAuthStore();
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [commentsVisible, setCommentsVisible] = useState(false);
  const [form] = Form.useForm();
  const screens = useBreakpoint();
  const isMobile = !screens.md;
  const [scoreModalVisible, setScoreModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [scoreForm] = Form.useForm();
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(joke.statistics.likeCount);
  const [statusModalVisible, setStatusModalVisible] = useState(false);
  const [aiScoreLoading, setAiScoreLoading] = useState(false);

  // 将0-10分映射到0-5星，提高精度
  const getStarRating = (score: number) => {
    // 使用更精确的映射：0-10分线性映射到0-5星，支持半星显示
    const starValue = Math.min(5, Math.max(0, score * 0.5));
    // 四舍五入到最近的0.5（Rate组件只支持半星精度）
    return starValue * 2 / 2;
  };

  const handleEdit = async (values: any) => {
    if (!user || !hasPermission('write', joke.theme.id)) {
      message.error('权限不足');
      return;
    }

    setLoading(true);
    try {
      await jokesAPI.update(joke.id, {
        title: values.title,
        content: values.content,
      });
      message.success('更新成功');
      setEditModalVisible(false);
      onUpdate();
    } catch (error) {
      message.error('更新失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!user) {
      message.error('请先登录');
      return;
    }

    try {
      await jokesAPI.delete(joke.id);
      message.success('删除成功');
      onUpdate();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleScore = async (values: { score: number }) => {
    if (!user || !hasPermission('score', joke.theme.id)) {
      message.error('权限不足，只有具有write权限的用户可以评分');
      return;
    }

    setLoading(true);
    try {
      // 保存原始评分信息
      const originalScore = joke.scores.manualScore;
      
      await jokesAPI.score(joke.id, values.score);
      
      // 显示详细的评分更新信息
      const successMessage = originalScore 
        ? `评分更新成功！原评分: ${originalScore.toFixed(1)} → 新评分: ${values.score}`
        : `评分设置成功！新评分: ${values.score}（原评分: ${joke.scores.aiScore?.toFixed(1) || 'N/A'}）`;
      
      message.success(successMessage, 4); // 显示4秒
      setScoreModalVisible(false);
      onUpdate();
    } catch (error) {
      message.error('评分失败');
    } finally {
      setLoading(false);
    }
  };

  const handleLike = async () => {
    if (liked) {
      message.info('已经点过赞了');
      return;
    }
    
    setLoading(true);
    try {
      await jokesAPI.like(joke.id);
      setLiked(true);
      setLikeCount(prev => prev + 1);
      message.success('点赞成功');
    } catch (error) {
      message.error('点赞失败');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusUpdate = async (status: string) => {
    if (!user || !hasPermission('write', joke.theme.id)) {
      message.error('权限不足');
      return;
    }

    setLoading(true);
    try {
      await jokesAPI.updateStatus(joke.id, status);
      const statusText = {
        'PENDING': '审核中',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'HIDDEN': '已隐藏'
      }[status] || status;
      message.success(`状态已更新为：${statusText}`);
      setStatusModalVisible(false);
      onUpdate();
    } catch (error) {
      message.error('状态更新失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAIScore = async () => {
    if (!user || !hasPermission('admin', joke.theme.id)) {
      message.error('权限不足，只有主题管理员可以使用AI评分');
      return;
    }

    // 从localStorage读取OpenAI配置
    let config;
    try {
      const configStr = localStorage.getItem('openai_config');
      if (!configStr) {
        message.error('请先在右上角配置OpenAI设置');
        return;
      }
      config = JSON.parse(configStr);
      if (!config.apiKey || !config.model || !config.baseUrl) {
        message.error('OpenAI配置不完整，请重新配置');
        return;
      }
    } catch (error) {
      message.error('读取OpenAI配置失败');
      return;
    }

    setAiScoreLoading(true);
    try {
      const response = await aiAPI.scoreJoke({
        jokeId: joke.id,
        apiKey: config.apiKey,
        modelName: config.model,
        baseUrl: config.baseUrl
      });
      
      // 显示详细的AI评分结果弹出框
      const aiScore = response.data?.aiScore;
      if (aiScore) {
        Modal.success({
          title: 'AI评分完成',
          content: (
            <div>
              <p><strong>评分：</strong>{aiScore.score}分</p>
              <p><strong>反馈：</strong>{aiScore.feedback}</p>
            </div>
          ),
          okText: '确认'
        });
      } else {
        message.success('AI评分完成');
      }
      
      onUpdate();
    } catch (error) {
      message.error('AI评分失败');
    } finally {
      setAiScoreLoading(false);
    }
  };


  return (
    <>
      <Card
        title={
          <div style={{ 
            fontSize: isMobile ? 14 : 16, 
            fontWeight: 500,
            lineHeight: 1.4
          }}>
            {joke.title}
          </div>
        }
        extra={
          <Space size="small">
            <Rate disabled value={getStarRating(joke.scores.finalScore)} allowHalf />
            <Tooltip title={
              <div>
                <div>最终评分: {joke.scores.finalScore.toFixed(1)}</div>
                {joke.scores.manualScore && (
                  <div>人工评分: {joke.scores.manualScore.toFixed(1)}</div>
                )}
                {joke.scores.aiScore && (
                  <div>AI评分: {joke.scores.aiScore.toFixed(1)}</div>
                )}
              </div>
            }>
              <Text type="secondary" style={{ fontSize: 12, cursor: 'pointer' }}>
                {joke.scores.finalScore.toFixed(1)}
              </Text>
            </Tooltip>
          </Space>
        }

        size="small"
        bodyStyle={{ padding: 0, display: 'flex', flexDirection: 'column' }}
        headStyle={{ padding: isMobile ? '8px 12px' : '12px 16px' }}
        style={{ minHeight: '200px' }}
      >
        {/* 内容区域 */}
        <div style={{ 
          flex: 1, 
          padding: isMobile ? '12px' : '16px',
          paddingBottom: 0
        }}>
          <Paragraph 
            style={{ 
              fontSize: isMobile ? 14 : 15, 
              marginBottom: isMobile ? 8 : 12,
              lineHeight: 1.5
            }}
          >
            {joke.content}
          </Paragraph>
          <div style={{ 
            marginTop: isMobile ? 8 : 12, 
            color: '#666', 
            fontSize: isMobile ? 11 : 12 
          }}>
            <Space 
              split={<span style={{ color: '#d9d9d9' }}>•</span>} 
              size={4}
              direction={isMobile ? 'vertical' : 'horizontal'}
              style={{ width: '100%' }}
            >
              <span>主题: {joke.theme.name}</span>
              <span>创建者: {joke.author?.username || '系统'}</span>
              <span>{new Date(joke.createdAt).toLocaleDateString()}</span>
              {showStatus && (
                <span style={{ 
                  color: joke.status === 'APPROVED' ? '#52c41a' : 
                         joke.status === 'REJECTED' ? '#ff4d4f' : 
                         joke.status === 'PENDING' ? '#faad14' : '#666'
                }}>
                  状态: {joke.status === 'APPROVED' ? '已通过' : 
                         joke.status === 'REJECTED' ? '已拒绝' : 
                         joke.status === 'PENDING' ? '待审核' : joke.status}
                </span>
              )}
            </Space>
           </div>
           
           {commentsVisible && (
             <div style={{ 
               marginTop: isMobile ? 12 : 16, 
               borderTop: '1px solid #f0f0f0', 
               paddingTop: isMobile ? 12 : 16 
             }}>
               <CommentList jokeId={joke.id} />
             </div>
           )}
         </div>

         {/* 按钮区域 - 固定在底部 */}
         <div style={{ 
           borderTop: '1px solid #f0f0f0', 
           padding: isMobile ? '6px 8px' : '8px 12px',
           marginTop: 'auto',
           backgroundColor: '#fafafa'
         }}>
          {/* 第一行：无需登录都有的按钮 */}
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-around', 
            alignItems: 'center',
            marginBottom: '6px',
            flexWrap: 'nowrap'
          }}>
            <Button
              type="text"
              icon={liked ? <LikeFilled style={{ color: '#1890ff' }} /> : <LikeOutlined />}
              onClick={handleLike}
              size={isMobile ? 'small' : 'middle'}
              loading={loading}
              style={{ 
                minWidth: isMobile ? '60px' : '80px',
                fontSize: isMobile ? '12px' : '14px',
                padding: isMobile ? '2px 6px' : '4px 8px'
              }}
            >
              {isMobile ? likeCount : `点赞 ${likeCount}`}
            </Button>
            <Button
              type="text"
              icon={<CommentOutlined />}
              onClick={() => setCommentsVisible(!commentsVisible)}
              size={isMobile ? 'small' : 'middle'}
              style={{ 
                minWidth: isMobile ? '60px' : '80px',
                fontSize: isMobile ? '12px' : '14px',
                padding: isMobile ? '2px 6px' : '4px 8px'
              }}
            >
              {isMobile ? '评论' : (commentsVisible ? '收起评论' : '评论')}
            </Button>
            <Button
              type="text"
              icon={<RobotOutlined />}
              onClick={handleAIScore}
              loading={aiScoreLoading}
              size={isMobile ? 'small' : 'middle'}
              style={{ 
                minWidth: isMobile ? '60px' : '80px',
                fontSize: isMobile ? '12px' : '14px',
                padding: isMobile ? '2px 6px' : '4px 8px'
              }}
            >
              {isMobile ? 'AI' : 'AI评分'}
            </Button>
          </div>
          
          {/* 第二行：仅有admin权限才出现的按钮 */}
          {user && (hasPermission('score', joke.theme.id) || hasPermission('write', joke.theme.id)) && (
            <div style={{ 
              display: 'flex', 
              justifyContent: 'center', 
              alignItems: 'center',
              gap: isMobile ? '4px' : '8px',
              flexWrap: 'nowrap'
            }}>
              {user && hasPermission('score', joke.theme.id) && (
                <Button
                  type="text"
                  icon={<StarOutlined />}
                  onClick={() => setScoreModalVisible(true)}
                  size={isMobile ? 'small' : 'middle'}
                  style={{ 
                    minWidth: isMobile ? '50px' : '70px',
                    fontSize: isMobile ? '12px' : '14px',
                    padding: isMobile ? '2px 4px' : '4px 6px'
                  }}
                >
                  {isMobile ? '评分' : '评分'}
                </Button>
              )}
              {hasPermission('write', joke.theme.id) && joke.status !== 'REJECTED' && (
                <Button
                  type="text"
                  icon={<EditOutlined />}
                  onClick={() => {
                    form.setFieldsValue({
                      title: joke.title,
                      content: joke.content
                    });
                    setEditModalVisible(true);
                  }}
                  size={isMobile ? 'small' : 'middle'}
                  style={{ 
                    minWidth: isMobile ? '50px' : '70px',
                    fontSize: isMobile ? '12px' : '14px',
                    padding: isMobile ? '2px 4px' : '4px 6px'
                  }}
                >
                  {isMobile ? '编辑' : '编辑'}
                </Button>
              )}
              {hasPermission('write', joke.theme.id) && (
                <Button
                  type="text"
                  icon={<SettingOutlined />}
                  onClick={() => setStatusModalVisible(true)}
                  size={isMobile ? 'small' : 'middle'}
                  style={{ 
                    minWidth: isMobile ? '50px' : '70px',
                    fontSize: isMobile ? '12px' : '14px',
                    padding: isMobile ? '2px 4px' : '4px 6px'
                  }}
                >
                  {isMobile ? '管理' : '状态管理'}
                </Button>
              )}
            </div>
          )}
        </div>
      </Card>

      {/* 编辑模态框 */}
      <Modal
        title="编辑笑话"
        open={editModalVisible}
        onCancel={() => setEditModalVisible(false)}
        footer={null}
      >
        <Form
          form={form}
          onFinish={handleEdit}
          layout="vertical"
        >
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            <TextArea rows={4} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                保存
              </Button>
              <Button onClick={() => setEditModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 评分模态框 */}
      <Modal
        title="笑话评分"
        open={scoreModalVisible}
        onCancel={() => setScoreModalVisible(false)}
        footer={null}
      >
        <Form
          form={scoreForm}
          onFinish={handleScore}
          layout="vertical"
        >
          <Form.Item
            name="score"
            label={
              <Space>
                <span>评分 (0-10分，可填写一位小数)</span>
                <Tooltip title={
                  <div>
                    <div style={{ marginBottom: 8, fontWeight: 'bold' }}>评分标准：</div>
                    <div>10分 - 非常优秀的笑话，有很强的创意和笑点</div>
                    <div>8分 - 优秀的笑话，有明显笑点</div>
                    <div>6分 - 一般的笑话，无法让用户笑起来</div>
                    <div>4分 - 偏离主题，或没有笑点的笑话</div>
                    <div>2分 - 和主题有一部分相关性，但不算笑话</div>
                    <div>0分 - 无关内容，不是笑话</div>
                  </div>
                }>
                  <InfoCircleOutlined style={{ color: '#1890ff', cursor: 'pointer' }} />
                </Tooltip>
              </Space>
            }
            rules={[
              { required: true, message: '请输入评分（如9.1）' },
              { 
                validator: (_, value) => {
                  const num = parseFloat(value);
                  if (isNaN(num)) {
                    return Promise.reject(new Error('请输入有效的数字'));
                  }
                  if (num < 0 || num > 10) {
                    return Promise.reject(new Error('评分必须在0-10之间'));
                  }
                  return Promise.resolve();
                }
              }
            ]}
          >
            <Input type="number" step="0.1" min="0" max="10" placeholder="请输入0-10之间的数字，如8.5" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                提交评分
              </Button>
              <Button onClick={() => setScoreModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 状态管理模态框 */}
      <Modal
        title="笑话状态管理"
        open={statusModalVisible}
        onCancel={() => setStatusModalVisible(false)}
        footer={null}
      >
        <div style={{ marginBottom: 16 }}>
          <Text strong>当前状态：</Text>
          <span style={{ 
            color: joke.status === 'APPROVED' ? '#52c41a' : 
                   joke.status === 'REJECTED' ? '#ff4d4f' : 
                   joke.status === 'PENDING' ? '#faad14' : 
                   joke.status === 'HIDDEN' ? '#666' : '#666'
          }}>
            {joke.status === 'APPROVED' ? '已通过' : 
             joke.status === 'REJECTED' ? '已拒绝' : 
             joke.status === 'PENDING' ? '审核中' : 
             joke.status === 'HIDDEN' ? '已隐藏' : joke.status}
          </span>
        </div>
        
        <div style={{ marginBottom: 16 }}>
          <Text strong>更改状态：</Text>
        </div>
        
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button 
            block 
            onClick={() => handleStatusUpdate('PENDING')}
            disabled={joke.status === 'PENDING'}
            loading={loading}
          >
            设为审核中
          </Button>
          <Button 
            block 
            type="primary"
            onClick={() => handleStatusUpdate('APPROVED')}
            disabled={joke.status === 'APPROVED'}
            loading={loading}
          >
            设为已通过
          </Button>
          <Button 
            block 
            onClick={() => handleStatusUpdate('REJECTED')}
            disabled={joke.status === 'REJECTED'}
            loading={loading}
          >
            设为已拒绝
          </Button>
          <Button 
            block 
            onClick={() => handleStatusUpdate('HIDDEN')}
            disabled={joke.status === 'HIDDEN'}
            loading={loading}
          >
            设为已隐藏
          </Button>
          
          <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 16, marginTop: 16 }}>
            <Text strong style={{ color: '#ff4d4f' }}>危险操作：</Text>
            <Popconfirm
              title="确定要删除这个笑话吗？此操作不可恢复！"
              onConfirm={handleDelete}
              okText="确定删除"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button 
                block 
                danger 
                icon={<DeleteOutlined />}
                style={{ marginTop: 8 }}
              >
                永久删除笑话
              </Button>
            </Popconfirm>
          </div>
        </Space>
      </Modal>

    </>
  );
};

export default JokeCard;

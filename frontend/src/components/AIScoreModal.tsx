import React, { useState, useEffect } from 'react';
import { Modal, Form, Button, message, Space, Card, Typography, Spin, Divider } from 'antd';
import { RobotOutlined } from '@ant-design/icons';
import { aiAPI, jokesAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { Text, Title } = Typography;
// 移除不必要的 Option 解构，使用 options API 或直接 Select 组件
// const { Option } = Select;

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

interface AIScoreModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  themeId?: number;
}

const AIScoreModal: React.FC<AIScoreModalProps> = ({ visible, onClose, onSuccess, themeId }) => {
  const { user, hasPermission } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [jokes, setJokes] = useState<Joke[]>([]);
  // 批量评分功能已移除
  const [form] = Form.useForm();

  // 检查权限
  const canUseAIScore = user && themeId && hasPermission('admin', themeId);

  // 从localStorage读取OpenAI配置
  const getOpenAIConfig = () => {
    try {
      const configStr = localStorage.getItem('openai_config');
      if (!configStr) {
        return null;
      }
      const config = JSON.parse(configStr);
      if (!config.apiKey || !config.model || !config.baseUrl) {
        return null;
      }
      return config;
    } catch (error) {
      console.error('读取OpenAI配置失败:', error);
      return null;
    }
  };

  // 加载笑话列表
  const loadJokes = async () => {
    if (!themeId) return;
    
    setLoading(true);
    try {
      const response = await jokesAPI.getByTheme(themeId.toString(), 'created', 0, 20);
      setJokes(response.data.content || []);
    } catch (error) {
      message.error('加载笑话列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && themeId) {
      loadJokes();
    }
  }, [visible, themeId]);

  // 单个笑话评分
  const handleSingleScore = async (jokeId: number) => {
    const config = getOpenAIConfig();
    if (!config) {
      message.error('请先在右上角配置OpenAI设置');
      return;
    }

    if (!canUseAIScore) {
      message.error('权限不足，只有主题管理员可以使用AI评分');
      return;
    }

    setLoading(true);
    try {
      const response = await aiAPI.scoreJoke({
        jokeId,
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
      
      loadJokes(); // 重新加载笑话列表
    } catch (error) {
      message.error('AI评分失败');
    } finally {
      setLoading(false);
    }
  };

  // 批量评分功能已移除
  // const handleBatchScore = async () => {};
  // const handleSelectLatest = () => {};

  if (!canUseAIScore) {
    return (
      <Modal
        title="AI评分"
        open={visible}
        onCancel={onClose}
        footer={null}
      >
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="secondary">权限不足，只有主题管理员可以使用AI评分功能</Text>
        </div>
      </Modal>
    );
  }

  const config = getOpenAIConfig();
  if (!config) {
    return (
      <Modal
        title="AI评分"
        open={visible}
        onCancel={onClose}
        footer={null}
      >
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="secondary">请先在右上角配置OpenAI设置</Text>
        </div>
      </Modal>
    );
  }

  return (
    <Modal
      title={<><RobotOutlined /> AI评分</>}
      open={visible}
      onCancel={onClose}
      width={800}
      footer={null}
    >
      <Spin spinning={loading}>
        <div style={{ marginBottom: 16 }}>
          <Text strong>当前配置：</Text>
          <Text type="secondary"> {config.model} @ {config.baseUrl}</Text>
        </div>

        {/* 批量操作功能已移除 */}

        <Divider>笑话列表</Divider>

        <div style={{ maxHeight: 400, overflowY: 'auto' }}>
          {jokes.map(joke => (
            <Card
              key={joke.id}
              size="small"
              style={{ marginBottom: 8 }}
              bodyStyle={{ padding: 12 }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1, marginRight: 16 }}>
                  <div style={{ fontWeight: 500, marginBottom: 4 }}>
                    {joke.title}
                  </div>
                  <div style={{ fontSize: 12, color: '#666', marginBottom: 8 }}>
                    {joke.content.substring(0, 100)}{joke.content.length > 100 ? '...' : ''}
                  </div>
                  <Space size="small">
                    <Text type="secondary" style={{ fontSize: 11 }}>AI评分: {joke.scores.aiScore?.toFixed(1) || 'N/A'}</Text>
                    <Text type="secondary" style={{ fontSize: 11 }}>最终评分: {joke.scores.finalScore?.toFixed(1)}</Text>
                    <Text type="secondary" style={{ fontSize: 11 }}>作者: {joke.author.username}</Text>
                  </Space>
                </div>
                <Space direction="vertical" size="small">
                  <Button
                    size="small"
                    icon={<RobotOutlined />}
                    onClick={() => handleSingleScore(joke.id)}
                  >
                    AI评分
                  </Button>
                </Space>
              </div>
            </Card>
          ))}
        </div>

        {jokes.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Text type="secondary">暂无笑话数据</Text>
          </div>
        )}
      </Spin>
    </Modal>
  );
};

export default AIScoreModal;
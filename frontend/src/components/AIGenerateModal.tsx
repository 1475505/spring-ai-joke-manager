import React, { useState } from 'react';
import { Modal, Form, Input, Button, message, InputNumber, Space, Card, Typography, Spin, Divider } from 'antd';
import { BulbOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { aiAPI, jokesAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { Text, Title } = Typography;
const { TextArea } = Input;

interface AIGenerateModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  themeId?: number;
}

interface GeneratedJoke {
  title: string;
  content: string;
  aiScore: number;
  qualityLevel: string;
  isSubmitting?: boolean;
  isSubmitted?: boolean;
}

const AIGenerateModal: React.FC<AIGenerateModalProps> = ({ visible, onClose, onSuccess, themeId }) => {
  const { user, hasPermission } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [generatedJokes, setGeneratedJokes] = useState<GeneratedJoke[]>([]);
  const [form] = Form.useForm();

  // 移除权限检查，允许所有用户使用AI生成功能

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

  // 生成笑话
  const handleGenerate = async (values: { prompt: string; count: number }) => {
    const config = getOpenAIConfig();
    if (!config) {
      message.error('请先在右上角配置OpenAI设置');
      return;
    }



    if (!themeId) {
      message.error('请先选择主题');
      return;
    }

    setLoading(true);
    try {
      const response = await aiAPI.generateJokes({
        themeId,
        prompt: values.prompt,
        count: values.count,
        apiKey: config.apiKey,
        modelName: config.model,
        baseUrl: config.baseUrl
      });
      
      setGeneratedJokes(response.data.jokes || []);
      message.success('成功生成笑话');
    } catch (error) {
      message.error('AI生成笑话失败');
      console.error('AI生成失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 重置表单和结果
  const handleReset = () => {
    form.resetFields();
    setGeneratedJokes([]);
  };

  // 关闭模态框时重置
  const handleClose = () => {
    handleReset();
    onClose();
  };

  // 投稿笑话
  const handleSubmitJoke = async (joke: GeneratedJoke, index: number) => {
    // 如果已经在投稿中或已投稿，直接返回
    if (joke.isSubmitting || joke.isSubmitted) {
      return;
    }
    
    // 设置投稿状态为加载中
    const updatedJokes = [...generatedJokes];
    updatedJokes[index] = { ...updatedJokes[index], isSubmitting: true };
    setGeneratedJokes(updatedJokes);
    
    try {
      await jokesAPI.create({
        title: joke.title,
        content: joke.content,
        themeId: themeId || 1 // 使用传入的themeId或默认值
      });
      message.success('笑话投稿成功，等待审核');
      
      // 标记为已投稿，不再移除笑话
      updatedJokes[index] = { ...updatedJokes[index], isSubmitting: false, isSubmitted: true };
      setGeneratedJokes(updatedJokes);
    } catch (error) {
      message.error('投稿失败，请重试');
      // 投稿失败时重置状态
      updatedJokes[index] = { ...updatedJokes[index], isSubmitting: false };
      setGeneratedJokes(updatedJokes);
    }
  };

  // 编辑笑话
  const handleEditJoke = (index: number, field: 'title' | 'content', value: string) => {
    const updatedJokes = [...generatedJokes];
    updatedJokes[index] = { ...updatedJokes[index], [field]: value };
    setGeneratedJokes(updatedJokes);
  };

  // 完成生成（关闭弹窗）
  const handleComplete = () => {
    onSuccess?.();
    handleClose();
  };

  // 移除权限检查的渲染逻辑

  const config = getOpenAIConfig();
  if (!config) {
    return (
      <Modal
        title="AI生成笑话"
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
      title={<><BulbOutlined /> AI生成笑话</>}
      open={visible}
      onCancel={handleClose}
      width={800}
      footer={null}
    >
      <Spin spinning={loading}>
        <div style={{ marginBottom: 16 }}>
          <Text strong>当前配置：</Text>
          <Text type="secondary"> {config.model} @ {config.baseUrl}</Text>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleGenerate}
          initialValues={{ count: 1 }}
        >
          <Form.Item
            name="prompt"
            label="生成提示词"
            rules={[
              { required: true, message: '请输入生成提示词' },
              { min: 1, message: '提示词至少1个字符' }
            ]}
          >
            <TextArea
              rows={4}
              placeholder="请输入笑话生成的提示词，例如：关于程序员的搞笑日常、办公室里的趣事等..."
              showCount
              maxLength={500}
            />
          </Form.Item>

          {/* 生成数量固定为1，隐藏输入框 */}
          <Form.Item name="count" hidden>
            <InputNumber value={1} />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                icon={<ThunderboltOutlined />}
                loading={loading}
              >
                生成笑话
              </Button>
              <Button onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {generatedJokes.length > 0 && (
          <>
            <Divider>生成结果</Divider>
            <div style={{ maxHeight: 400, overflowY: 'auto' }}>
              {generatedJokes.map((joke, index) => (
                <Card
                  key={index}
                  size="small"
                  style={{ marginBottom: 12 }}
                  title={
                    <Space>
                      <Text>笑话 {index + 1}</Text>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        AI评分: {joke.aiScore?.toFixed(1)}
                      </Text>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        质量: {joke.qualityLevel}
                      </Text>
                    </Space>
                  }
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <Text strong>标题：</Text>
                      <Input
                        value={joke.title}
                        onChange={(e) => handleEditJoke(index, 'title', e.target.value)}
                        placeholder="请输入笑话标题"
                        style={{ marginTop: 4 }}
                      />
                    </div>
                    <div>
                      <Text strong>内容：</Text>
                      <Input.TextArea
                        value={joke.content}
                        onChange={(e) => handleEditJoke(index, 'content', e.target.value)}
                        placeholder="请输入笑话内容"
                        rows={4}
                        style={{ marginTop: 4 }}
                      />
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <Button
                        type={joke.isSubmitted ? "default" : "primary"}
                        size="small"
                        loading={joke.isSubmitting}
                        disabled={joke.isSubmitting || joke.isSubmitted}
                        onClick={() => handleSubmitJoke(joke, index)}
                      >
                        {joke.isSubmitting ? '投稿中...' : joke.isSubmitted ? '已投稿' : '投稿这个笑话'}
                      </Button>
                    </div>
                  </Space>
                </Card>
              ))}
            </div>
            
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Space>
                <Button onClick={handleComplete}>
                  关闭
                </Button>
                <Button onClick={handleReset}>
                  重新生成
                </Button>
              </Space>
            </div>
          </>
        )}
      </Spin>
    </Modal>
  );
};

export default AIGenerateModal;
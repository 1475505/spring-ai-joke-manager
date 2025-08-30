import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, Button, message, Grid, Card, Space, Typography } from 'antd';
import { jokesAPI, themesAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { TextArea } = Input;
const { Option } = Select;
const { useBreakpoint } = Grid;
const { Text } = Typography;

interface CreateJokeModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const CreateJokeModal: React.FC<CreateJokeModalProps> = ({ visible, onClose, onSuccess }) => {
  const { user } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const screens = useBreakpoint();
  const isMobile = !screens.md;
  const [similarJoke, setSimilarJoke] = useState<any>(null);
  const [showSimilarityWarning, setShowSimilarityWarning] = useState(false);
  const [pendingSubmit, setPendingSubmit] = useState<any>(null);
  const [themes, setThemes] = useState<any[]>([]);

  // 加载主题列表
  useEffect(() => {
    const loadThemes = async () => {
      try {
        const response = await themesAPI.getThemes();
        setThemes(response.data || []);
      } catch (error) {
        console.error('加载主题失败:', error);
      }
    };
    
    if (visible) {
      loadThemes();
    }
  }, [visible]);

  // 设置默认标题
  useEffect(() => {
    if (visible && user) {
      form.setFieldsValue({
        title: `${user.username}的笑话`
      });
    } else if (visible && !user) {
      form.setFieldsValue({
        title: ''
      });
    }
  }, [visible, user, form]);

  const checkSimilarity = async (content: string, theme: number) => {
    try {
      const response = await jokesAPI.checkSimilarity(content, theme);
      return response.data;
    } catch (error) {
      console.error('相似度检查失败:', error);
      return null;
    }
  };

  const handleSubmit = async (values: any) => {
    // 检查相似度
    if (!showSimilarityWarning) {
      setLoading(true);
      // 根据主题名称查找主题ID
      const selectedTheme = themes.find(t => t.name === values.theme);
      if (!selectedTheme) {
        message.error('选择的主题不存在');
        setLoading(false);
        return;
      }
      
      const similarityResult = await checkSimilarity(values.content, selectedTheme.id);
      setLoading(false);

      if (similarityResult && similarityResult.hasSimilar && similarityResult.maxSimilarity > 0.95) {
        setSimilarJoke(similarityResult.similarJokes[0]);
        setShowSimilarityWarning(true);
        setPendingSubmit(values);
        return;
      }
    }

    // 执行创建
    setLoading(true);
    try {
      // 根据主题名称查找主题ID
      const selectedTheme = themes.find(t => t.name === values.theme);
      if (!selectedTheme) {
        message.error('选择的主题不存在');
        return;
      }

      await jokesAPI.create({
        title: values.title,
        content: values.content,
        themeId: selectedTheme.id
      });
      message.success('感谢投稿笑话，请等待管理员审批');
      form.resetFields();
      setSimilarJoke(null);
      setShowSimilarityWarning(false);
      setPendingSubmit(null);
      onSuccess();
      onClose();
    } catch (error) {
      message.error('创建笑话失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmCreate = () => {
    if (pendingSubmit) {
      setShowSimilarityWarning(true);
      handleSubmit(pendingSubmit);
    }
  };

  const handleCancelCreate = () => {
    setSimilarJoke(null);
    setShowSimilarityWarning(false);
    setPendingSubmit(null);
  };

  return (
    <Modal
      title="创建新笑话"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
    >
      {showSimilarityWarning && similarJoke ? (
        <div style={{ marginBottom: 16 }}>
          <Card title="发现相似笑话" size="small">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text type="warning">
                检测到相似度超过95%的笑话，请确认是否继续创建：
              </Text>
              <Card size="small" style={{ backgroundColor: '#fff7e6' }}>
                <Text strong>{similarJoke.title || '无标题'}</Text>
                <br />
                <Text>{similarJoke.content}</Text>
                <br />
                <Text type="secondary">
                  主题：{similarJoke.theme} | 评分：{similarJoke.score}
                </Text>
              </Card>
              <Space>
                <Button onClick={handleCancelCreate}>
                  取消创建
                </Button>
                <Button type="primary" onClick={handleConfirmCreate} loading={loading}>
                  仍然创建
                </Button>
              </Space>
            </Space>
          </Card>
        </div>
      ) : null}

      <Form
        form={form}
        onFinish={handleSubmit}
        layout="vertical"
        initialValues={{ theme: '鸡煲笑话' }}
        size={isMobile ? 'small' : 'middle'}
      >
        <Form.Item
          name="title"
          label="标题（可选）"
        >
          <Input placeholder="请输入笑话标题（可选）" />
        </Form.Item>

        <Form.Item
          name="theme"
          label="主题"
          rules={[{ required: true, message: '请选择主题' }]}
        >
          <Select placeholder="请选择主题" loading={themes.length === 0}>
            {themes.map(theme => (
              <Option key={theme.id} value={theme.name}>{theme.name}</Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          name="content"
          label="内容"
          rules={[
            { required: true, message: '笑话内容不能为空，请输入至少3个字符的内容' },
            { min: 3, message: '笑话内容长度必须在3-2000字符之间。当前内容过短，请添加更多文字使内容更加丰富有趣' },
            { max: 2000, message: '笑话内容长度必须在3-2000字符之间。当前内容过长，请适当精简内容' }
          ]}
        >
          <TextArea
            rows={isMobile ? 4 : 6}
            placeholder="请输入笑话内容"
            maxLength={1000}
            showCount
          />
        </Form.Item>

        <Form.Item style={{ textAlign: 'right', marginBottom: 0 }}>
          <Button onClick={onClose} style={{ marginRight: 8 }}>
            取消
          </Button>
          <Button 
            type="primary" 
            htmlType="submit" 
            loading={loading}
            disabled={showSimilarityWarning}
          >
            {showSimilarityWarning ? '检查相似度中...' : '创建'}
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateJokeModal;

import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, message, Grid, AutoComplete, Tabs } from 'antd';
import { SettingOutlined, KeyOutlined, LinkOutlined, DatabaseOutlined } from '@ant-design/icons';

const { useBreakpoint } = Grid;

interface OpenAIConfigModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

interface OpenAIConfig {
  apiKey: string;
  model: string;
  baseUrl: string;
}

interface EmbeddingConfig {
  apiKey: string;
  modelName: string;
  baseUrl: string;
}

const OpenAIConfigModal: React.FC<OpenAIConfigModalProps> = ({ visible, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [embeddingForm] = Form.useForm();
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  // 从localStorage加载配置
  const loadConfig = (): OpenAIConfig => {
    try {
      const config = localStorage.getItem('openai_config');
      if (config) {
        return JSON.parse(config);
      }
    } catch (error) {
      console.error('加载OpenAI配置失败:', error);
    }
    return {
      apiKey: '',
      model: 'deepseek-chat',
      baseUrl: 'https://api.deepseek.com/v1'
    };
  };

  // 从localStorage加载embedding配置
  const loadEmbeddingConfig = (): EmbeddingConfig => {
    try {
      const config = localStorage.getItem('embedding_config');
      if (config) {
        return JSON.parse(config);
      }
    } catch (error) {
      console.error('加载Embedding配置失败:', error);
    }
    return {
      apiKey: '',
      modelName: 'BAAI/bge-m3',
      baseUrl: 'https://api.siliconflow.cn'
    };
  };

  // 保存配置到localStorage
  const saveConfig = (config: OpenAIConfig) => {
    try {
      localStorage.setItem('openai_config', JSON.stringify(config));
    } catch (error) {
      console.error('保存OpenAI配置失败:', error);
      throw error;
    }
  };

  // 保存embedding配置到localStorage
  const saveEmbeddingConfig = (config: EmbeddingConfig) => {
    try {
      localStorage.setItem('embedding_config', JSON.stringify(config));
    } catch (error) {
      console.error('保存Embedding配置失败:', error);
      throw error;
    }
  };

  useEffect(() => {
    if (visible) {
      const config = loadConfig();
      const embeddingConfig = loadEmbeddingConfig();
      form.setFieldsValue(config);
      embeddingForm.setFieldsValue(embeddingConfig);
    }
  }, [visible, form, embeddingForm]);

  const handleSaveConfig = async (values: OpenAIConfig) => {
    setLoading(true);
    try {
      saveConfig(values);
      message.success('OpenAI配置保存成功！');
      onSuccess?.();
      onClose();
    } catch (error) {
      message.error('保存配置失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveEmbeddingConfig = async (values: EmbeddingConfig) => {
    setLoading(true);
    try {
      saveEmbeddingConfig(values);
      message.success('Embedding配置保存成功！');
      onSuccess?.();
      onClose();
    } catch (error) {
      message.error('保存配置失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    const defaultConfig = {
      apiKey: '',
      model: 'deepseek-chat',
      baseUrl: 'https://api.deepseek.com/v1'
    };
    form.setFieldsValue(defaultConfig);
  };

  const handleEmbeddingReset = () => {
    const defaultConfig = {
      apiKey: '',
      modelName: 'BAAI/bge-m3',
      baseUrl: 'https://api.siliconflow.cn'
    };
    embeddingForm.setFieldsValue(defaultConfig);
  };

  return (
    <Modal
      title="AI配置"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={isMobile ? '90%' : 600}
    >
      <Tabs
        defaultActiveKey="openai"
        items={[
          {
            key: 'openai',
            label: 'OpenAI配置',
            icon: <SettingOutlined />,
            children: (
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSaveConfig}
                size={isMobile ? 'small' : 'middle'}
              >
                <Form.Item
                  name="apiKey"
                  label="API Key"
                  rules={[
                    { required: true, message: '请输入OpenAI API Key' }
                  ]}
                >
                  <Input.Password
                    prefix={<KeyOutlined />}
                    placeholder="请输入OpenAI API Key"
                  />
                </Form.Item>

                <Form.Item
                  name="model"
                  label="模型"
                  rules={[
                    { required: true, message: '请选择或输入模型名称' }
                  ]}
                >
                  <AutoComplete
                    placeholder="输入模型名称（如：dee 可联想到 deepseek 系列）"
                    allowClear
                    filterOption={(inputValue, option) =>
                      option!.value.toLowerCase().indexOf(inputValue.toLowerCase()) !== -1
                    }
                    options={[
                      { value: 'deepseek-chat', label: 'deepseek-chat' },
                      { value: 'deepseek-reasoner', label: 'deepseek-reasoner' },
                      { value: 'gpt-4o', label: 'gpt-4o' },
                      { value: 'gpt-4.1', label: 'gpt-4.1' },
                      { value: 'claude-3.5-sonnet', label: 'claude-3.5-sonnet' },
                      { value: 'gemini-2.5-pro', label: 'gemini-2.5-pro' },
                    ]}
                  />
                </Form.Item>

                <Form.Item
                  name="baseUrl"
                  label="Base URL"
                  rules={[
                    { required: true, message: '请输入Base URL' },
                    { type: 'url', message: '请输入有效的URL地址' }
                  ]}
                >
                  <Input
                    prefix={<LinkOutlined />}
                    placeholder="请输入OpenAI API Base URL"
                  />
                </Form.Item>

                <Form.Item style={{ marginBottom: 0 }}>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <Button
                      type="default"
                      onClick={handleReset}
                      style={{ flex: 1 }}
                    >
                      重置
                    </Button>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      style={{ flex: 2 }}
                    >
                      保存配置
                    </Button>
                  </div>
                </Form.Item>
              </Form>
            )
          },
          {
            key: 'embedding',
            label: 'Embedding配置',
            icon: <DatabaseOutlined />,
            children: (
              <Form
                form={embeddingForm}
                layout="vertical"
                onFinish={handleSaveEmbeddingConfig}
                size={isMobile ? 'small' : 'middle'}
              >
                <Form.Item
                  name="apiKey"
                  label="API Key"
                  rules={[
                    { required: true, message: '请输入Embedding API Key' }
                  ]}
                >
                  <Input.Password
                    prefix={<KeyOutlined />}
                    placeholder="请输入Embedding API Key"
                  />
                </Form.Item>

                <Form.Item
                  name="modelName"
                  label="模型名称"
                  rules={[
                    { required: true, message: '请选择或输入模型名称' }
                  ]}
                >
                  <AutoComplete
                    placeholder="输入模型名称"
                    allowClear
                    filterOption={(inputValue, option) =>
                      option!.value.toLowerCase().indexOf(inputValue.toLowerCase()) !== -1
                    }
                    options={[
                      { value: 'BAAI/bge-m3', label: 'BAAI/bge-m3' },
                      { value: 'BAAI/bge-large-zh-v1.5', label: 'BAAI/bge-large-zh-v1.5' },
                      { value: 'text-embedding-ada-002', label: 'text-embedding-ada-002' },
                      { value: 'text-embedding-3-small', label: 'text-embedding-3-small' },
                      { value: 'text-embedding-3-large', label: 'text-embedding-3-large' },
                    ]}
                  />
                </Form.Item>

                <Form.Item
                  name="baseUrl"
                  label="Base URL"
                  rules={[
                    { required: true, message: '请输入Base URL' },
                    { type: 'url', message: '请输入有效的URL地址' }
                  ]}
                >
                  <Input
                    prefix={<LinkOutlined />}
                    placeholder="请输入Embedding API Base URL"
                  />
                </Form.Item>

                <Form.Item style={{ marginBottom: 0 }}>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <Button
                      type="default"
                      onClick={handleEmbeddingReset}
                      style={{ flex: 1 }}
                    >
                      重置
                    </Button>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      style={{ flex: 2 }}
                    >
                      保存配置
                    </Button>
                  </div>
                </Form.Item>
              </Form>
            )
          }
        ]}
      />

      <div style={{ marginTop: '16px', padding: '12px', backgroundColor: '#f6f8fa', borderRadius: '6px' }}>
        <p style={{ margin: 0, fontSize: '14px', color: '#333', fontWeight: 'bold', marginBottom: '8px' }}>
          <SettingOutlined /> 配置说明：
        </p>
        <div style={{ fontSize: '14px', color: '#666' }}>
          <div>• <strong>API Key:</strong> 您的AI API密钥</div>
          <div>• <strong>模型:</strong> 选择要使用的AI模型</div>
          <div>• <strong>Base URL:</strong> AI API的基础URL，支持代理服务</div>
          <div style={{ marginTop: '8px', fontStyle: 'italic' }}>配置将只保存在浏览器本地存储中，不会存储在服务器上</div>
          <div style={{ marginTop: '8px', fontStyle: 'italic' }}>希望配置的模型支持json output和tool call，否则可能无法正常使用</div>
        </div>
      </div>
    </Modal>
  );
};

export default OpenAIConfigModal;
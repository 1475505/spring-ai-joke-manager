import React, { useState, useRef, useEffect } from 'react';
import { Button, Input, Modal, List, Card, Space, Typography, message, Spin } from 'antd';
import { SendOutlined, RobotOutlined, UserOutlined, ToolOutlined } from '@ant-design/icons';
import { mcpAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { TextArea } = Input;
const { Text } = Typography;

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface ToolCall {
  name: string;
  arguments: Record<string, any>;
}

const MCPChat: React.FC = () => {
  const [visible, setVisible] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [toolCalls, setToolCalls] = useState<ToolCall[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { user } = useAuthStore();

  // 检查是否为ROOT用户
  const isRootUser = user?.role === 'ROOT';

  // 滚动到最新消息
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 发送消息
  const sendMessage = async () => {
    if (!inputValue.trim() || loading) return;

    // 添加用户消息到聊天记录
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setLoading(true);
    setToolCalls([]);

    try {
      // 调用后端MCP接口
      const response = await mcpAPI.chat(inputValue);
      
      // 由于响应拦截器已经将response.data替换为response.data.data
      // 所以直接从response.data中获取aiResponse
      const aiResponse = response.data.aiResponse;
      
      // 添加AI回复到聊天记录
      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: aiResponse,
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, aiMessage]);
    } catch (error: any) {
      console.error('MCP chat error:', error);
      message.error(error.message || '发送消息失败');
      
      // 添加错误消息到聊天记录
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: '抱歉，处理您的请求时发生了错误。',
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  // 处理输入框按键
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // 打开对话框
  const openChat = () => {
    if (!isRootUser) {
      message.error('仅ROOT用户可以使用此功能');
      return;
    }
    
    setVisible(true);
    
    // 添加欢迎消息
    if (messages.length === 0) {
      const welcomeMessage: Message = {
        id: 'welcome',
        role: 'assistant',
        content: '您好！我是您的AI助手，可以通过自然语言帮您操作笑话数据库。您可以问我："查询所有包含「想法」的笑话"、"创建一个关于程序员的笑话"、"删除ID为1的笑话"等。',
        timestamp: new Date(),
      };
      setMessages([welcomeMessage]);
    }
  };

  // 关闭对话框
  const closeChat = () => {
    setVisible(false);
    setMessages([]);
    setInputValue('');
    setToolCalls([]);
  };

  return (
    <>
      {/* 悬浮按钮 */}
      {isRootUser && (
        <div 
          style={{
            position: 'fixed',
            right: 24,
            bottom: 24,
            zIndex: 1000,
          }}
        >
          <Button
            type="primary"
            shape="circle"
            icon={<RobotOutlined />}
            size="large"
            onClick={openChat}
            style={{
              width: 60,
              height: 60,
              fontSize: 20,
              boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            }}
          />
        </div>
      )}

      {/* 对话框 */}
      <Modal
        title={
          <Space>
            <RobotOutlined />
            <span>MCP自然语言操作</span>
          </Space>
        }
        open={visible}
        onCancel={closeChat}
        footer={null}
        width={600}
        styles={{
          body: { padding: 0 }
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', height: '60vh' }}>
          {/* 消息列表 */}
          <div 
            style={{ 
              flex: 1, 
              overflowY: 'auto', 
              padding: '16px',
              backgroundColor: '#f5f5f5'
            }}
          >
            <List
              dataSource={messages}
              renderItem={(item) => (
                <List.Item style={{ border: 'none', padding: '8px 0' }}>
                  <Card
                    size="small"
                    style={{
                      maxWidth: '80%',
                      marginLeft: item.role === 'user' ? 'auto' : 0,
                      marginRight: item.role === 'assistant' ? 'auto' : 0,
                      backgroundColor: item.role === 'user' ? '#e6f7ff' : '#ffffff',
                      borderRadius: '8px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                      {item.role === 'user' ? (
                        <UserOutlined style={{ color: '#1890ff', marginRight: 8, marginTop: 2 }} />
                      ) : (
                        <RobotOutlined style={{ color: '#52c41a', marginRight: 8, marginTop: 2 }} />
                      )}
                      <div>
                        <Text strong>
                          {item.role === 'user' ? '您' : 'AI助手'}
                        </Text>
                        <div style={{ marginTop: 4 }} dangerouslySetInnerHTML={{ __html: item.content.replace(/\n/g, '<br>') }}></div>
                        <Text type="secondary" style={{ fontSize: '12px', marginTop: 8, display: 'block' }}>
                          {item.timestamp.toLocaleTimeString()}
                        </Text>
                      </div>
                    </div>
                  </Card>
                </List.Item>
              )}
            />
            {loading && (
              <div style={{ textAlign: 'center', padding: '16px' }}>
                <Spin tip="AI正在处理中..." />
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* 输入区域 */}
          <div style={{ padding: '16px', borderTop: '1px solid #f0f0f0' }}>
            <TextArea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onPressEnter={handleKeyPress}
              placeholder="请输入您的指令，例如：查询所有笑话、创建一个关于程序员的笑话..."
              autoSize={{ minRows: 2, maxRows: 4 }}
              disabled={loading}
            />
            <div style={{ marginTop: 12, textAlign: 'right' }}>
              <Button 
                type="primary" 
                icon={<SendOutlined />} 
                onClick={sendMessage}
                loading={loading}
                disabled={!inputValue.trim()}
              >
                发送
              </Button>
            </div>
          </div>
        </div>
      </Modal>
    </>
  );
};

export default MCPChat;
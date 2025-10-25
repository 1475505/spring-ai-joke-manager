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
      
      // 正确处理后端返回的数据格式
      // 后端返回的是包含aiResponse和toolCalls的对象
      const responseData = response.data;
      const aiResponse = responseData.aiResponse || responseData; // 兼容可能的直接文本返回
      const toolCalls: ToolCall[] = responseData.toolCalls || [];
      
      // 更新工具调用状态
      setToolCalls(toolCalls);
      
      // 添加AI回复到聊天记录
      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: aiResponse,
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, aiMessage]);
      
      // 显示工具调用返回链
      if (toolCalls && toolCalls.length > 0) {
        try {
          // 构建工具调用链的格式化内容，避免使用JSX组件标签
          let toolChainContent = '<div style="margin-top: 8px; padding: 12px; background-color: #f0f5ff; border-radius: 6px;">';
          toolChainContent += '<div style="display: flex; align-items: center; margin-bottom: 8px;"><span style="color: #1890ff; margin-right: 8px;">⚙️</span> <strong>工具调用链：</strong></div>';
          
          // 遍历每个工具调用
          toolCalls.forEach((toolCall: ToolCall, index: number) => {
            // 安全检查每个属性
            const safeName = toolCall.name || '未知工具';
            const safeArgs = toolCall.arguments || {};
            const argsString = JSON.stringify(safeArgs, null, 2) || '{}';
            
            toolChainContent += `<div style="margin-bottom: 12px; padding-left: 24px; position: relative;">`;
            toolChainContent += `<div style="position: absolute; left: 0; top: 6px; width: 16px; height: 1px; background-color: #d9d9d9;"></div>`;
            toolChainContent += `<div style="margin-bottom: 4px;"><strong>工具名：</strong>${safeName}</div>`;
            toolChainContent += `<div><strong>参数：</strong></div><div style="background-color: #fafafa; padding: 8px; border-radius: 4px; overflow-x: auto; font-family: monospace; white-space: pre-wrap;">`;
            toolChainContent += argsString;
            toolChainContent += `</div>`;
            toolChainContent += `</div>`;
          });
          
          toolChainContent += '</div>';
          
          const toolCallMessage: Message = {
            id: (Date.now() + 2).toString(),
            role: 'assistant',
            content: toolChainContent,
            timestamp: new Date(),
          };
          setMessages(prev => [...prev, toolCallMessage]);
        } catch (error) {
          console.error('工具调用链渲染错误:', error);
          // 添加简单的错误消息而不是复杂的HTML结构
          const errorMessage: Message = {
            id: (Date.now() + 2).toString(),
            role: 'assistant',
            content: '工具调用链渲染失败',
            timestamp: new Date(),
          };
          setMessages(prev => [...prev, errorMessage]);
        }
      } else {
        // 没有工具被调用时，显示提示信息
        const noToolsMessage: Message = {
          id: (Date.now() + 2).toString(),
          role: 'assistant',
          content: '<div style="margin-top: 8px; padding: 12px; background-color: #f6ffed; border-radius: 6px; border-left: 4px solid #52c41a;"><span style="color: #52c41a; margin-right: 8px;">💡</span>本次对话未调用任何工具</div>',
          timestamp: new Date(),
        };
        setMessages(prev => [...prev, noToolsMessage]);
      }
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
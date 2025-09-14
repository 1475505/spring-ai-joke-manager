import React, { useState, useEffect } from 'react';
import { Button, Space, Typography, Modal, Form, InputNumber, Input, message, List, Tag } from 'antd';
import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { jokesAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

interface PendingJoke {
  id: number;
  title: string;
  content: string;
  theme: {
    id: number;
    name: string;
  };
  author: {
    id: number;
    username: string;
  } | null;
  createdAt: string;
  status: string;
}

interface AdminPanelProps {
  visible: boolean;
  onClose: () => void;
  currentThemeId: number;
  onSuccess?: () => void;
}

const AdminPanel: React.FC<AdminPanelProps> = ({ visible, onClose, currentThemeId, onSuccess }) => {
  const { hasPermission } = useAuthStore();
  const [pendingJokes, setPendingJokes] = useState<PendingJoke[]>([]);
  const [loading, setLoading] = useState(false);
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [selectedJoke, setSelectedJoke] = useState<PendingJoke | null>(null);
  const [form] = Form.useForm();
  const [rejectForm] = Form.useForm();

  // 加载待审核笑话
  const loadPendingJokes = async () => {
    if (!hasPermission('write', currentThemeId)) {
      return;
    }

    setLoading(true);
    try {
      const response = await jokesAPI.getPending(currentThemeId);
      setPendingJokes(response.data.content || response.data || []);
    } catch (error) {
      message.error('加载待审核笑话失败');
      console.error('Load pending jokes error:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && hasPermission('write', currentThemeId)) {
      loadPendingJokes();
    }
  }, [visible, currentThemeId]);

  // 审批通过
  const handleApprove = async (values: any) => {
    if (!selectedJoke) return;

    try {
      await jokesAPI.approve(selectedJoke.id, values.manualScore);
      message.success('笑话审批通过');
      setApproveModalVisible(false);
      form.resetFields();
      setSelectedJoke(null);
      loadPendingJokes();
    } catch (error) {
      message.error('审批失败');
    }
  };

  // 审批拒绝
  const handleReject = async (values: any) => {
    if (!selectedJoke) return;

    try {
      await jokesAPI.reject(selectedJoke.id, values.reason);
      message.success('笑话已拒绝');
      setRejectModalVisible(false);
      rejectForm.resetFields();
      setSelectedJoke(null);
      loadPendingJokes();
    } catch (error) {
      message.error('拒绝失败');
    }
  };

  const openApproveModal = (joke: PendingJoke) => {
    setSelectedJoke(joke);
    setApproveModalVisible(true);
  };

  const openRejectModal = (joke: PendingJoke) => {
    setSelectedJoke(joke);
    setRejectModalVisible(true);
  };

  if (!hasPermission('write', currentThemeId)) {
    return null;
  }

  return (
    <>
      <Modal
        title="管理员审批面板"
        open={visible}
        onCancel={onClose}
        footer={null}
        width={800}
        style={{ top: 20 }}
      >
        <div style={{ marginBottom: 16 }}>
          <Text strong>待审核笑话列表 ({pendingJokes.length})</Text>
          <Button 
            type="link" 
            onClick={loadPendingJokes}
            loading={loading}
            style={{ float: 'right' }}
          >
            刷新
          </Button>
        </div>

        <List
          loading={loading}
          dataSource={pendingJokes}
          renderItem={(joke) => (
            <List.Item
              actions={[
                <Button
                  key="approve"
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => openApproveModal(joke)}
                  size="small"
                >
                  通过
                </Button>,
                <Button
                  key="reject"
                  danger
                  icon={<CloseOutlined />}
                  onClick={() => openRejectModal(joke)}
                  size="small"
                >
                  拒绝
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Text strong>{joke.title || '无标题'}</Text>
                    <Tag color="blue">{joke.theme.name}</Tag>
                    <Tag color="green">{joke.status}</Tag>
                  </Space>
                }
                description={
                  <div>
                    <Paragraph
                      ellipsis={{ rows: 2, expandable: true, symbol: '展开' }}
                      style={{ marginBottom: 8 }}
                    >
                      {joke.content}
                    </Paragraph>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      作者: {joke.author?.username || '匿名用户'} | 
                      创建时间: {new Date(joke.createdAt).toLocaleString()}
                    </Text>
                  </div>
                }
              />
            </List.Item>
          )}
          locale={{ emptyText: '暂无待审核笑话' }}
        />
      </Modal>

      {/* 审批通过模态框 */}
      <Modal
        title="审批通过"
        open={approveModalVisible}
        onCancel={() => {
          setApproveModalVisible(false);
          form.resetFields();
          setSelectedJoke(null);
        }}
        footer={null}
      >
        <Form
          form={form}
          onFinish={handleApprove}
          layout="vertical"
        >
          {selectedJoke && (
            <div style={{ marginBottom: 16, padding: 12, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
              <Text strong>{selectedJoke.title || '无标题'}</Text>
              <Paragraph style={{ marginTop: 8, marginBottom: 0 }}>
                {selectedJoke.content}
              </Paragraph>
            </div>
          )}
          
          <Form.Item
            name="manualScore"
            label="人工评分 (可选)"
            help="0-10分，不填写则使用默认评分"
          >
            <InputNumber
              min={0}
              max={10}
              step={0.1}
              placeholder="请输入评分"
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => {
                setApproveModalVisible(false);
                form.resetFields();
                setSelectedJoke(null);
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                确认通过
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 审批拒绝模态框 */}
      <Modal
        title="审批拒绝"
        open={rejectModalVisible}
        onCancel={() => {
          setRejectModalVisible(false);
          rejectForm.resetFields();
          setSelectedJoke(null);
        }}
        footer={null}
      >
        <Form
          form={rejectForm}
          onFinish={handleReject}
          layout="vertical"
        >
          {selectedJoke && (
            <div style={{ marginBottom: 16, padding: 12, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
              <Text strong>{selectedJoke.title || '无标题'}</Text>
              <Paragraph style={{ marginTop: 8, marginBottom: 0 }}>
                {selectedJoke.content}
              </Paragraph>
            </div>
          )}
          
          <Form.Item
            name="reason"
            label="拒绝原因 (可选)"
            help="请说明拒绝的原因"
          >
            <TextArea
              rows={3}
              placeholder="请输入拒绝原因..."
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => {
                setRejectModalVisible(false);
                rejectForm.resetFields();
                setSelectedJoke(null);
              }}>
                取消
              </Button>
              <Button type="primary" danger htmlType="submit">
                确认拒绝
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AdminPanel;
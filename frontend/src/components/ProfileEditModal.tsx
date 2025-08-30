import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, message, Grid } from 'antd';
import { UserOutlined, LinkOutlined } from '@ant-design/icons';
import { authAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { useBreakpoint } = Grid;

interface ProfileEditModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

const ProfileEditModal: React.FC<ProfileEditModalProps> = ({ visible, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const { user, updateUser } = useAuthStore();
  const [form] = Form.useForm();
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  useEffect(() => {
    if (visible && user) {
      form.setFieldsValue({
        username: user.username,
        avatarUrl: user.avatarUrl || ''
      });
    }
  }, [visible, user, form]);

  const handleUpdateProfile = async (values: { username?: string; avatarUrl?: string }) => {
    if (!user) {
      message.error('用户信息不存在');
      return;
    }

    setLoading(true);
    try {
      // 更新头像URL
      if (values.avatarUrl !== user.avatarUrl) {
        await authAPI.updateProfile(values.avatarUrl);
      }

      // 如果用户名发生变化，需要调用用户管理接口
      if (values.username && values.username !== user.username) {
        // 注意：这里需要后端支持用户自己更新用户名的接口
        // 目前的updateProfile接口只支持avatarUrl
        message.warning('用户名修改功能暂未开放');
      }

      // 更新本地用户信息
      const updatedUser = {
        ...user,
        avatarUrl: values.avatarUrl || user.avatarUrl
      };
      updateUser(updatedUser);

      message.success('个人信息更新成功！');
      onSuccess?.();
      onClose();
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || '更新个人信息失败，请重试';
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="编辑个人信息"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={isMobile ? '90%' : 500}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleUpdateProfile}
        size={isMobile ? 'small' : 'middle'}
      >
        <Form.Item
          name="username"
          label="用户名"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, max: 50, message: '用户名长度必须在3-50字符之间' }
          ]}
        >
          <Input
            prefix={<UserOutlined />}
            placeholder="请输入用户名"
            disabled // 暂时禁用用户名修改
          />
        </Form.Item>

        <Form.Item
          name="avatarUrl"
          label="头像URL"
          rules={[
            { type: 'url', message: '请输入有效的URL地址' }
          ]}
        >
          <Input
            prefix={<LinkOutlined />}
            placeholder="请输入头像URL（可选）"
          />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            style={{ width: '100%' }}
          >
            更新信息
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ProfileEditModal;
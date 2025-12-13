import React, { useState } from 'react';
import { Modal, Form, Input, Button, Tabs, message, Grid, Select } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { authAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';

const { Option } = Select;
const { useBreakpoint } = Grid;

interface LoginModalProps {
  visible: boolean;
  onClose: () => void;
}

const LoginModal: React.FC<LoginModalProps> = ({ visible, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [registerLoading, setRegisterLoading] = useState(false);
  const [addUserLoading, setAddUserLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('login');
  const { login, user } = useAuthStore();
  const [addUserForm] = Form.useForm();
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const response = await authAPI.login(values.username, values.password);
      
      // 处理登录响应数据
      const responseData = response.data;
      
      if (responseData) {
        const { user, accessToken, refreshToken } = responseData;
        
        if (user && accessToken) {
          login(user, accessToken, refreshToken);
          message.success('登录成功！');
          onClose();
        } else {
          message.error('登录响应格式错误');
        }
      } else {
        message.error('登录失败，请检查用户名和密码');
      }
    } catch (error) {
      const anyErr: any = error;
      const backendMsg = anyErr?.response?.data?.message || anyErr?.message;
      if (backendMsg) {
        message.error(backendMsg);
      } else {
        message.error('登录失败，请检查网络连接');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: { username: string; password: string }) => {
    setRegisterLoading(true);
    try {
      const response = await authAPI.register(values.username, values.password);
      
      // 处理注册响应数据
      const responseData = response.data;
      
      if (responseData) {
        message.success('注册成功！请登录');
        setActiveTab('login'); // 切换到登录选项卡
      } else {
        message.error('注册失败，请重试');
      }
    } catch (error) {
      const anyErr: any = error;
      const backendMsg = anyErr?.response?.data?.message || anyErr?.message;
      if (backendMsg) {
        message.error(backendMsg);
      } else {
        message.error('注册失败，请检查网络连接');
      }
    } finally {
      setRegisterLoading(false);
    }
  };

  const handleAddUser = async (values: any) => {
    if (!user || (user.role !== 'ROOT')) {
      message.error('权限不足');
      return;
    }

    setAddUserLoading(true);
    try {
      await authAPI.addUser(values.username, values.password, values.role);
      message.success('用户添加成功！');
      addUserForm.resetFields();
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || '添加用户失败，请重试';
      message.error(errorMessage);
    } finally {
      setAddUserLoading(false);
    }
  };

  return (
    <Modal
      title="用户登录"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={isMobile ? '90%' : 500}
    >
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        size={isMobile ? 'small' : 'middle'}
        items={[
          {
            key: 'login',
            label: '登录',
            children: (
              <Form
                layout="vertical"
                onFinish={handleLogin}
                size={isMobile ? 'small' : 'middle'}
              >
                <Form.Item
                  name="username"
                  label="用户名"
                  rules={[{ required: true, message: '请输入用户名' }]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="请输入用户名"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  label="密码"
                  rules={[{ required: true, message: '请输入密码' }]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="请输入密码"
                  />
                </Form.Item>

                <Form.Item style={{ marginBottom: 0 }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    style={{ width: '100%' }}
                  >
                    登录
                  </Button>
                </Form.Item>
              </Form>
            ),
          },
          {
            key: 'register',
            label: '注册',
            children: (
              <Form
                layout="vertical"
                onFinish={handleRegister}
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
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  label="密码"
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, max: 100, message: '密码长度必须在6-100字符之间' }
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="请输入密码"
                  />
                </Form.Item>

                <Form.Item
                  name="confirmPassword"
                  label="确认密码"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: '请确认密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve();
                        }
                        return Promise.reject(new Error('两次输入的密码不一致'));
                      },
                    }),
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="请再次输入密码"
                  />
                </Form.Item>

                <Form.Item style={{ marginBottom: 0 }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={registerLoading}
                    style={{ width: '100%' }}
                  >
                    注册
                  </Button>
                </Form.Item>
              </Form>
            ),
          },
          ...(user && (user.role === 'ROOT') ? [{
            key: 'add-user',
            label: '添加用户',
            children: (
              <Form
                form={addUserForm}
                layout="vertical"
                onFinish={handleAddUser}
                size={isMobile ? 'small' : 'middle'}
              >
                <Form.Item
                  name="username"
                  label="用户名"
                  rules={[{ required: true, message: '请输入用户名' }]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="请输入用户名"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  label="密码"
                  rules={[{ required: true, message: '请输入密码' }]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="请输入密码"
                  />
                </Form.Item>

                <Form.Item
                  name="role"
                  label="角色"
                  rules={[{ required: true, message: '请选择角色' }]}
                  initialValue="USER"
                >
                  <Select placeholder="请选择用户角色">
                    <Option value="USER">普通用户</Option>
                    {user?.role === 'ROOT' && (
                      <>
                        <Option value="ADMIN">管理员</Option>
                        <Option value="THEME_ADMIN">主题管理员</Option>
                        <Option value="ROOT">超级管理员</Option>
                      </>
                    )}
                  </Select>
                </Form.Item>

                <Form.Item
                  noStyle
                  shouldUpdate={(prevValues, currentValues) => 
                    prevValues.role !== currentValues.role
                  }
                >
                  {({ getFieldValue }) =>
                    getFieldValue('role') === 'THEME_ADMIN' ? (
                      <Form.Item
                        name="managedTheme"
                        label="管理主题"
                        rules={[{ required: true, message: '请选择管理的主题' }]}
                      >
                        <Select placeholder="请选择管理的主题">
                          <Option value="鸡煲笑话">鸡煲笑话</Option>
                          <Option value="赛诺笑话">赛诺笑话</Option>
                          <Option value="字节蹲坑笑话">字节蹲坑笑话</Option>
                        </Select>
                      </Form.Item>
                    ) : null
                  }
                </Form.Item>

                <Form.Item style={{ marginBottom: 0 }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={addUserLoading}
                    style={{ width: '100%' }}
                  >
                    添加用户
                  </Button>
                </Form.Item>
              </Form>
            ),
          }] : [])
        ]}
      />
    </Modal>
  );
};

export default LoginModal;

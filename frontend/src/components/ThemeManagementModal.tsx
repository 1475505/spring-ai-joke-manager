import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, message, List, Card, Space, Typography, Popconfirm, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UserOutlined } from '@ant-design/icons';
import { useThemeStore } from '../store/themeStore';
import { useAuthStore } from '../store/authStore';
import { usersAPI } from '../services/api';

const { Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

interface ThemeManagementModalProps {
  visible: boolean;
  onClose: () => void;
  embedded?: boolean;
}

interface User {
  id: number;
  username: string;
  role: string;
}

const ThemeManagementModal: React.FC<ThemeManagementModalProps> = ({ visible, onClose, embedded = false }) => {
  const { user, hasPermission, hasAnyThemeAdminPermission } = useAuthStore();
  const { 
    themes, 
    loading, 
    error,
    loadThemes,
    createTheme, 
    updateTheme, 
    deleteTheme,
    grantPermission,
    revokePermission,
    clearError,
    currentThemeId 
  } = useThemeStore();
  
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [permissionForm] = Form.useForm();
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [permissionModalVisible, setPermissionModalVisible] = useState(false);
  const [editingTheme, setEditingTheme] = useState<any>(null);
  const [selectedTheme, setSelectedTheme] = useState<any>(null);
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    if (visible) {
      loadThemes();
      loadUsers();
    }
  }, [visible, loadThemes]);

  useEffect(() => {
    if (error) {
      message.error(error);
      clearError();
    }
  }, [error, clearError]);

  const loadUsers = async () => {
    try {
      const response = await usersAPI.getUsers(0, 100);
      setUsers(response.data.content || []);
    } catch (error) {
      console.error('加载用户列表失败:', error);
    }
  };

  const handleCreateTheme = async (values: any) => {
    try {
      await createTheme(values);
      message.success('主题创建成功');
      setCreateModalVisible(false);
      createForm.resetFields();
    } catch (error) {
      console.error('创建主题失败:', error);
    }
  };

  const handleEditTheme = async (values: any) => {
    if (!editingTheme) return;
    
    try {
      await updateTheme(editingTheme.id, values);
      message.success('主题更新成功');
      setEditModalVisible(false);
      setEditingTheme(null);
      editForm.resetFields();
    } catch (error) {
      console.error('更新主题失败:', error);
    }
  };

  const handleDeleteTheme = async (themeId: number) => {
    try {
      await deleteTheme(themeId);
      message.success('主题删除成功');
    } catch (error) {
      console.error('删除主题失败:', error);
    }
  };

  const handleGrantPermission = async (values: any) => {
    if (!selectedTheme) return;
    
    try {
      await grantPermission(selectedTheme.id, values.userId, values.permissionType);
      message.success('权限授予成功');
      setPermissionModalVisible(false);
      setSelectedTheme(null);
      permissionForm.resetFields();
    } catch (error) {
      console.error('授予权限失败:', error);
    }
  };

  const openEditModal = (theme: any) => {
    setEditingTheme(theme);
    editForm.setFieldsValue({
      name: theme.name,
      prompt: theme.prompt,
      icon: theme.icon
    });
    setEditModalVisible(true);
  };

  const openPermissionModal = (theme: any) => {
    setSelectedTheme(theme);
    setPermissionModalVisible(true);
  };

  // 检查用户权限
  const canManageThemes = user?.role === 'ROOT' || hasAnyThemeAdminPermission();
  const canManagePermissions = user?.role === 'ROOT' || hasAnyThemeAdminPermission(); // ROOT用户或拥有任何主题admin权限的用户可以管理权限
  const canCreateDeleteThemes = user?.role === 'ROOT'; // 只有ROOT用户可以创建和删除主题
  
  // 检查特定主题的权限
  const canManageTheme = (themeId: number) => {
    return user?.role === 'ROOT' || hasPermission('admin', themeId);
  };

  const canManageThemePermissions = (themeId: number) => {
    return user?.role === 'ROOT' || hasPermission('admin', themeId);
  };

  if (!canManageThemes) {
    return null;
  }

  const mainContent = (
    <div>
      {canCreateDeleteThemes && (
        <div style={{ marginBottom: 16 }}>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={() => setCreateModalVisible(true)}
          >
            创建主题
          </Button>
        </div>
      )}

        <List
          loading={loading}
          dataSource={themes.sort((a, b) => {
            // 当前主题置顶，其他按创建时间倒序排列
            if (a.id === currentThemeId) return -1;
            if (b.id === currentThemeId) return 1;
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
          })}
          renderItem={(theme) => (
            <List.Item>
              <Card 
                size="small" 
                style={{ 
                  width: '100%',
                  border: theme.id === currentThemeId ? '2px solid #1890ff' : undefined,
                  backgroundColor: theme.id === currentThemeId ? '#f0f8ff' : undefined
                }}
                actions={[
                  canManageTheme(theme.id) && (
                    <Button
                      key="edit"
                      type="text"
                      icon={<EditOutlined />}
                      onClick={() => openEditModal(theme)}
                    >
                      编辑
                    </Button>
                  ),
                  canManageThemePermissions(theme.id) && (
                    <Button
                      key="permission"
                      type="text"
                      icon={<UserOutlined />}
                      onClick={() => openPermissionModal(theme)}
                    >
                      权限管理
                    </Button>
                  ),
                  canCreateDeleteThemes && (
                    <Popconfirm
                      key="delete"
                      title="确定要删除这个主题吗？"
                      onConfirm={() => handleDeleteTheme(theme.id)}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                      >
                        删除
                      </Button>
                    </Popconfirm>
                  )
                ].filter(Boolean)}
              >
                <Card.Meta
                  title={<Space>
                    {theme.icon && <span>{theme.icon}</span>}
                    <Text strong>{theme.name}</Text>
                    {theme.id === currentThemeId && (
                      <span style={{ 
                        backgroundColor: '#1890ff', 
                        color: 'white', 
                        padding: '2px 6px', 
                        borderRadius: '4px', 
                        fontSize: '12px' 
                      }}>
                        当前主题
                      </span>
                    )}
                  </Space>}
                  description={
                    <div>
                      <Text type="secondary">{theme.prompt || '暂无描述'}</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        创建者: {theme.createdBy?.username} | 
                        创建时间: {new Date(theme.createdAt).toLocaleString()}
                      </Text>
                    </div>
                  }
                />
              </Card>
            </List.Item>
          )}
          locale={{ emptyText: '暂无主题' }}
        />
    </div>
  );

  if (embedded) {
    return (
      <>
        {mainContent}
        {/* 创建主题模态框 */}
        {canCreateDeleteThemes && (
          <Modal
            title="创建主题"
            open={createModalVisible}
            onCancel={() => {
              setCreateModalVisible(false);
              createForm.resetFields();
            }}
            footer={null}
          >
          <Form
            form={createForm}
            onFinish={handleCreateTheme}
            layout="vertical"
          >
            <Form.Item
              name="name"
              label="主题名称"
              rules={[{ required: true, message: '请输入主题名称' }]}
            >
              <Input placeholder="请输入主题名称" />
            </Form.Item>
            
            <Form.Item
              name="prompt"
              label="主题提示词"
              rules={[{ required: true, message: '请输入主题提示词' }]}
            >
              <TextArea rows={4} placeholder="请输入主题提示词" />
            </Form.Item>
            
            <Form.Item
              name="icon"
              label="主题图标"
            >
              <Input placeholder="请输入主题图标（emoji）" />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
              <Space>
                <Button onClick={() => {
                  setCreateModalVisible(false);
                  createForm.resetFields();
                }}>
                  取消
                </Button>
                <Button type="primary" htmlType="submit" loading={loading}>
                  创建
                </Button>
              </Space>
            </Form.Item>
          </Form>
          </Modal>
        )}

        {/* 编辑主题模态框 */}
        <Modal
          title="编辑主题"
          open={editModalVisible}
          onCancel={() => {
            setEditModalVisible(false);
            setEditingTheme(null);
            editForm.resetFields();
          }}
          footer={null}
        >
          <Form
            form={editForm}
            onFinish={handleEditTheme}
            layout="vertical"
          >
            <Form.Item
              name="name"
              label="主题名称"
              rules={[{ required: true, message: '请输入主题名称' }]}
            >
              <Input placeholder="请输入主题名称" />
            </Form.Item>
            
            <Form.Item
              name="prompt"
              label="主题提示词"
              rules={[{ required: true, message: '请输入主题提示词' }]}
            >
              <TextArea rows={4} placeholder="请输入主题提示词" />
            </Form.Item>
            
            <Form.Item
              name="icon"
              label="主题图标"
            >
              <Input placeholder="请输入主题图标（emoji）" />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
              <Space>
                <Button onClick={() => {
                  setEditModalVisible(false);
                  setEditingTheme(null);
                  editForm.resetFields();
                }}>
                  取消
                </Button>
                <Button type="primary" htmlType="submit" loading={loading}>
                  更新
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>

        {/* 权限管理模态框 */}
        {canManagePermissions && (
          <Modal
            title={`管理主题权限: ${selectedTheme?.name}`}
            open={permissionModalVisible}
            onCancel={() => {
              setPermissionModalVisible(false);
              setSelectedTheme(null);
              permissionForm.resetFields();
            }}
            footer={null}
          >
          <Form
            form={permissionForm}
            onFinish={handleGrantPermission}
            layout="vertical"
          >
            <Form.Item
              name="userId"
              label="选择用户"
              rules={[{ required: true, message: '请选择用户' }]}
            >
              <Select placeholder="请选择要授权的用户">
                {users.map(user => (
                  <Option key={user.id} value={user.id}>
                    {user.username} ({user.role})
                  </Option>
                ))}
              </Select>
            </Form.Item>
            
            <Form.Item
              name="permissionType"
              label="权限类型"
              rules={[{ required: true, message: '请选择权限类型' }]}
            >
              <Select placeholder="请选择权限类型">
                <Option value="WRITE">写入权限</Option>
                <Option value="ADMIN">管理员权限</Option>
              </Select>
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
              <Space>
                <Button onClick={() => {
                  setPermissionModalVisible(false);
                  setSelectedTheme(null);
                  permissionForm.resetFields();
                }}>
                  取消
                </Button>
                <Button type="primary" htmlType="submit" loading={loading}>
                  授予权限
                </Button>
              </Space>
            </Form.Item>
          </Form>
          </Modal>
        )}
      </>
    );
  }

  return (
    <>
      <Modal
        title="主题管理"
        open={visible}
        onCancel={onClose}
        footer={null}
        width={800}
        style={{ top: 20 }}
      >
        {mainContent}
      </Modal>

      {/* 创建主题模态框 */}
      <Modal
        title="创建主题"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          createForm.resetFields();
        }}
        footer={null}
      >
        <Form
          form={createForm}
          onFinish={handleCreateTheme}
          layout="vertical"
        >
          <Form.Item
            name="name"
            label="主题名称"
            rules={[{ required: true, message: '请输入主题名称' }]}
          >
            <Input placeholder="请输入主题名称" />
          </Form.Item>
          
          <Form.Item
            name="prompt"
            label="AI生成提示词"
            help="用于AI生成该主题笑话的特征描述"
          >
            <TextArea 
              rows={3} 
              placeholder="请输入用于AI生成笑话的提示词或特征描述" 
            />
          </Form.Item>
          
          <Form.Item
            name="icon"
            label="图标"
          >
            <Input placeholder="请输入图标（emoji或文字）" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => {
                setCreateModalVisible(false);
                createForm.resetFields();
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                创建
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑主题模态框 */}
      <Modal
        title="编辑主题"
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false);
          setEditingTheme(null);
          editForm.resetFields();
        }}
        footer={null}
      >
        <Form
          form={editForm}
          onFinish={handleEditTheme}
          layout="vertical"
        >
          <Form.Item
            name="name"
            label="主题名称"
            rules={[{ required: true, message: '请输入主题名称' }]}
          >
            <Input placeholder="请输入主题名称" />
          </Form.Item>
          
          <Form.Item
            name="prompt"
            label="AI生成提示词"
            help="用于AI生成该主题笑话的特征描述"
          >
            <TextArea 
              rows={3} 
              placeholder="请输入用于AI生成笑话的提示词或特征描述" 
            />
          </Form.Item>
          
          <Form.Item
            name="icon"
            label="图标"
          >
            <Input placeholder="请输入图标（emoji或文字）" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => {
                setEditModalVisible(false);
                setEditingTheme(null);
                editForm.resetFields();
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                保存
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 权限管理模态框 */}
      <Modal
        title={`权限管理 - ${selectedTheme?.name}`}
        open={permissionModalVisible}
        onCancel={() => {
          setPermissionModalVisible(false);
          setSelectedTheme(null);
          permissionForm.resetFields();
        }}
        footer={null}
      >
        <Form
          form={permissionForm}
          onFinish={handleGrantPermission}
          layout="vertical"
        >
          <Form.Item
            name="userId"
            label="选择用户"
            rules={[{ required: true, message: '请选择用户' }]}
          >
            <Select placeholder="请选择要授权的用户">
              {users.map(user => (
                <Option key={user.id} value={user.id}>
                  {user.username} ({user.role})
                </Option>
              ))}
            </Select>
          </Form.Item>
          
          <Form.Item
            name="permissionType"
            label="权限类型"
            rules={[{ required: true, message: '请选择权限类型' }]}
          >
            <Select placeholder="请选择权限类型">
              <Option value="WRITE">写入权限</Option>
              <Option value="ADMIN">管理员权限</Option>
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => {
                setPermissionModalVisible(false);
                setSelectedTheme(null);
                permissionForm.resetFields();
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                授予权限
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default ThemeManagementModal;
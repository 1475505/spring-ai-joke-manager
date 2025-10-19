import { useState, useEffect } from 'react';
import { Layout, Button, Space, Typography, Select, Input, message, Grid, Menu, Drawer, Tabs, Modal, Card, Dropdown, Form } from 'antd';
import { UserOutlined, PlusOutlined, LogoutOutlined, MenuOutlined, SearchOutlined, CommentOutlined, ThunderboltOutlined, AuditOutlined, SettingOutlined, EditOutlined, DownOutlined, AppstoreOutlined, DeleteOutlined, BulbOutlined } from '@ant-design/icons';
import { jokesAPI, themesAPI } from './services/api';
import { useAuthStore } from './store/authStore';
import { useThemeStore } from './store/themeStore';
import JokeCard from './components/JokeCard';
import LoginModal from './components/LoginModal';
import CreateJokeModal from './components/CreateJokeModal';
import AdminPanel from './components/AdminPanel';
import ProfileEditModal from './components/ProfileEditModal';
import OpenAIConfigModal from './components/OpenAIConfigModal';
import ThemeManagementModal from './components/ThemeManagementModal';
import AIGenerateModal from './components/AIGenerateModal';

import { ThemeComments } from './components/theme/ThemeComments';
import './App.css';

const { Header, Content, Sider } = Layout;
const { Title } = Typography;
// 移除不再使用的 Option 解构
// const { Option } = Select;
const { Search } = Input;
const { useBreakpoint } = Grid;

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

function App() {
  const { user, isLoggedIn, logout, hasPermission, hasAnyThemeAdminPermission, loadUserPermissions } = useAuthStore();
  const [jokes, setJokes] = useState<Joke[]>([]);
  const [loading, setLoading] = useState(false);
  const [loginModalVisible, setLoginModalVisible] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [adminPanelVisible, setAdminPanelVisible] = useState(false);
  const [currentTheme, setCurrentTheme] = useState('鸡煲笑话');
  const [sortBy, setSortBy] = useState('created');
  const [showMyJokes, setShowMyJokes] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('APPROVED');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [siderCollapsed, setSiderCollapsed] = useState(false);
  const [mobileDrawerVisible, setMobileDrawerVisible] = useState(false);
  const [randomJokeModalVisible, setRandomJokeModalVisible] = useState(false);
  const [randomJoke, setRandomJoke] = useState<Joke | null>(null);
  const [profileEditModalVisible, setProfileEditModalVisible] = useState(false);
  const [openAIConfigModalVisible, setOpenAIConfigModalVisible] = useState(false);
  const [aiGenerateModalVisible, setAiGenerateModalVisible] = useState(false);

  const screens = useBreakpoint();

  const { setCurrentTheme: setThemeInStore, themes, currentThemeId, loadThemes } = useThemeStore();

  const loadJokes = async () => {
    setLoading(true);
    try {
      let response;
      if (searchKeyword) {
        response = await jokesAPI.search(searchKeyword, currentThemeId?.toString());
      } else {
        response = await jokesAPI.getByTheme(currentThemeId?.toString() || '1', sortBy, 0, 5, showMyJokes, statusFilter);
      }
      setJokes(response.data.content || response.data);
    } catch (error) {
      message.error('加载笑话失败');
      console.error('Load jokes error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (value: string) => {
    setSearchKeyword(value);
  };


  const handleSingleRandomJoke = async () => {
    try {
      const response = await jokesAPI.getRandom(currentThemeId?.toString(), 1);
      const joke = response.data.content ? response.data.content[0] : response.data[0];
      setRandomJoke(joke);
      setRandomJokeModalVisible(true);
    } catch (error) {
      message.error('获取随机笑话失败');
      console.error('Single random joke error:', error);
    }
  };

  const handleLogout = () => {
    logout();
    message.success('已退出登录');
    window.location.reload(); // 登出后刷新整个页面
  };



  useEffect(() => {
    loadThemes();
  }, [loadThemes]);

  useEffect(() => {
    if (currentThemeId) {
      loadJokes();
    }
  }, [currentThemeId, sortBy, searchKeyword, showMyJokes, statusFilter]);

  // 当用户登录状态或主题改变时，设置默认的tab状态
  useEffect(() => {
    if (isLoggedIn && currentThemeId && hasPermission('write', currentThemeId)) {
      // 具有写入权限的用户默认显示全部笑话
      setShowMyJokes(false);
    }
  }, [isLoggedIn, currentThemeId, hasPermission]);

  useEffect(() => {
    setThemeInStore(currentTheme);
  }, [currentTheme, setThemeInStore]);

  useEffect(() => {
    if (isLoggedIn && user) {
      loadUserPermissions();
    }
  }, [isLoggedIn, user?.id, loadUserPermissions]);

  const isMobile = !screens.md;

  // 左侧主题列表使用固定排序（按ID排序），不受主题管理界面排序影响
  const themeOptions = themes
    .slice() // 创建副本避免修改原数组
    .sort((a, b) => a.id - b.id) // 按ID升序排列，保持固定顺序
    .map(theme => ({
      key: theme.name,
      label: theme.name
    }));

  const renderSiderContent = () => (
    <div style={{ padding: '16px 0' }}>
      <Title level={4} style={{ color: '#d4380d', textAlign: 'center', marginBottom: 24 }}>
        笑话主题
      </Title>
      <Menu
        mode="inline"
        selectedKeys={[currentTheme]}
        style={{ borderRight: 0, background: 'transparent' }}
        theme="light"
        items={themeOptions.map(theme => ({
          key: theme.key,
          label: theme.label,
          onClick: () => setCurrentTheme(theme.key)
        }))}
      />
    </div>
  );

  const renderOperations = () => (
    <Space direction="vertical" size="middle" style={{ width: '100%', marginBottom: 24 }}>
      {/* 第一行：筛选和搜索功能 */}
      <Space wrap size="middle">
        <Select
          value={sortBy}
          onChange={setSortBy}
          style={{ width: 120 }}
          size={isMobile ? 'small' : 'middle'}
          options={[
            { value: 'created', label: '最新' },
            { value: 'score', label: '评分' },
            { value: 'likes', label: '赞数' }
          ]}
        />

        {isLoggedIn && (
          <Select
            value={showMyJokes ? 'my' : 'all'}
            onChange={(value) => setShowMyJokes(value === 'my')}
            style={{ width: 120 }}
            size={isMobile ? 'small' : 'middle'}
            options={[
              { value: 'all', label: '全部笑话' },
              { value: 'my', label: '我的投稿' }
            ]}
          />
        )}

        {isLoggedIn && hasPermission('write', currentThemeId ?? undefined) && (
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 120 }}
            size={isMobile ? 'small' : 'middle'}
            placeholder="状态筛选"
            options={[
              { value: '', label: '全部状态' },
              { value: 'PENDING', label: '审核中' },
              { value: 'APPROVED', label: '已通过' },
              { value: 'REJECTED', label: '已拒绝' },
              { value: 'HIDDEN', label: '已隐藏' }
            ]}
          />
        )}

        <Search
          placeholder="搜索笑话内容"
          allowClear
          style={{ width: isMobile ? 200 : 300 }}
          onSearch={handleSearch}
          size={isMobile ? 'small' : 'middle'}
          enterButton={<SearchOutlined />}
        />
      </Space>
      
      {/* 第二行：操作按钮 */}
      <Space wrap size="middle">
        <Button
          onClick={handleSingleRandomJoke}
          size={isMobile ? 'small' : 'middle'}
          icon={<ThunderboltOutlined />}
        >
          随机笑话
        </Button>
        
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalVisible(true)}
          size={isMobile ? 'small' : 'middle'}
        >
          投稿笑话
        </Button>
        
        <Button
          icon={<BulbOutlined />}
          onClick={() => setAiGenerateModalVisible(true)}
          size={isMobile ? 'small' : 'middle'}
        >
          AI生成
        </Button>
        
        {/* 需要权限校验的按钮 */}
        {hasPermission('write', currentThemeId ?? undefined) && (
          <Button
            icon={<AuditOutlined />}
            onClick={() => setAdminPanelVisible(true)}
            size={isMobile ? 'small' : 'middle'}
          >
            审批管理
          </Button>
        )}
      </Space>
    </Space>
  );

  const renderUserActions = () => {
    const userMenuItems = [
      {
        key: 'profile',
        icon: <EditOutlined />,
        label: '个人信息',
        onClick: () => setProfileEditModalVisible(true),
      },
      {
        type: 'divider' as const,
      },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出',
        onClick: handleLogout,
      },
    ];

    return (
      <Space size={isMobile ? 'small' : 'middle'}>
        <Button
          icon={<SettingOutlined />}
          onClick={() => setOpenAIConfigModalVisible(true)}
          size={isMobile ? 'small' : 'middle'}
          title="OpenAI配置"
        >
          {!isMobile && 'OpenAI配置'}
        </Button>
        
        {isLoggedIn ? (
          <>
            {!isMobile && (
              <span style={{ color: 'white' }}>
                欢迎，{user?.username} ({user?.role})
              </span>
            )}
            <Dropdown
              menu={{ items: userMenuItems }}
              placement="bottomRight"
              trigger={['click']}
            >
              <Button
                icon={<UserOutlined />}
                size={isMobile ? 'small' : 'middle'}
              >
                {!isMobile && user?.username} <DownOutlined />
              </Button>
            </Dropdown>
          </>
        ) : (
          <Button
            type="primary"
            icon={<UserOutlined />}
            onClick={() => setLoginModalVisible(true)}
            size={isMobile ? 'small' : 'middle'}
          >
            {isMobile ? '' : '登录'}
          </Button>
        )}
      </Space>
    );
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: isMobile ? '0 16px' : '0 50px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          {isMobile && (
            <Button
              type="text"
              icon={<MenuOutlined />}
              onClick={() => setMobileDrawerVisible(true)}
              style={{ color: 'white', marginRight: 16 }}
            />
          )}
          <Title
            level={isMobile ? 4 : 3}
            style={{ color: 'white', margin: 0 }}
          >
            {isMobile ? '笑话平台' : 'AI笑话管理平台'}
          </Title>
        </div>
        {renderUserActions()}
      </Header>

      <Layout>
        {!isMobile && (
          <Sider
            width={250}
            theme="light"
            collapsible
            collapsed={siderCollapsed}
            onCollapse={setSiderCollapsed}
            style={{ background: '#fff', borderRight: '1px solid #f0f0f0' }}
          >
            {renderSiderContent()}
          </Sider>
        )}

        <Content style={{ padding: isMobile ? '16px' : '24px' }}>
          {renderOperations()}

          <Tabs
            defaultActiveKey="jokes"
            items={[
              {
                key: 'jokes',
                label: '笑话列表',
                children: (
                  <div 
                    className="joke-list" 
                    style={{ 
                      marginTop: 16,
                      display: 'grid',
                      gridTemplateColumns: isMobile 
                        ? '1fr' 
                        : 'repeat(auto-fill, minmax(350px, 1fr))',
                      gap: isMobile ? '12px' : '20px',
                      alignItems: 'start'
                    }}
                  >
                    {jokes.map((joke) => (
                      <JokeCard 
                        key={joke.id} 
                        joke={joke} 
                        onUpdate={loadJokes} 
                        showStatus={showMyJokes || hasPermission('write', currentThemeId ?? undefined)} 
                      />
                    ))}
                  </div>
                ),
              },
              {
                key: 'comments',
                label: (
                  <span>
                    <CommentOutlined /> 主题讨论
                  </span>
                ),
                children: <ThemeComments theme={currentTheme} />,
              },
              ...(user?.role === 'ROOT' || hasAnyThemeAdminPermission() ? [{
                key: 'theme-management',
                label: (
                  <span>
                    <AppstoreOutlined /> 主题管理
                  </span>
                ),
                children: (
                  <ThemeManagementModal 
                    visible={true} 
                    onClose={() => {}} 
                    embedded={true} 
                  />
                ),
              }] : [])
            ]}
          />
          {jokes.length === 0 && !loading && (
            <div style={{
              textAlign: 'center',
              padding: isMobile ? '30px 0' : '50px 0',
              color: '#999'
            }}>
              {searchKeyword ? '没有找到相关笑话' : '暂无笑话数据'}
            </div>
          )}
        </Content>
      </Layout>

      {/* Mobile Drawer for Theme Selection */}
      <Drawer
        title="笑话主题"
        placement="left"
        onClose={() => setMobileDrawerVisible(false)}
        open={mobileDrawerVisible}
        width={250}
      >
        <Menu
          mode="inline"
          selectedKeys={[currentTheme]}
          items={themeOptions.map(theme => ({
          key: theme.key,
          label: theme.label,
          onClick: () => {
            setCurrentTheme(theme.key);
            setMobileDrawerVisible(false);
          }
        }))}
        />
      </Drawer>

      <LoginModal
        visible={loginModalVisible}
        onClose={() => setLoginModalVisible(false)}
      />

      <CreateJokeModal
        visible={createModalVisible}
        onClose={() => setCreateModalVisible(false)}
        onSuccess={loadJokes}
      />

      <AdminPanel
        visible={adminPanelVisible}
        onClose={() => setAdminPanelVisible(false)}
        currentThemeId={currentThemeId || 1}
        onSuccess={() => {
          loadJokes();
          setAdminPanelVisible(false);
        }}
      />

      {/* 随机笑话弹出对话框 */}
      <Modal
        title={(
          <span>
            <ThunderboltOutlined style={{ marginRight: 8 }} />
            随机笑话
          </span>
        )}
        open={randomJokeModalVisible}
        onCancel={() => setRandomJokeModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setRandomJokeModalVisible(false)}>
            关闭
          </Button>,
          <Button key="another" type="primary" onClick={handleSingleRandomJoke}>
            再来一个
          </Button>,
        ]}
        width={600}
      >
        {randomJoke && (
          <Card
            title={randomJoke.title || '无标题'}
            extra={(
              <Space>
                <span>主题: {randomJoke.theme.name}</span>
                <span>评分: {randomJoke.scores.finalScore}</span>
              </Space>
            )}
            style={{ border: 'none' }}
          >
            <div style={{ fontSize: '16px', lineHeight: '1.6', marginBottom: '16px' }}>
              {randomJoke.content}
            </div>
            <div style={{ color: '#666', fontSize: '14px' }}>
              <Space>
                <span>作者: {randomJoke.author.username}</span>
                <span>浏览: {randomJoke.statistics.viewCount}</span>
                <span>点赞: {randomJoke.statistics.likeCount}</span>
              </Space>
            </div>
          </Card>
        )}
      </Modal>

      <ProfileEditModal
        visible={profileEditModalVisible}
        onClose={() => setProfileEditModalVisible(false)}
        onSuccess={() => {
          setProfileEditModalVisible(false);
          message.success('个人信息更新成功');
        }}
      />

      <OpenAIConfigModal
        visible={openAIConfigModalVisible}
        onClose={() => setOpenAIConfigModalVisible(false)}
        onSuccess={() => {
          setOpenAIConfigModalVisible(false);
          message.success('OpenAI配置保存成功');
        }}
      />

      <AIGenerateModal
        visible={aiGenerateModalVisible}
        onClose={() => setAiGenerateModalVisible(false)}
        onSuccess={() => {
          setAiGenerateModalVisible(false);
          loadJokes();
        }}
        themeId={currentThemeId ?? undefined}
      />

    </Layout>
  );
}

export default App;

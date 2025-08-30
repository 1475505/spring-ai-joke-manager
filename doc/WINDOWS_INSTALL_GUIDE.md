# Windows开发环境安装指南

## 1. 安装Java 21 (OpenJDK)

### 方法一：使用Oracle JDK
1. 访问 [Oracle JDK 21下载页面](https://www.oracle.com/java/technologies/downloads/#java21)
2. 选择Windows x64 Installer
3. 下载并运行安装程序
4. 按照向导完成安装

### 方法二：使用OpenJDK (推荐)
1. 访问 [Adoptium](https://adoptium.net/) 或 [Microsoft OpenJDK](https://learn.microsoft.com/java/openjdk/download)
2. 下载Windows x64版本
3. 运行安装程序

### 验证安装
```bash
java -version
javac -version
```

## 2. 安装Maven

### 方法一：使用Chocolatey (推荐)
```bash
# 以管理员身份运行PowerShell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# 安装Maven
choco install maven -y
```

### 方法二：手动安装
1. 访问 [Maven官网](https://maven.apache.org/download.cgi)
2. 下载Binary zip archive (apache-maven-3.9.x-bin.zip)
3. 解压到 `C:\Program Files\Apache\maven`
4. 配置环境变量：
   - 新建系统变量 `MAVEN_HOME`: `C:\Program Files\Apache\maven\apache-maven-3.9.x`
   - 编辑系统变量 `Path`，添加 `%MAVEN_HOME%\bin`

### 验证安装
```bash
mvn -version
```

## 3. 安装Node.js和npm

### 使用Node.js官方安装程序
1. 访问 [Node.js官网](https://nodejs.org/)
2. 下载LTS版本 (推荐v20.x)
3. 运行安装程序，确保勾选"npm package manager"

### 使用Chocolatey
```bash
choco install nodejs-lts -y
```

### 验证安装
```bash
node -v
npm -v
```

## 4. 环境变量配置

### 检查环境变量
确保以下路径已添加到系统环境变量 `Path`：
- `C:\Program Files\Java\jdk-21\bin`
- `C:\Program Files\Apache\maven\apache-maven-3.9.x\bin`
- `C:\Users\[用户名]\AppData\Roaming\npm`

### 设置MAVEN_OPTS (可选)
```bash
# 设置Maven内存限制
setx MAVEN_OPTS "-Xmx1024m -XX:MaxPermSize=256m"
```

## 5. 安装开发工具

### IntelliJ IDEA (推荐)
1. 访问 [JetBrains官网](https://www.jetbrains.com/idea/download/)
2. 下载Community版或Ultimate版
3. 安装并配置JDK和Maven路径

### VS Code
1. 访问 [VS Code官网](https://code.visualstudio.com/)
2. 下载Windows版本
3. 安装Java Extension Pack和Spring Boot Extension Pack

## 6. 项目依赖安装

### 后端依赖 (在项目根目录)
```bash
# 安装Maven依赖
mvn clean install

# 运行Spring Boot应用
mvn spring-boot:run
```

### 前端依赖 (在项目根目录)
```bash
# 安装npm依赖
npm install

# 启动开发服务器
npm run dev
```

## 7. 数据库安装

### PostgreSQL
1. 访问 [PostgreSQL官网](https://www.postgresql.org/download/windows/)
2. 下载并安装PostgreSQL 16
3. 记住设置的密码

### Redis
1. 访问 [Redis Windows下载](https://github.com/tporadowski/redis/releases)
2. 下载Redis-x64-5.x.x.msi
3. 运行安装程序

## 8. 常见问题解决

### Java版本冲突
```bash
# 检查Java版本
where java
java -version

# 如果版本不正确，检查环境变量优先级
```

### Maven下载慢
编辑 `C:\Users\[用户名]\.m2\settings.xml`：
```xml
<settings>
    <mirrors>
        <mirror>
            <id>aliyunmaven</id>
            <mirrorOf>*</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
```

### npm权限问题
```bash
# 以管理员身份运行PowerShell
npm config set registry https://registry.npmmirror.com
npm install -g npm@latest
```

## 9. 一键安装脚本

创建 `install-dev-env.bat`：
```batch
@echo off
echo 正在安装开发环境...
echo.

REM 检查Chocolatey
where choco >nul 2>nul
if %errorlevel% neq 0 (
    echo 正在安装Chocolatey...
    powershell -Command "Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))"
)

echo 正在安装Java 21...
choco install openjdk21 -y

echo 正在安装Maven...
choco install maven -y

echo 正在安装Node.js...
choco install nodejs-lts -y

echo 正在安装Git...
choco install git -y

echo.
echo 安装完成！请重启终端并运行以下命令验证：
echo java -version
echo mvn -version
echo node -v
echo npm -v
pause
```

## 10. 验证环境

运行以下命令验证所有组件：
```bash
# 验证Java
java -version

# 验证Maven
mvn -version

# 验证Node.js
node -v

# 验证npm
npm -v

# 验证Git
git --version
```

## 11. 下一步操作

1. 克隆项目代码
2. 按照 `DEVELOPMENT.md` 配置数据库
3. 运行后端：`mvn spring-boot:run`
4. 运行前端：`npm run dev`
5. 访问 http://localhost:5173

---

**提示**：建议先安装Chocolatey包管理器，可以大大简化安装过程。
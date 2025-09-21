# 嵌入模型文件

此目录用于存放ONNX格式的嵌入模型文件。

## 模型下载

由于模型文件较大（约23MB），需要手动下载：

1. 下载 all-MiniLM-L6-v2 ONNX模型：
   ```bash
   wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx -O all-MiniLM-L6-v2.onnx
   ```

2. 或者从以下链接下载：
   - https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/main/onnx

## 模型信息

- **模型名称**: all-MiniLM-L6-v2
- **向量维度**: 384
- **支持语言**: 多语言（包括中文）
- **文件大小**: 约23MB
- **格式**: ONNX

## 临时解决方案

如果无法下载模型文件，系统会使用随机向量作为降级处理，确保应用正常运行。
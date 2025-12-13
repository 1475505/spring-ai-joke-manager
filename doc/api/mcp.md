# MCP/自然语言操作 API

TODO

## 概述

MCP（Model Context Protocol）/自然语言操作模块允许ROOT用户通过自然语言直接操作数据库。通过spring ai提供的@Tool注解和LLM的toolCall能力，将已有的service的能力用@Tool注解，给出tools列表，让模型自动调用llm来处理，实现智能化的自然语言操作笑话的增删和查询。

root用户点击前端的气泡即可对话，会展示结果和工具tools调用。

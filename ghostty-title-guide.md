# Ghostty 终端标题配置指南

## 方法一：使用 Shell Integration（推荐）

Ghostty 内置 shell integration 功能，可以自动设置终端标题为当前工作目录。

### 配置文件位置
`~/.config/ghostty/config`

### 配置内容
```ini
# 自动检测 shell 并注入集成脚本
shell-integration = detect

# 启用的功能（用逗号分隔）
# - cursor: 在提示符处将光标设为 bar 样式
# - title: 通过 shell 集成自动设置窗口标题（显示当前工作目录）
# - sudo: 设置 sudo wrapper 以保留 terminfo
# - ssh-env: SSH 时自动转换 TERM 变量
# - ssh-terminfo: 自动在远程主机安装 terminfo
# - path: 将 Ghostty 添加到 PATH
shell-integration-features = cursor,title
```

启用 `title` 后，标题会自动显示**完整路径**，如 `/Users/xxx/Documents/shuranArt`

---

## 方法二：强制固定标题

如果你想让所有窗口显示固定标题：

```ini
# 强制设置窗口标题（会忽略程序发送的标题设置）
title = My Terminal

# 如果想要空白标题，用空格
title = " "
```

**注意**：设置 `title` 后，shell-integration 的 title 功能和程序的标题设置都会被忽略。

---

## 方法三：显示短目录名（仅目录名，不含路径）

Shell integration 的 title 功能显示完整路径。如果只想显示目录名（如 `shuranArt`），需要在 `~/.zshrc` 中添加：

```bash
# 设置终端标题为当前目录名（不含完整路径）
precmd() {
  print -Pn "\e]0;${PWD##*/}\a"
}
```

这会覆盖 shell-integration 的标题设置。

如果想显示 `~/Documents/shuranArt` 格式（用 ~ 替代 home 目录）：
```bash
precmd() {
  print -Pn "\e]0;%~\a"
}
```

---

## 方法四：手动修改标题

Ghostty 支持通过命令面板手动修改标题：

- **修改标签页标题**: `Cmd+Shift+P` → 搜索 "Change Tab Title"
- **修改终端标题**: `Cmd+Shift+P` → 搜索 "Change Terminal Title"

也可以绑定快捷键：
```ini
keybind = cmd+shift+t=prompt_tab_title
keybind = cmd+shift+y=prompt_surface_title
```

---

## 其他相关配置

### 窗口副标题（仅 GTK/Linux）
```ini
# 在标题下方显示工作目录作为副标题
window-subtitle = working-directory

# 禁用副标题
window-subtitle = false
```

### 标题栏字体
```ini
# 自定义标题栏字体
window-title-font-family = SF Pro Display
```

### 标题栏背景/前景色（仅 GTK，需 window-theme = ghostty）
```ini
window-theme = ghostty
window-titlebar-background = #1e1e2e
window-titlebar-foreground = #cdd6f4
```

---

## 配置生效

修改配置后：
1. **热重载**: 按 `Cmd+Shift+,` 或在命令面板中选择 "Reload Config"
2. **完全重启**: 部分配置需要重启 Ghostty 才能生效

---

## 常见问题

### Q: 标题不更新？
检查是否有其他程序或配置在设置标题：
- 检查 `~/.zshrc` 中是否有 `precmd` 函数
- 检查是否设置了固定的 `title` 配置
- 尝试 `shell-integration = none` 排查冲突

### Q: 如何查看所有配置选项？
```bash
ghostty +show-config --default --docs
```

### Q: 如何只查看 title 相关配置？
```bash
ghostty +show-config --default --docs | grep -A 20 "^title"
```

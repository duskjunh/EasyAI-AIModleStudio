package com.ai;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * AI 聊天面板
 * 支持 OpenAI 兼容 API（DeepSeek、通义千问、智谱等）
 */
public class ChatPanel extends JPanel {

    private JTextField apiUrlField;
    private JPasswordField apiKeyField;
    private JTextField modelField;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendBtn;
    private JButton clearBtn;

    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";
    private String apiKey = "";
    private String modelName = "deepseek-chat";

    public ChatPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("AI 聊天"));

        // 顶部：API 配置
        JPanel configPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("API 配置"));

        configPanel.add(new JLabel("API 地址:"));
        apiUrlField = new JTextField(apiUrl);
        configPanel.add(apiUrlField);

        configPanel.add(new JLabel("API Key:"));
        apiKeyField = new JPasswordField();
        configPanel.add(apiKeyField);

        configPanel.add(new JLabel("模型名:"));
        modelField = new JTextField(modelName);
        configPanel.add(modelField);

        add(configPanel, BorderLayout.NORTH);

        // 中间：聊天记录
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        add(chatScroll, BorderLayout.CENTER);

        // 底部：输入框 + 按钮
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        inputField = new JTextField();
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        inputField.addActionListener(e -> sendMessage());
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        sendBtn = new JButton("发送");
        sendBtn.addActionListener(e -> sendMessage());
        clearBtn = new JButton("清空");
        clearBtn.addActionListener(e -> chatArea.setText(""));
        btnPanel.add(sendBtn);
        btnPanel.add(clearBtn);
        inputPanel.add(btnPanel, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) return;

        // 读取配置
        apiUrl = apiUrlField.getText().trim();
        apiKey = new String(apiKeyField.getPassword()).trim();
        modelName = modelField.getText().trim();

        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先输入 API Key");
            return;
        }

        // 显示用户消息
        chatArea.append("你: " + userInput + "\n\n");
        inputField.setText("");
        sendBtn.setEnabled(false);
        chatArea.append("AI: 正在思考...\n");

        final String finalInput = userInput;

        // 后台线程发送请求
        new Thread(() -> {
            try {
                String response = callAPI(finalInput);
                SwingUtilities.invokeLater(() -> {
                    // 删掉"正在思考..."
                    String text = chatArea.getText();
                    int idx = text.lastIndexOf("AI: 正在思考...\n");
                    if (idx != -1) {
                        chatArea.setText(text.substring(0, idx));
                    }
                    chatArea.append("AI: " + response + "\n\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    sendBtn.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    String text = chatArea.getText();
                    int idx = text.lastIndexOf("AI: 正在思考...\n");
                    if (idx != -1) {
                        chatArea.setText(text.substring(0, idx));
                    }
                    chatArea.append("AI: [错误] " + e.getMessage() + "\n\n");
                    sendBtn.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * 调用 OpenAI 兼容 API
     */
    private String callAPI(String userMessage) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // 构建 JSON 请求体
        String jsonBody = buildRequestJson(userMessage);

        // 发送
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            InputStream es = conn.getErrorStream();
            String errMsg = readStream(es);
            throw new IOException("HTTP " + code + ": " + errMsg);
        }

        // 读取响应
        String response = readStream(conn.getInputStream());
        return extractContent(response);
    }

    /**
     * 构建请求 JSON
     */
    private String buildRequestJson(String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(modelName)).append("\",");
        sb.append("\"messages\":[");
        sb.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");
        sb.append("],");
        sb.append("\"temperature\":0.7");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 从响应 JSON 中提取 content 字段
     */
    private String extractContent(String json) {
        // 简单解析，找 "content":"..."
        int idx = json.indexOf("\"content\"");
        if (idx == -1) return json;

        int start = json.indexOf("\"", idx + 9);
        if (start == -1) return json;
        start++;

        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            baos.write(buf, 0, len);
        }
        return baos.toString("UTF-8");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
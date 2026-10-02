package com.ai;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 * 统一读写 model_studio_config.json
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "model_studio_config.json";
    private static Map<String, Object> config = null;

    /**
     * 加载配置
     */
    public static Map<String, Object> load() {
        if (config != null) return config;

        config = new HashMap<>();
        config.put("model_dir", System.getProperty("user.dir"));
        config.put("language", "zh_CN");

        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try {
                String json = new String(
                        java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
                Map<String, Object> loaded = JsonParser.asObject(
                        new JsonParser(json).parse());
                for (Map.Entry<String, Object> e : loaded.entrySet()) {
                    config.put(e.getKey(), e.getValue());
                }
                System.out.println("[Config] 已加载: " + CONFIG_FILE);
            } catch (Exception e) {
                System.out.println("[Config] 加载失败: " + e.getMessage());
            }
        }

        return config;
    }

    /**
     * 保存配置
     */
    public static boolean save() {
        if (config == null) return false;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Object> e : config.entrySet()) {
                sb.append("  \"").append(e.getKey()).append("\": ");
                Object v = e.getValue();
                if (v instanceof String) {
                    sb.append("\"").append(v).append("\"");
                } else {
                    sb.append(v);
                }
                if (++i < config.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("}\n");

            java.nio.file.Files.write(new File(CONFIG_FILE).toPath(),
                    sb.toString().getBytes("UTF-8"));

            System.out.println("[Config] 已保存: " + CONFIG_FILE);
            return true;
        } catch (Exception e) {
            System.out.println("[Config] 保存失败: " + e.getMessage());
            return false;
        }
    }

    public static String get(String key, String defaultValue) {
        Map<String, Object> c = load();
        Object v = c.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    public static void set(String key, Object value) {
        Map<String, Object> c = load();
        c.put(key, value);
    }

    public static String getModelDir() {
        return get("model_dir", System.getProperty("user.dir"));
    }

    public static void setModelDir(String dir) {
        set("model_dir", dir);
        save();
    }

    public static String getLanguage() {
        return get("language", "zh_CN");
    }

    public static void setLanguage(String lang) {
        set("language", lang);
        save();
    }
}
package com.ai;

import java.io.*;
import java.util.Properties;

public class Lang {

    private static Properties props = new Properties();
    private static String currentLang = "zh_CN";

    public static void load(String langCode) {
        currentLang = langCode;
        props.clear();

        File external = new File("lang/" + langCode + ".lang");
        if (external.exists()) {
            try (InputStreamReader isr = new InputStreamReader(
                    new FileInputStream(external), "UTF-8")) {
                props.load(isr);
                System.out.println("[Lang] 已从外部加载: " + external.getPath());
                return;
            } catch (IOException e) {
                System.out.println("[Lang] 外部加载失败: " + e.getMessage());
            }
        }

        try (InputStream is = Lang.class.getResourceAsStream("/lang/" + langCode + ".lang")) {
            if (is != null) {
                props.load(new InputStreamReader(is, "UTF-8"));
                System.out.println("[Lang] 已从 jar 内部加载: " + langCode);
                return;
            }
        } catch (IOException e) {
            System.out.println("[Lang] 内部加载失败: " + e.getMessage());
        }

        System.out.println("[Lang] 未找到语言文件: " + langCode);
    }

    public static String get(String key) {
        return props.getProperty(key, key);
    }

    public static String get(String key, Object... args) {
        return String.format(get(key), args);
    }

    public static String getCurrentLang() {
        return currentLang;
    }
}
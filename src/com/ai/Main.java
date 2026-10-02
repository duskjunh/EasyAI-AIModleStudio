package com.ai;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 从配置读语言
        String langCode = ConfigManager.getLanguage();
        Lang.load(langCode);

        System.out.println("[Lang] 当前语言: " + langCode);

        SwingUtilities.invokeLater(() -> new GUI());
    }
}
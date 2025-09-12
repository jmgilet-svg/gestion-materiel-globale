package com.materiel.suite.client.ui.setup;

import com.materiel.suite.client.config.AppConfig;

import javax.swing.*;

public class ModeChoiceDialog {
    public static AppConfig chooseMode(AppConfig cfg) {
        String[] options = {"Mock JSON", "Backend API"};
        int res = JOptionPane.showOptionDialog(null, "Choisir le mode de fonctionnement", "Mode",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (res == 1) {
            cfg.setMode("backend");
        } else {
            cfg.setMode("mock");
        }
        return cfg;
    }
}

package com.materiel.suite.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.MainFrame;
import com.materiel.suite.client.ui.setup.ModeChoiceDialog;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            AppConfig cfg = AppConfig.load();
            cfg = ModeChoiceDialog.chooseMode(cfg);
            ServiceFactory.init(cfg);
            new MainFrame().setVisible(true);
            System.out.println("CLIENT_READY_UI");
        });
    }
}

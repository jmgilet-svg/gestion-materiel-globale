package com.materiel.suite.client.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        super("Gestion Materiel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        JLabel label = new JLabel("CLIENT_READY_UI", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}

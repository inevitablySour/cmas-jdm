package com.cmas.main.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ResultsDashboardPanel extends JPanel {
    private final DefaultListModel<String> listModel;
    private final JList<String> resultList;

    public ResultsDashboardPanel(List<String> previousScores) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("Previous CMAS Scores", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        listModel = new DefaultListModel<>();
        previousScores.forEach(listModel::addElement);

        resultList = new JList<>(listModel);
        resultList.setFont(new Font("SansSerif", Font.PLAIN, 14));

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(resultList), BorderLayout.CENTER);
    }

    public void refreshScores(List<String> newScores) {
        listModel.clear();
        newScores.forEach(listModel::addElement);
    }
}
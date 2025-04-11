package com.cmas.main.gui.patient;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ResultsDashboardPanel extends JPanel {
    private final DefaultListModel<String> listModel;
    private final JList<String> resultList;
    private final JPanel chartContainer;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");

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

        chartContainer = new JPanel();
        chartContainer.setLayout(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        chartContainer.setBackground(Color.WHITE);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(resultList), BorderLayout.CENTER);
        add(chartContainer, BorderLayout.SOUTH);

        buildChartFromScores(previousScores);
    }

    public void refreshScores(List<String> newScores) {
        listModel.clear();
        newScores.stream()
                .sorted((a, b) -> {
                    String dateA = a.split("\\|")[0].split(":")[1].trim();
                    String dateB = b.split("\\|")[0].split(":")[1].trim();
                    return dateB.compareTo(dateA); // DESCENDING
                })
                .forEach(listModel::addElement);
        buildChartFromScores(newScores);
    }

    private void buildChartFromScores(List<String> scores) {
        chartContainer.removeAll();

        if (scores.size() < 2) {
            chartContainer.revalidate();
            chartContainer.repaint();
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // ✅ Step 1: Parse entries into a sortable structure
        List<String[]> parsed = scores.stream()
                .map(s -> {
                    try {
                        String[] parts = s.split("\\|");
                        String date = parts[0].split(":")[1].trim();
                        String value = parts[2].split(":")[1].trim();
                        return new String[]{date, value};
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(e -> e != null)
                .sorted((a, b) -> a[0].compareTo(b[0]))
                .toList();


        for (String[] entry : parsed) {
            try {
                int value = Integer.parseInt(entry[1]);
                dataset.addValue(value, "CMAS Score", entry[0]);
            } catch (NumberFormatException ignored) {}
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "CMAS Score Progress",
                "Date",
                "Score",
                dataset
        );

        CategoryPlot plot = chart.getCategoryPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultLinesVisible(true);
        renderer.setDefaultShape(new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());

        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 300));
        chartPanel.setDisplayToolTips(true);

        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }
}
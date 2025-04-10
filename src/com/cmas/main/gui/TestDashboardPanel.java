package com.cmas.main.gui;

import com.cmas.main.cmas.CMASTest;
import com.cmas.main.dao.DatabaseController;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestDashboardPanel extends JPanel {
    private final Map<String, Integer> testScores = new LinkedHashMap<>();
    private final int totalTests = 14;
    private final Runnable onAllTestsCompleted;
    private DatabaseController db = new DatabaseController();

    public TestDashboardPanel(Runnable onAllTestsCompleted) throws SQLException {
        this.onAllTestsCompleted = onAllTestsCompleted;

        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        JLabel title = new JLabel("CMAS Test Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
        grid.setBackground(new Color(245, 245, 245));
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Define test names and matching methods
        List<TestCard> testCards = List.of(
                new TestCard("1. Head Elevation", wrap("1. Head Elevation", CMASTest::runHeadElevationTest)),
                new TestCard("2. Leg Raise", wrap("2. Leg Raise", CMASTest::runLegRaiseTest)),
                new TestCard("3. Leg Lift Duration", wrap("3. Leg Lift Duration", CMASTest::runStraightLegLiftTest)),
                new TestCard("4. Supine to Prone", wrap("4. Supine to Prone", CMASTest::runSupineToProneTest)),
                new TestCard("5. Sit-Ups", wrap("5. Sit-Ups", CMASTest::runSitUpTest)),
                new TestCard("6. Supine to Sit", wrap("6. Supine to Sit", CMASTest::runSupineToSitTest)),
                new TestCard("7. Arm Raise / Straighten", wrap("7. Arm Raise / Straighten", CMASTest::runArmRaiseTest)),
                new TestCard("8. Arm Raise / Duration", wrap("8. Arm Raise / Duration", CMASTest::runArmRaiseDurationTest)),
                new TestCard("9. Floor Sit", wrap("9. Floor Sit", CMASTest::runFloorSitTest)),
                new TestCard("10. All Fours", wrap("10. All Fours", CMASTest::runAllFoursTest)),
                new TestCard("11. Floor Rise", wrap("11. Floor Rise", CMASTest::runFloorRiseTest)),
                new TestCard("12. Chair Rise", wrap("12. Chair Rise", CMASTest::runChairRiseTest)),
                new TestCard("13. Stool Step", wrap("13. Stool Step", CMASTest::runStoolStepTest)),
                new TestCard("14. Pick-Up Object", wrap("14. Pick-Up Object", CMASTest::runPickUpObjectTest))
        );

        testCards.forEach(grid::add);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(grid), BorderLayout.CENTER);
    }

    // Wrapper to intercept score and check for completion
    private TestFunction wrap(String label, TestFunction originalTest) {
        return () -> {
            int score = originalTest.run();
            testScores.put(label, score);

            if (testScores.size() == totalTests) {
                int total = testScores.values().stream().mapToInt(Integer::intValue).sum();
                JOptionPane.showMessageDialog(this,
                        "All tests completed!\nTotal CMAS Score: " + total,
                        "CMAS Summary", JOptionPane.INFORMATION_MESSAGE);

                // Save score
                db.saveCMASScore(total);

                // Trigger UI refresh
                if (onAllTestsCompleted != null) {
                    onAllTestsCompleted.run();
                }
            }

            return score;
        };
    }

    public int getTotalScore() {
        return testScores.values().stream().mapToInt(Integer::intValue).sum();
    }
}
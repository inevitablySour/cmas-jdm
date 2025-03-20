package com.cmas.main.pose;

import java.util.Map;

public class CMASScorer {

    /**
     * Calculate the CMAS score based on extracted pose data.
     * @param poseData Map of pose keypoints (x, y, z) coordinates.
     * @return CMAS score
     */
    public static int calculateScore(Map<String, Map<String, Double>> poseData) {
        int score = 0;

        // Example: Check if the patient can lift their arms
        score += assessArmLifting(poseData);
        score += assessLegLifting(poseData);
        score += assessStandingUp(poseData);
        score += assessKneeling(poseData);
        score += assessObjectPickup(poseData);

        return score;
    }

    /**
     * Assess arm lifting ability based on shoulder and wrist height difference.
     */
    private static int assessArmLifting(Map<String, Map<String, Double>> poseData) {
        try {
            double shoulderY = poseData.get("point_11").get("y"); // Left shoulder
            double wristY = poseData.get("point_15").get("y"); // Left wrist

            double shoulderY2 = poseData.get("point_12").get("y"); // Right shoulder
            double wristY2 = poseData.get("point_16").get("y"); // Right wrist

            // If wrist is higher than shoulder (lower y-value in image), full score (5)
            if (wristY < shoulderY && wristY2 < shoulderY2) {
                return 5;
            }
            return 2; // Partial movement
        } catch (Exception e) {
            return 0; // No valid data
        }
    }

    /**
     * Assess leg lifting ability based on hip and ankle height.
     */
    private static int assessLegLifting(Map<String, Map<String, Double>> poseData) {
        try {
            double hipY = poseData.get("point_23").get("y"); // Left hip
            double ankleY = poseData.get("point_27").get("y"); // Left ankle

            double hipY2 = poseData.get("point_24").get("y"); // Right hip
            double ankleY2 = poseData.get("point_28").get("y"); // Right ankle

            // If ankle is lifted above hip height, full score (5)
            if (ankleY < hipY && ankleY2 < hipY2) {
                return 5;
            }
            return 2; // Partial movement
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Assess ability to stand up from the floor based on hip and knee positions.
     */
    private static int assessStandingUp(Map<String, Map<String, Double>> poseData) {
        try {
            double kneeY = poseData.get("point_25").get("y"); // Left knee
            double kneeY2 = poseData.get("point_26").get("y"); // Right knee
            double hipY = poseData.get("point_23").get("y"); // Left hip
            double hipY2 = poseData.get("point_24").get("y"); // Right hip

            // If hip is significantly above knee (indicating standing), full score (10)
            if (hipY < kneeY && hipY2 < kneeY2) {
                return 10;
            }
            return 4; // Partial movement
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Assess ability to kneel and maneuver on hands and knees.
     */
    private static int assessKneeling(Map<String, Map<String, Double>> poseData) {
        try {
            double kneeY = poseData.get("point_25").get("y"); // Left knee
            double wristY = poseData.get("point_15").get("y"); // Left wrist

            double kneeY2 = poseData.get("point_26").get("y"); // Right knee
            double wristY2 = poseData.get("point_16").get("y"); // Right wrist

            // If knees are close to the ground and hands are placed forward, full score (5)
            if (kneeY > wristY && kneeY2 > wristY2) {
                return 5;
            }
            return 2; // Partial movement
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Assess ability to pick up an object based on hip and hand positions.
     */
    private static int assessObjectPickup(Map<String, Map<String, Double>> poseData) {
        try {
            double wristY = poseData.get("point_15").get("y"); // Left wrist
            double ankleY = poseData.get("point_27").get("y"); // Left ankle

            double wristY2 = poseData.get("point_16").get("y"); // Right wrist
            double ankleY2 = poseData.get("point_28").get("y"); // Right ankle

            // If wrist is close to ankle (indicating reaching for an object), full score (5)
            if (Math.abs(wristY - ankleY) < 0.1 && Math.abs(wristY2 - ankleY2) < 0.1) {
                return 5;
            }
            return 2; // Partial movement
        } catch (Exception e) {
            return 0;
        }
    }
}
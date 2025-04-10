package com.cmas.main.cmas;


public class CMASScorer {

    // 1. Head Lift (Neck Flexion)
    public static int scoreHeadElevation(double seconds) {
        if (seconds < 1) return 0;
        if (seconds < 10) return 1;
        if (seconds < 30) return 2;
        if (seconds < 60) return 3;
        if (seconds < 120) return 4;
        return 5;
    }



    // 2. Leg Raise / Touch Object
    public static int scoreLegRaise(boolean clearsTable, boolean touchesObject) {
        if (!clearsTable) return 0;
        if (!touchesObject) return 1;
        return 2;
    }



    // 3. Straight Leg Lift / Duration
    public static int scoreLegLiftDuration(double seconds) {
        if (seconds < 1) return 0;
        if (seconds < 10) return 1;
        if (seconds < 30) return 2;
        if (seconds < 60) return 3;
        if (seconds < 120) return 4;
        return 5;
    }



    // 4. Supine to Prone
    public static int scoreSupineToProne(boolean rolledSide, boolean freedArm, boolean reachedProne, boolean clean) {
        if (!rolledSide) return 0;
        if (!freedArm || !reachedProne) return 1;
        if (!clean) return 2;
        return 3;
    }


    // 5. Sit-ups (0–6)
    public static int scoreSitUps(int completedSitUps) {
        return Math.max(0, Math.min(completedSitUps, 6));
    }

    // 6. Supine to Sit
    public static int scoreSupineToSit(boolean success, boolean struggled, long duration) {
        if (!success) return 0;
        if (duration > 7000 || struggled) return 1;
        if (duration > 4000) return 2;
        return 3;
    }

    // 7. Arm Raise / Straighten
    public static int scoreArmRaiseHeight(boolean aboveAC, boolean aboveHead, boolean elbowsStraight) {
        if (!aboveAC) return 0;
        if (!aboveHead) return 1;
        if (!elbowsStraight) return 2;
        return 3;
    }

    // 8. Arm Raise / Duration
    public static int scoreArmRaiseDuration(double seconds) {
        if (seconds < 1) return 0;
        if (seconds < 10) return 1;
        if (seconds < 30) return 2;
        if (seconds < 60) return 3;
        return 4;
    }

    // 9. Floor Sit
    public static int scoreFloorSit(boolean reachedSit, boolean hesitant) {
        if (!reachedSit) return 0;
        if (hesitant) return 2;
        return 3;
    }

    // 10. All Fours Maneuver
    public static int scoreAllFours(boolean reachedAllFours, boolean raisedHead, boolean crawled, boolean legLifted) {
        if (!reachedAllFours) return 0;
        if (!raisedHead) return 1;
        if (!crawled) return 2;
        if (!legLifted) return 3;
        return 4;
    }

    // 11. Floor Rise
    public static int scoreFloorRise(boolean stoodUp, boolean usedHands, boolean struggled) {
        if (!stoodUp) return 0;
        if (usedHands) return 2;
        if (struggled) return 3;
        return 4;
    }

    // 12. Chair Rise
    public static int scoreChairRise(boolean stoodUp, boolean usedChairSide, boolean usedThighs, boolean struggled) {
        if (!stoodUp) return 0;
        if (usedChairSide) return 1;
        if (usedThighs) return 2;
        if (struggled) return 3;
        return 4;
    }

    // 13. Stool Step
    public static int scoreStoolStep(boolean attempted, boolean usedSupport, boolean usedThigh, boolean completed) {
        if (!attempted) return 0;
        if (usedSupport) return 1;
        if (usedThigh) return 2;
        if (completed) return 3;
        return 1; // fallback if completed condition failed but attempted was true
    }

    // 14. Pick-up Object

    public static int scorePickUpObject(boolean attempted, boolean heavySupport, boolean lightSupport, boolean completed) {
        if (!attempted || !completed) return 0;
        if (heavySupport) return 1;
        if (lightSupport) return 2;
        return 3;
    }

    // Sum all scores
    public static int totalScore(int... scores) {
        int sum = 0;
        for (int score : scores) sum += score;
        return sum;
    }
}
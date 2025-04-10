package com.cmas.main.cmas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.*;

import static com.cmas.main.cmas.CMASScorer.*;

public class CMASTest {

    //Getting the cmas data
    private static final String PYTHON_POSE_URL = "http://localhost:8080/latest-frame";
    private static final int GET_READY_DELAY_MS = 5000;
    private static final int POLL_INTERVAL_MS = 200;

    private static JsonObject getPoseData() throws IOException {
        URL url = new URL(PYTHON_POSE_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    // #1 Head Elevation Test
    public static int runHeadElevationTest() throws Exception {
        System.out.println("Starting test: Get ready...");
        Thread.sleep(GET_READY_DELAY_MS);

        double baseNoseY = -1;
        long liftStart = 0;
        long liftEnd = 0;
        boolean lifted = false;

        while (true) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 1) continue;

            JsonObject nose = pose.get(0).getAsJsonObject();
            double noseY = nose.get("y").getAsDouble();

            if (baseNoseY < 0) {
                baseNoseY = noseY;
                System.out.println("Baseline nose Y: " + baseNoseY);
            }

            // Lift detected
            if (!lifted && noseY < baseNoseY - 0.05) {
                liftStart = System.currentTimeMillis();
                lifted = true;
                System.out.println("Head lifted!");
            }

            // Drop detected
            if (lifted && noseY > baseNoseY - 0.005) {
                liftEnd = System.currentTimeMillis();
                System.out.println("Head lowered.");
                break;
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }

        double durationSec = (liftEnd - liftStart) / 1000.0;
        System.out.println("Head was up for " + durationSec + " seconds");

        return scoreHeadElevation(durationSec);
    }

    // #2 Leg Raise Test

    public static int runLegRaiseTest() throws Exception {
        System.out.println("Get ready for Leg Raise Test...");
          // 5-second prep delay

        double restY = -1;
        double footLength = -1;
        boolean lifted = false;
        boolean touched = false;

        long testStart = System.currentTimeMillis();
        long maxDuration = 10000;  // 10 seconds timeout

        while (System.currentTimeMillis() - testStart < maxDuration) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 33) continue;

            JsonObject ankle = pose.get(28).getAsJsonObject();
            JsonObject toe = pose.get(32).getAsJsonObject();

            double ankleY = ankle.get("y").getAsDouble();

            if (footLength < 0) {
                double dx = toe.get("x").getAsDouble() - ankle.get("x").getAsDouble();
                double dy = toe.get("y").getAsDouble() - ankle.get("y").getAsDouble();
                footLength = Math.sqrt(dx * dx + dy * dy);
                System.out.println("Estimated foot length: " + footLength);
            }

            if (restY < 0) {
                restY = ankleY;
                System.out.println("Baseline ankle Y: " + restY);
                continue;
            }

            double delta = restY - ankleY;
            double liftThreshold = 0.05;               // Clears table (minimum lift)
            double touchThreshold = 2 * footLength;    // Estimated goal height

            if (!lifted && delta > liftThreshold) {
                lifted = true;
                System.out.println("Leg cleared the table!");
            }

            if (!touched && delta > touchThreshold) {
                touched = true;
                System.out.println("Leg reached target height (~2 foot lengths)!");
                break;
            }

            Thread.sleep(200);
        }

        return scoreLegRaise(lifted, touched);
    }

    // #3 Straight leg lift test

    public static int runStraightLegLiftTest() throws Exception {
        System.out.println("Get ready for Straight Leg Lift Test...");
          // 5-second delay for user to prepare

        double restY = -1;
        double footLength = -1;
        double liftTarget = -1;

        long liftStart = 0;
        long liftEnd = 0;
        boolean lifting = false;

        long timeout = 120_000; // 2 minutes max
        long testStart = System.currentTimeMillis();

        while (System.currentTimeMillis() - testStart < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 33) continue;

            JsonObject ankle = pose.get(28).getAsJsonObject();
            JsonObject toe = pose.get(32).getAsJsonObject();

            double ankleY = ankle.get("y").getAsDouble();

            if (footLength < 0) {
                double dx = toe.get("x").getAsDouble() - ankle.get("x").getAsDouble();
                double dy = toe.get("y").getAsDouble() - ankle.get("y").getAsDouble();
                footLength = Math.sqrt(dx * dx + dy * dy);
                System.out.println("Foot length: " + footLength);
            }

            if (restY < 0) {
                restY = ankleY;
                liftTarget = restY - footLength;
                System.out.println("Baseline ankle Y: " + restY + " | Target Y: " + liftTarget);
                continue;
            }

            boolean aboveTarget = ankleY < liftTarget;

            if (aboveTarget && !lifting) {
                liftStart = System.currentTimeMillis();
                lifting = true;
                System.out.println("Leg lift detected! Timing started...");
            }

            if (!aboveTarget && lifting) {
                liftEnd = System.currentTimeMillis();
                System.out.println("Leg dropped. Timing ended.");
                break;
            }

            Thread.sleep(200);
        }

        if (!lifting || liftStart == 0) {
            System.out.println("User never lifted leg.");
            return 0;
        }

        // If still holding by end of test window
        if (liftEnd == 0) liftEnd = System.currentTimeMillis();

        double duration = (liftEnd - liftStart) / 1000.0;
        System.out.println("Held for " + duration + " seconds");

        return scoreLegLiftDuration(duration);
    }


    // #4 Suprine to Prone Test

    public static int runSupineToProneTest() throws Exception {
        System.out.println("Get ready for Supine to Prone Roll...");
        

        boolean turnedSide = false;
        boolean freedArm = false;
        boolean proneAchieved = false;

        long timeStart = System.currentTimeMillis();
        long timeRolled = 0;
        long timeFreedArm = 0;
        long timeProne = 0;

        long maxDuration = 15000;

        while (System.currentTimeMillis() - timeStart < maxDuration) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 25) continue;

            double lShoulderX = pose.get(11).getAsJsonObject().get("x").getAsDouble();
            double rShoulderX = pose.get(12).getAsJsonObject().get("x").getAsDouble();
            double lHipX = pose.get(23).getAsJsonObject().get("x").getAsDouble();
            double rHipX = pose.get(24).getAsJsonObject().get("x").getAsDouble();
            double rWristX = pose.get(16).getAsJsonObject().get("x").getAsDouble();
            double rElbowX = pose.get(14).getAsJsonObject().get("x").getAsDouble();

            // Detect roll to side
            double shoulderDiff = Math.abs(rShoulderX - lShoulderX);
            double hipDiff = Math.abs(rHipX - lHipX);
            if (!turnedSide && shoulderDiff < 0.1 && hipDiff < 0.1) {
                turnedSide = true;
                timeRolled = System.currentTimeMillis();
                System.out.println("Rolled onto side.");
            }

            // Detect arm freed
            double torsoMidX = (rShoulderX + rHipX) / 2.0;
            if (turnedSide && !freedArm && Math.abs(rWristX - torsoMidX) > 0.1) {
                freedArm = true;
                timeFreedArm = System.currentTimeMillis();
                System.out.println("Arm freed.");
            }

            // Fully prone
            double rWristY = pose.get(16).getAsJsonObject().get("y").getAsDouble();
            double rShoulderY = pose.get(12).getAsJsonObject().get("y").getAsDouble();
            if (Math.abs(rWristY - rShoulderY) < 0.05 && turnedSide && freedArm) {
                proneAchieved = true;
                timeProne = System.currentTimeMillis();
                System.out.println("Reached prone.");
                break;
            }

            Thread.sleep(200);
        }

        boolean clean = checkCleanSupineToProne(timeStart, timeRolled, timeFreedArm, timeProne);
        return scoreSupineToProne(turnedSide, freedArm, proneAchieved, clean);
    }

    private static boolean checkCleanSupineToProne(long start, long rolled, long freed, long prone) {
        if (rolled == 0 || freed == 0 || prone == 0) return false;

        long total = prone - start;
        long armDelay = freed - rolled;

        return total < 6000 && armDelay < 2000;
    }

    // #5 Situp Test

    public static int runSitUpTest() throws Exception {
        System.out.println("Get ready for Sit-Up Test. You’ll perform 6 sit-ups...");
        

        int successfulSitUps = 0;
        int totalSitUps = 6;
        boolean inRestPosition = true;
        boolean inSitUp = false;
        long sitUpTimeout = 15000;  // 15s max per sit-up

        for (int i = 1; i <= totalSitUps; i++) {
            System.out.println("Waiting for Sit-Up #" + i);
            long start = System.currentTimeMillis();
            boolean counted = false;

            while (System.currentTimeMillis() - start < sitUpTimeout) {
                JsonObject data = getPoseData();
                JsonArray pose = data.getAsJsonArray("cmas");
                if (pose == null || pose.size() < 25) continue;

                double lShoulderY = pose.get(11).getAsJsonObject().get("y").getAsDouble();
                double rShoulderY = pose.get(12).getAsJsonObject().get("y").getAsDouble();
                double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
                double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();

                double avgShoulderY = (lShoulderY + rShoulderY) / 2;
                double avgHipY = (lHipY + rHipY) / 2;
                double distance = avgShoulderY - avgHipY;

                // Resting = shoulders high above hips
                if (distance > 0.15) inRestPosition = true;

                // Sit-up detected
                if (inRestPosition && distance < 0.05 && !counted) {
                    successfulSitUps++;
                    counted = true;
                    inRestPosition = false;
                    System.out.println(" Sit-up #" + i + " counted!");
                    break;
                }

                Thread.sleep(150);
            }

            if (!counted) {
                System.out.println(" Sit-up #" + i + " not detected.");
            }

            Thread.sleep(1000); // Small break before next
        }

        return scoreSitUps(successfulSitUps);
    }


    // 6. Supine to Sit

    public static int runSupineToSitTest() throws Exception {
        System.out.println("Get ready for Supine to Sit test...");
         

        boolean transitioned = false;
        boolean sitting = false;
        boolean struggling = false;

        long timeStart = System.currentTimeMillis();
        long rollStart = 0;
        long sitCompleteTime = 0;

        double previousShoulderY = -1;
        int delayCount = 0;
        int maxDelayAllowed = 5;
        long maxTestDuration = 15000;

        while (System.currentTimeMillis() - timeStart < maxTestDuration) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");
            if (pose == null || pose.size() < 25) continue;

            double lShoulderY = pose.get(11).getAsJsonObject().get("y").getAsDouble();
            double rShoulderY = pose.get(12).getAsJsonObject().get("y").getAsDouble();
            double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
            double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();

            double avgShoulderY = (lShoulderY + rShoulderY) / 2;
            double avgHipY = (lHipY + rHipY) / 2;
            double shoulderHeight = avgHipY - avgShoulderY;

            if (rollStart == 0 && shoulderHeight < 0.05) {
                rollStart = System.currentTimeMillis();
                System.out.println("Supine position baseline established.");
            }

            if (shoulderHeight > 0.1 && !transitioned) {
                transitioned = true;
                sitCompleteTime = System.currentTimeMillis();
                System.out.println("Sit-up movement detected.");
            }

            // Monitor for struggling (jitter or slow progress)
            if (previousShoulderY > 0 && Math.abs(previousShoulderY - avgShoulderY) < 0.005) {
                delayCount++;
            } else {
                delayCount = Math.max(0, delayCount - 1);
            }

            if (delayCount > maxDelayAllowed) {
                struggling = true;
            }

            previousShoulderY = avgShoulderY;

            Thread.sleep(200);
        }

        return scoreSupineToSit(transitioned, struggling, sitCompleteTime - rollStart);
    }

    // 7. Arm Raise / Straighten

    private static double getAngle(JsonObject a, JsonObject b, JsonObject c) {
        double ax = a.get("x").getAsDouble();
        double ay = a.get("y").getAsDouble();
        double bx = b.get("x").getAsDouble();
        double by = b.get("y").getAsDouble();
        double cx = c.get("x").getAsDouble();
        double cy = c.get("y").getAsDouble();

        double abX = ax - bx;
        double abY = ay - by;
        double cbX = cx - bx;
        double cbY = cy - by;

        double dot = (abX * cbX + abY * cbY);
        double cross = (abX * cbY - abY * cbX);

        double angle = Math.toDegrees(Math.atan2(Math.abs(cross), dot));
        return angle;
    }

    public static int runArmRaiseTest() throws Exception {
        System.out.println("Get ready for Arm Raise Test...");
        

        int result = 0;

        long timeout = 10000;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");
            if (pose == null || pose.size() < 17) continue;

            JsonObject lShoulder = pose.get(11).getAsJsonObject();
            JsonObject rShoulder = pose.get(12).getAsJsonObject();
            JsonObject lElbow = pose.get(13).getAsJsonObject();
            JsonObject rElbow = pose.get(14).getAsJsonObject();
            JsonObject lWrist = pose.get(15).getAsJsonObject();
            JsonObject rWrist = pose.get(16).getAsJsonObject();

            double lShoulderY = lShoulder.get("y").getAsDouble();
            double rShoulderY = rShoulder.get("y").getAsDouble();
            double lWristY = lWrist.get("y").getAsDouble();
            double rWristY = rWrist.get("y").getAsDouble();

            boolean lAboveShoulder = lWristY < lShoulderY;
            boolean rAboveShoulder = rWristY < rShoulderY;
            boolean lAboveHead = lWristY < 0.3;
            boolean rAboveHead = rWristY < 0.3;

            double lAngle = getAngle(lShoulder, lElbow, lWrist);
            double rAngle = getAngle(rShoulder, rElbow, rWrist);

            boolean lStraight = lAngle > 160;
            boolean rStraight = rAngle > 160;

            if (!lAboveShoulder && !rAboveShoulder) {
                result = 0;
            } else if ((lAboveShoulder || rAboveShoulder) && (!lAboveHead && !rAboveHead)) {
                result = 1;
            } else if ((lAboveHead || rAboveHead) && (!lStraight || !rStraight)) {
                result = 2;
            } else if ((lAboveHead && rAboveHead) && (lStraight && rStraight)) {
                result = 3;
            }

            if (result > 0) break;

            Thread.sleep(200);
        }

        return result;
    }

    // 8. Arm Raise / Duration

    public static int runArmRaiseDurationTest() throws Exception {
        System.out.println("Get ready for Arm Raise Duration Test...");

        long raiseStart = 0;
        long raiseEnd = 0;
        boolean raised = false;

        long timeout = 65000;
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 17) continue;

            JsonObject lWrist = pose.get(15).getAsJsonObject();
            JsonObject rWrist = pose.get(16).getAsJsonObject();
            JsonObject lShoulder = pose.get(11).getAsJsonObject();
            JsonObject rShoulder = pose.get(12).getAsJsonObject();

            boolean lUp = lWrist.get("y").getAsDouble() < lShoulder.get("y").getAsDouble();
            boolean rUp = rWrist.get("y").getAsDouble() < rShoulder.get("y").getAsDouble();

            if (lUp && rUp && !raised) {
                raiseStart = System.currentTimeMillis();
                raised = true;
                System.out.println("Arms raised. Timing started...");
            }

            if ((!lUp || !rUp) && raised) {
                raiseEnd = System.currentTimeMillis();
                System.out.println("Arms lowered. Timing ended.");
                break;
            }

            Thread.sleep(200);
        }

        if (!raised) return 0;
        if (raiseEnd == 0) raiseEnd = System.currentTimeMillis();

        double duration = (raiseEnd - raiseStart) / 1000.0;
        System.out.println("Duration held: " + duration + " seconds");

        return scoreArmRaiseDuration(duration);
    }

    // 9. Floor Sit

    public static int runFloorSitTest() throws Exception {
        System.out.println("Get ready for Floor Sit test...");

        boolean sittingDetected = false;
        boolean hesitant = false;

        long timeStart = System.currentTimeMillis();
        long sitStartTime = 0;
        long sitEndTime = 0;

        double previousHipY = -1;
        int freezeCount = 0;
        int freezeThreshold = 5;
        long maxDuration = 15000;

        while (System.currentTimeMillis() - timeStart < maxDuration) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 25) continue;

            double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
            double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();
            double avgHipY = (lHipY + rHipY) / 2;

            if (sitStartTime == 0) {
                sitStartTime = System.currentTimeMillis();
            }

            // Detect sitting position
            if (avgHipY > 0.8) {
                sittingDetected = true;
                sitEndTime = System.currentTimeMillis();
                System.out.println("Sit position detected.");
                break;
            }

            // Hesitation detection (no significant change)
            if (previousHipY > 0 && Math.abs(previousHipY - avgHipY) < 0.005) {
                freezeCount++;
            } else {
                freezeCount = Math.max(0, freezeCount - 1);
            }

            if (freezeCount >= freezeThreshold) {
                hesitant = true;
            }

            previousHipY = avgHipY;

            Thread.sleep(200);
        }

        return scoreFloorSit(sittingDetected, hesitant);
    }

    // 10. All Fours Maneuver

    public static int runAllFoursTest() throws Exception {
        System.out.println("Get ready for All Fours Maneuver...");

        boolean reachedAllFours = false;
        boolean raisedHead = false;
        boolean crawled = false;
        boolean legLifted = false;

        double initialHipX = -1;
        double initialWristX = -1;

        long testStart = System.currentTimeMillis();
        long maxDuration = 20000;

        while (System.currentTimeMillis() - testStart < maxDuration) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");
            if (pose == null || pose.size() < 29) continue;

            double lWristY = pose.get(15).getAsJsonObject().get("y").getAsDouble();
            double rWristY = pose.get(16).getAsJsonObject().get("y").getAsDouble();
            double lKneeY = pose.get(25).getAsJsonObject().get("y").getAsDouble();
            double rKneeY = pose.get(26).getAsJsonObject().get("y").getAsDouble();
            double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
            double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();
            double noseY = pose.get(0).getAsJsonObject().get("y").getAsDouble();

            double avgWristY = (lWristY + rWristY) / 2;
            double avgKneeY = (lKneeY + rKneeY) / 2;
            double avgHipY = (lHipY + rHipY) / 2;

            if (!reachedAllFours && avgWristY > avgHipY && avgKneeY > avgHipY) {
                reachedAllFours = true;
                initialHipX = (pose.get(23).getAsJsonObject().get("x").getAsDouble()
                        + pose.get(24).getAsJsonObject().get("x").getAsDouble()) / 2;
                initialWristX = (pose.get(15).getAsJsonObject().get("x").getAsDouble()
                        + pose.get(16).getAsJsonObject().get("x").getAsDouble()) / 2;
                System.out.println("Entered all-fours position.");
            }

            if (reachedAllFours && !raisedHead && noseY < avgHipY - 0.05) {
                raisedHead = true;
                System.out.println("Head raised.");
            }

            if (reachedAllFours && !crawled) {
                double currentHipX = (pose.get(23).getAsJsonObject().get("x").getAsDouble()
                        + pose.get(24).getAsJsonObject().get("x").getAsDouble()) / 2;
                double currentWristX = (pose.get(15).getAsJsonObject().get("x").getAsDouble()
                        + pose.get(16).getAsJsonObject().get("x").getAsDouble()) / 2;

                if (Math.abs(currentHipX - initialHipX) > 0.05 || Math.abs(currentWristX - initialWristX) > 0.05) {
                    crawled = true;
                    System.out.println("Crawling motion detected.");
                }
            }

            if (reachedAllFours && !legLifted) {
                double lAnkleY = pose.get(27).getAsJsonObject().get("y").getAsDouble();
                double rAnkleY = pose.get(28).getAsJsonObject().get("y").getAsDouble();

                if (lAnkleY < lKneeY - 0.1 || rAnkleY < rKneeY - 0.1) {
                    legLifted = true;
                    System.out.println("Leg lifted and extended.");
                    break;
                }
            }

            Thread.sleep(200);
        }

        return scoreAllFours(reachedAllFours, raisedHead, crawled, legLifted);
    }

    // 11. Floor Rise

    public static int runFloorRiseTest() throws Exception {
        System.out.println("Get ready for Floor Rise test...");
        

        boolean usedChair = false;  // Not detectable without external reference
        boolean usedHands = false;
        boolean struggled = false;
        boolean stoodUp = false;

        double hipYStart = -1;
        double hipYEnd = -1;
        long riseStart = 0;
        long riseEnd = 0;
        double prevHipY = -1;
        int freezeCount = 0;
        int freezeLimit = 5;

        long startTime = System.currentTimeMillis();
        long timeout = 15000;

        while (System.currentTimeMillis() - startTime < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 29) continue;

            double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
            double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();
            double avgHipY = (lHipY + rHipY) / 2;

            double lWristY = pose.get(15).getAsJsonObject().get("y").getAsDouble();
            double rWristY = pose.get(16).getAsJsonObject().get("y").getAsDouble();
            double lKneeY = pose.get(25).getAsJsonObject().get("y").getAsDouble();
            double rKneeY = pose.get(26).getAsJsonObject().get("y").getAsDouble();
            double avgKneeY = (lKneeY + rKneeY) / 2;

            // Starting seated
            if (hipYStart < 0 && avgHipY > 0.8) {
                hipYStart = avgHipY;
                riseStart = System.currentTimeMillis();
            }

            // Detect use of hands (wrists below hips during motion)
            if (avgHipY > 0.6 && (lWristY > avgKneeY || rWristY > avgKneeY)) {
                usedHands = true;
            }

            // Movement stagnation (hesitation)
            if (prevHipY > 0 && Math.abs(prevHipY - avgHipY) < 0.005) {
                freezeCount++;
            } else {
                freezeCount = Math.max(0, freezeCount - 1);
            }

            if (freezeCount > freezeLimit) {
                struggled = true;
            }

            prevHipY = avgHipY;

            // Standing = hips high enough
            if (avgHipY < 0.45 && hipYStart > 0.7) {
                hipYEnd = avgHipY;
                riseEnd = System.currentTimeMillis();
                stoodUp = true;
                break;
            }

            Thread.sleep(200);
        }

        return scoreFloorRise(stoodUp, usedHands, struggled);
    }

    // 12. Chair Rise

    public static int runChairRiseTest() throws Exception {
        System.out.println("Get ready for Chair Rise test...");
        

        boolean usedChairSide = false; // inferred from hands behind or outside body
        boolean usedThighs = false;
        boolean struggled = false;
        boolean stoodUp = false;

        double hipYStart = -1;
        double hipYEnd = -1;
        long riseStart = 0;
        long riseEnd = 0;
        double prevHipY = -1;
        int freezeCount = 0;
        int freezeLimit = 5;

        long startTime = System.currentTimeMillis();
        long timeout = 15000;

        while (System.currentTimeMillis() - startTime < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");

            if (pose == null || pose.size() < 29) continue;

            double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
            double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();
            double avgHipY = (lHipY + rHipY) / 2;

            double lWristY = pose.get(15).getAsJsonObject().get("y").getAsDouble();
            double rWristY = pose.get(16).getAsJsonObject().get("y").getAsDouble();
            double lWristX = pose.get(15).getAsJsonObject().get("x").getAsDouble();
            double rWristX = pose.get(16).getAsJsonObject().get("x").getAsDouble();

            double lKneeY = pose.get(25).getAsJsonObject().get("y").getAsDouble();
            double rKneeY = pose.get(26).getAsJsonObject().get("y").getAsDouble();
            double lKneeX = pose.get(25).getAsJsonObject().get("x").getAsDouble();
            double rKneeX = pose.get(26).getAsJsonObject().get("x").getAsDouble();

            // Seated detection
            if (hipYStart < 0 && avgHipY > 0.75) {
                hipYStart = avgHipY;
                riseStart = System.currentTimeMillis();
            }

            // Wrist position analysis
            boolean leftOnThigh = lWristY > lKneeY && Math.abs(lWristX - lKneeX) < 0.1;
            boolean rightOnThigh = rWristY > rKneeY && Math.abs(rWristX - rKneeX) < 0.1;
            boolean leftBehindBody = lWristX < pose.get(23).getAsJsonObject().get("x").getAsDouble() - 0.15;
            boolean rightBehindBody = rWristX > pose.get(24).getAsJsonObject().get("x").getAsDouble() + 0.15;

            if (leftOnThigh || rightOnThigh) {
                usedThighs = true;
            }

            if (leftBehindBody || rightBehindBody) {
                usedChairSide = true;
            }

            // Standing detection
            if (avgHipY < 0.45 && hipYStart > 0.7) {
                hipYEnd = avgHipY;
                riseEnd = System.currentTimeMillis();
                stoodUp = true;
                break;
            }

            // Struggle detection
            if (prevHipY > 0 && Math.abs(prevHipY - avgHipY) < 0.005) {
                freezeCount++;
            } else {
                freezeCount = Math.max(0, freezeCount - 1);
            }

            if (freezeCount > freezeLimit) {
                struggled = true;
            }

            prevHipY = avgHipY;

            Thread.sleep(200);
        }

        return scoreChairRise(stoodUp, usedChairSide, usedThighs, struggled);
    }

    // 13. Stool Step

    public static int runStoolStepTest() throws Exception {
        System.out.println("Get ready for Stool Step test...");
        

        boolean attempted = false;
        boolean usedSupport = false;
        boolean usedThigh = false;
        boolean completed = false;

        long startTime = System.currentTimeMillis();
        long timeout = 15000;

        while (System.currentTimeMillis() - startTime < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");
            if (pose == null || pose.size() < 29) continue;

            double lAnkleY = pose.get(27).getAsJsonObject().get("y").getAsDouble();
            double rAnkleY = pose.get(28).getAsJsonObject().get("y").getAsDouble();
            double lKneeY = pose.get(25).getAsJsonObject().get("y").getAsDouble();
            double rKneeY = pose.get(26).getAsJsonObject().get("y").getAsDouble();
            double lHipY = pose.get(23).getAsJsonObject().get("y").getAsDouble();
            double rHipY = pose.get(24).getAsJsonObject().get("y").getAsDouble();

            double lWristY = pose.get(15).getAsJsonObject().get("y").getAsDouble();
            double rWristY = pose.get(16).getAsJsonObject().get("y").getAsDouble();
            double lWristX = pose.get(15).getAsJsonObject().get("x").getAsDouble();
            double rWristX = pose.get(16).getAsJsonObject().get("x").getAsDouble();
            double lKneeX = pose.get(25).getAsJsonObject().get("x").getAsDouble();
            double rKneeX = pose.get(26).getAsJsonObject().get("x").getAsDouble();

            // Detect attempted step: one foot significantly raised
            if (!attempted && Math.abs(lAnkleY - rAnkleY) > 0.15) {
                attempted = true;
                System.out.println("Step attempt detected.");
            }

            // Detect support on thigh
            boolean leftOnThigh = lWristY > lKneeY && Math.abs(lWristX - lKneeX) < 0.1;
            boolean rightOnThigh = rWristY > rKneeY && Math.abs(rWristX - rKneeX) < 0.1;
            if (leftOnThigh || rightOnThigh) {
                usedThigh = true;
            }

            // Detect general use of support (wrists below hips)
            double avgHipY = (lHipY + rHipY) / 2;
            if (lWristY > avgHipY || rWristY > avgHipY) {
                usedSupport = true;
            }

            // Detect completion: foot stays raised
            if (attempted && (lAnkleY < 0.6 || rAnkleY < 0.6)) {
                completed = true;
                break;
            }

            Thread.sleep(200);
        }

        return scoreStoolStep(attempted, usedSupport, usedThigh, completed);
    }

    // 14. Pick-up Object

    public static int runPickUpObjectTest() throws Exception {
        System.out.println("Get ready for Pick-Up Object test...");
        

        boolean attempted = false;
        boolean usedHeavySupport = false;
        boolean usedLightSupport = false;
        boolean completed = false;

        long startTime = System.currentTimeMillis();
        long timeout = 15000;

        while (System.currentTimeMillis() - startTime < timeout) {
            JsonObject data = getPoseData();
            JsonArray pose = data.getAsJsonArray("cmas");
            if (pose == null || pose.size() < 29) continue;

            double lWristY = pose.get(15).getAsJsonObject().get("y").getAsDouble();
            double rWristY = pose.get(16).getAsJsonObject().get("y").getAsDouble();
            double lWristX = pose.get(15).getAsJsonObject().get("x").getAsDouble();
            double rWristX = pose.get(16).getAsJsonObject().get("x").getAsDouble();

            double lKneeY = pose.get(25).getAsJsonObject().get("y").getAsDouble();
            double rKneeY = pose.get(26).getAsJsonObject().get("y").getAsDouble();
            double lKneeX = pose.get(25).getAsJsonObject().get("x").getAsDouble();
            double rKneeX = pose.get(26).getAsJsonObject().get("x").getAsDouble();

            double lAnkleY = pose.get(27).getAsJsonObject().get("y").getAsDouble();
            double rAnkleY = pose.get(28).getAsJsonObject().get("y").getAsDouble();
            double floorY = Math.max(lAnkleY, rAnkleY);

            // Detect reach attempt (at least one hand gets close to floor)
            if (!attempted && (lWristY > 0.8 || rWristY > 0.8)) {
                attempted = true;
                System.out.println("Pick-up attempt detected.");
            }

            // Detect heavy support (hand near floor)
            if (lWristY > floorY + 0.05 || rWristY > floorY + 0.05) {
                usedHeavySupport = true;
            }

            // Detect hand on thigh or light support (near knees)
            boolean leftTouchingKnee = lWristY > lKneeY && Math.abs(lWristX - lKneeX) < 0.1;
            boolean rightTouchingKnee = rWristY > rKneeY && Math.abs(rWristX - rKneeX) < 0.1;
            if (leftTouchingKnee || rightTouchingKnee) {
                usedLightSupport = true;
            }

            // Detect pick-up success (wrist down then rises back up quickly)
            if (attempted && lWristY < 0.6 && rWristY < 0.6) {
                completed = true;
                break;
            }

            Thread.sleep(200);
        }

        return scorePickUpObject(attempted, usedHeavySupport, usedLightSupport, completed);
    }
}

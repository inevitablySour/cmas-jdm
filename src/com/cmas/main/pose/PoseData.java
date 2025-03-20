package com.cmas.main.pose;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class PoseData {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("pose_data")
    private Map<String, Map<String, Double>> poseData;

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Map<String, Map<String, Double>> getPoseData() { return poseData; }
    public void setPoseData(Map<String, Map<String, Double>> poseData) { this.poseData = poseData; }
}

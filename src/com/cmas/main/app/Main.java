package com.cmas.main.app;



import com.cmas.main.pose.CMASScorer;
import com.cmas.main.pose.PoseData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String imagePath = "path/to/image.jpg";
        String apiUrl = "http://localhost:5000/detect_pose";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadFile = new HttpPost(apiUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("image", new File(imagePath), ContentType.IMAGE_JPEG, "image.jpg");
            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
                ObjectMapper objectMapper = new ObjectMapper();
                PoseData poseData = objectMapper.readValue(response.getEntity().getContent(), PoseData.class);

                if (poseData.isSuccess()) {
                    int cmasScore = CMASScorer.calculateScore(poseData.getPoseData());
                    System.out.println("CMAS Score: " + cmasScore);
                } else {
                    System.out.println("Pose detection failed.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
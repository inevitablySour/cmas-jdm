from flask import Flask, jsonify
import threading
import cv2
import mediapipe as mp

app = Flask(__name__)
latest_frame_data = {}

# Initialize MediaPipe
mp_pose = mp.solutions.pose
pose = mp_pose.Pose()
cap = cv2.VideoCapture(0)

def capture_loop():
    global latest_frame_data
    while cap.isOpened():
        success, frame = cap.read()
        if not success:
            continue

        image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(image_rgb)

        pose_landmarks = []
        legs = {'left_leg': {}, 'right_leg': {}}
        feet = {'left_foot': {}, 'right_foot': {}}

        if results.pose_landmarks:
            for idx, lm in enumerate(results.pose_landmarks.landmark):
                point = {'id': idx, 'x': lm.x, 'y': lm.y, 'z': lm.z, 'visibility': lm.visibility}
                pose_landmarks.append(point)

                # Group leg/foot points
                if idx == 23:
                    legs['left_leg']['hip'] = point
                elif idx == 25:
                    legs['left_leg']['knee'] = point
                elif idx == 27:
                    legs['left_leg']['ankle'] = point
                elif idx == 29:
                    feet['left_foot']['heel'] = point
                elif idx == 31:
                    feet['left_foot']['toe'] = point

                elif idx == 24:
                    legs['right_leg']['hip'] = point
                elif idx == 26:
                    legs['right_leg']['knee'] = point
                elif idx == 28:
                    legs['right_leg']['ankle'] = point
                elif idx == 30:
                    feet['right_foot']['heel'] = point
                elif idx == 32:
                    feet['right_foot']['toe'] = point

        latest_frame_data = {
            'pose': pose_landmarks,
            'legs': legs,
            'feet': feet
        }

# Background capture thread
threading.Thread(target=capture_loop, daemon=True).start()

@app.route('/latest-frame', methods=['GET'])
def get_latest_frame():
    return jsonify(latest_frame_data)

if __name__ == '__main__':
    app.run(port=8080)
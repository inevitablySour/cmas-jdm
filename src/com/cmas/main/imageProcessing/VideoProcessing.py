from flask import Flask, jsonify, Response
import threading
import cv2
import mediapipe as mp
import platform
import sys
import time

app = Flask(__name__)
latest_frame_data = {}

# Setup camera and MediaPipe
cap = cv2.VideoCapture(0)  # Use 1 or 2 if external webcam
mp_pose = mp.solutions.pose
pose = mp_pose.Pose()
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles


def explain_permission_issue():
    os_name = platform.system()
    print("\nERROR: Could not access the webcam.")
    print("This may be due to missing camera permissions.\n")

    if os_name == "Darwin":  # macOS
        print("👉 Go to: System Settings > Privacy & Security > Camera")
        print("✅ Ensure Python or Terminal has camera access.")
    elif os_name == "Windows":
        print("👉 Go to: Settings > Privacy > Camera")
        print("✅ Ensure 'Camera access' and 'Allow desktop apps to access your camera' are ON.")
    elif os_name == "Linux":
        print("⚠️ Camera permissions are typically managed by your window manager.")
        print("✅ Make sure no other application is locking the camera.")
    else:
        print("⚠️ Unknown OS. Please check your system's camera privacy settings.")

    print("\nExiting due to camera access failure.")
    time.sleep(5)
    sys.exit(1)


def capture_loop():
    global latest_frame_data

    if not cap.isOpened():
        explain_permission_issue()

    while True:
        success, frame = cap.read()
        if not success:
            continue

        image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(image_rgb)

        pose_landmarks = []
        legs = {'left_leg': {}, 'right_leg': {}}
        feet = {'left_foot': {}, 'right_foot': {}}

        if results.pose_landmarks:
            mp_drawing.draw_landmarks(
                frame,
                results.pose_landmarks,
                mp_pose.POSE_CONNECTIONS,
                landmark_drawing_spec=mp_drawing_styles.get_default_pose_landmarks_style()
            )

            for idx, lm in enumerate(results.pose_landmarks.landmark):
                point = {'id': idx, 'x': lm.x, 'y': lm.y, 'z': lm.z, 'visibility': lm.visibility}
                pose_landmarks.append(point)

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
            'cmas': pose_landmarks,
            'legs': legs,
            'feet': feet
        }

        global last_drawn_frame
        last_drawn_frame = frame.copy()


def generate_mjpeg():
    while True:
        if last_drawn_frame is not None:
            ret, jpeg = cv2.imencode('.jpg', last_drawn_frame)
            if ret:
                frame = jpeg.tobytes()
                yield (b'--frame\r\n'
                       b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')
        cv2.waitKey(1)


@app.route('/video_feed')
def video_feed():
    return Response(generate_mjpeg(), mimetype='multipart/x-mixed-replace; boundary=frame')


@app.route('/latest-frame', methods=['GET'])
def get_latest_frame():
    return jsonify(latest_frame_data)


if __name__ == '__main__':
    last_drawn_frame = None

    # Start Flask server in background thread
    threading.Thread(target=lambda: app.run(port=8080, use_reloader=False), daemon=True).start()

    # Start video processing
    capture_loop()
# CMAS AI Motion Tracking System

## Project Overview
The **CMAS AI Motion Tracking System** is a Java-based application that helps doctors remotely monitor children diagnosed with **Juvenile Dermatomyositis (JDM)**. The system utilizes **image recognition and pose detection** to automatically assess a child’s **CMAS (Children’s Myositis Assessment Scale) score**, eliminating the need for manual input.

## Key Features
- **AI-Powered Motion Tracking**: Uses OpenCV & TensorFlow to analyze body movements.
- **Automated CMAS Scoring**: Determines the child's ability to perform specific exercises and assigns a score.
- **User-Friendly Patient Interface**: Children perform tests using a camera; the system automatically evaluates them.
- **Doctor’s Dashboard**: Provides patient history, movement trends, and biomarker correlations.
- **Data Storage & Analysis**: Scores and biomarker data are stored in an SQLite database for tracking disease progression.

##  Tech Stack
| Component            | Technology         |
|---------------------|-------------------|
| **Language**       | Java |
| **Image Recognition** | OpenCV |
| **Pose Detection** | TensorFlow + MediaPipe |
| **Database**       | SQLite |
| **GUI**           | JavaFX |

## How It Works
### Patient Workflow (Children’s Interface)
1. **Home Screen**: Displays upcoming CMAS tests and past performance.
2. **Exercise Test**:
   - Shows a **video or animation** demonstrating the required movement.
   - The child **performs the movement in front of the camera**.
   - AI tracks the movement and **assigns a CMAS score** automatically.
3. **Results**: The child sees their **progress and encouragement messages**.

### Doctor Workflow (Dashboard)
1. **Login Screen**: Secure authentication for doctors.
2. **Patient Overview**: Displays a list of patients and their latest scores.
3. **Detailed Reports**:
   - Shows **CMAS trends and biomarker correlations**.
   - Provides AI-generated movement insights.
4. **Export Data**: Allows generating **progress reports for medical records**.

## Installation & Setup
### Prerequisites:
- Java (JDK 17 or later)
- OpenCV for Java
- TensorFlow for Java (PoseNet or MediaPipe)
- SQLite for database management





# CMAS AI Motion Tracking System

## Overview
The CMAS Motion Tracking System is a cross-platform application built in Java and Python, designed to help doctors remotely assess motor function in children diagnosed with Juvenile Dermatomyositis (JDM). The system automates the Children’s Myositis Assessment Scale (CMAS) scoring process by combining pose estimation, video capture, and biomarker analysis within an intuitive graphical interface for doctors and patients.

## Key Features

### Patient Interface
- Guided CMAS test experience with live instructions
- Real-time pose tracking via webcam
- Automated scoring based on motion accuracy
- Feedback and encouragement after each test

### Doctor Dashboard
- Search and review patient history by name or ID
- Interactive charts of CMAS scores over time
- Access to biomarker test data and medication group
- JFreeChart integration for medical trend visualization

### Backend & Data
- Live pose tracking via Python Flask and MediaPipe
- MySQL database stores:
   - Patient records
   - Lab results
   - CMAS test history
- FlatLaf for a modern Swing-based UI

## Tech Stack

| Component             | Technology                 |
|----------------------|----------------------------|
| Language              | Java (JDK 23), Python 3    |
| Frontend GUI          | Swing + FlatLaf            |
| Database              | MySQL                      |
| Pose Detection        | MediaPipe + OpenCV         |
| Charts                | JFreeChart                 |
| API Communication     | Python Flask server        |
| Other Libraries       | GSON, Jackson, Apache HTTP |

## How It Works

### Doctor Workflow
1. Login securely
2. Search patients by name or ID
3. View medication group and lab data
4. Analyze performance trends (interactive charts)
5. Export or print medical reports

### Patient Workflow
1. Launch CMAS test window
2. Follow animated instructions
3. Perform movement on camera
4. AI scores the performance
5. See encouragement and progress feedback

## Installation & Setup

### Java Requirements (Maven)
Add the following dependencies to your `pom.xml`:

- FlatLaf
- JFreeChart
- Google GSON
- MySQL JDBC Connector
- Jackson Databind
- Apache HTTP Components (Client and Mime)

### Python Requirements

Install Python dependencies via pip:

```
pip install mediapipe opencv-python flask
```

## Project Structure


```
cmas-jdm/
└── src/
    ├── com.cmas.main.gui              # Patient & Doctor GUIs
    ├── com.cmas.main.dao              # Database controller & queries
    ├── com.cmas.main.imageProcessing  # Python server communication
    └── resources/                     # Encrypted config files
```


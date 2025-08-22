# backend/python-api/app.py
from flask import Flask, request, jsonify
from ultralytics import YOLO
import cv2
import base64
import numpy as np
import io
import torch
import logging

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)

# YOLOv8 model loading
try:
    model = YOLO('weights/best_m.pt')  # Ensure the path is correct
    logging.info("YOLOv8 model loaded successfully.")
except Exception as e:
    logging.error(f"Error loading YOLOv8 model: {e}")
    model = None

@app.route('/analyze_crowd', methods=['POST'])
def analyze_crowd():
    if model is None:
        return jsonify({"error": "Model not loaded"}), 500

    if 'image' not in request.json:
        return jsonify({"error": "No image data provided"}), 400

    try:
        # Get base64 encoded image data from request
        image_data = request.json['image']
        image_bytes = base64.b64decode(image_data)

        # Decode image using OpenCV
        nparr = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if image is None:
            return jsonify({"error": "Could not decode image"}), 400

        # Perform object detection
        results = model(image)
        
        # Get the number of detected people (assuming 'person' class is 0 in your model)
        person_count = 0
        for result in results:
            # Check if a tensor is returned and has detections
            if result.boxes is not None:
                # Assuming class 0 is 'person' based on common COCO dataset
                person_count += torch.sum(result.boxes.cls == 0).item()
            
        logging.info(f"Image analyzed, detected {person_count} people.")

        return jsonify({"person_count": person_count})

    except Exception as e:
        logging.error(f"Error during crowd analysis: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    # Use GPU if available
    if torch.cuda.is_available():
        logging.info("CUDA is available. Using GPU.")
    else:
        logging.info("CUDA is not available. Using CPU.")
    
    app.run(host='0.0.0.0', port=5000, debug=False)
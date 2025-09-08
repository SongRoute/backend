# backend/python-api/app.py
from flask import Flask, request, jsonify
from ultralytics import YOLO
import cv2
import base64
import numpy as np
import io
import torch
import logging

# 로깅 설정 (INFO 레벨, 시간/레벨/메시지 출력)
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Flask 애플리케이션 생성
app = Flask(__name__)

# YOLOv8 model loading
try:
    model = YOLO('weights/best_m.pt')  # 모델 가중치 파일 경로 (확인 필요)
    logging.info("YOLOv8 model loaded successfully.")
except Exception as e:
    logging.error(f"Error loading YOLOv8 model: {e}")
    model = None

# 인원 수 분석 API 엔드포인트
@app.route('/analyze_crowd', methods=['POST'])
def analyze_crowd():
    # 모델이 로드되지 않은 경우 에러 반환
    if model is None:
        return jsonify({"error": "Model not loaded"}), 500

    # 요청에 이미지 데이터가 없는 경우
    if 'image' not in request.json:
        return jsonify({"error": "No image data provided"}), 400

    try:
        # 요청 JSON에서 base64 인코딩된 이미지 추출
        image_data = request.json['image']
        image_bytes = base64.b64decode(image_data)

        # OpenCV를 사용하여 이미지 디코딩
        nparr = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if image is None:
            return jsonify({"error": "Could not decode image"}), 400

        # YOLO 모델을 이용한 객체 탐지 수행
        results = model(image)
        
        # 탐지된 사람 수 계산 (COCO 기준 class 0 = 'person')
        person_count = 0
        for result in results:
            if result.boxes is not None:
                # result.boxes.cls는 탐지된 객체의 클래스 ID 텐서
                person_count += torch.sum(result.boxes.cls == 0).item()
            
        logging.info(f"Image analyzed, detected {person_count} people.")

        # JSON 형태로 결과 반환
        return jsonify({"person_count": person_count})

    except Exception as e:
        # 분석 과정에서 예외 발생 시 에러 메시지 반환
        logging.error(f"Error during crowd analysis: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    # 실행 환경 확인 (GPU/CPU)
    if torch.cuda.is_available():
        logging.info("CUDA is available. Using GPU.")
    else:
        logging.info("CUDA is not available. Using CPU.")
    
    # Flask 서버 실행 (0.0.0.0:5000)
    app.run(host='0.0.0.0', port=5000, debug=False)
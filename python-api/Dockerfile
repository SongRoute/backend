# Python 3.10 slim 이미지 기반으로 빌드
FROM python:3.10-slim

# 컨테이너 내부 작업 디렉토리 설정
WORKDIR /app

# OpenCV 및 이미지 처리를 위해 필요한 라이브러리 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libsm6 \
    libxext6 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip --default-timeout=1200 install --no-cache-dir -r requirements.txt

# YOLO 설정 디렉토리 생성 및 권한 부여
RUN mkdir -p /root/.config/Ultralytics && chmod -R 777 /root/.config/Ultralytics

# Gunicorn 설치 (WSGI 서버)
RUN pip install --no-cache-dir gunicorn

# YOLO 설정 경로 환경 변수
ENV YOLO_CONFIG_DIR=/root/.config/Ultralytics

# Flask API 서버 코드 복사
COPY app.py .

# YOLO 가중치 파일 복사 (모델 실행에 필요)
COPY weights/best_m.pt ./weights/best_m.pt

# Flask 서버가 열릴 포트 (5000) 노출
EXPOSE 5000

# 컨테이너 실행 시 app.py 실행 (Flask 서버 시작)
#CMD ["python", "app.py"]
# 컨테이너 실행 시 Gunicorn으로 Flask 앱 실행
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:5000", "app:app"]
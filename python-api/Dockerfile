#FROM ubuntu:latest
#LABEL authors="ekQnd"
#
#ENTRYPOINT ["top", "-b"]

FROM python:3.10-slim

# (선택) 이미지 다운로드 등 보조 유틸
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 의존성 설치
COPY requirements.txt .
RUN apt-get update && apt-get install -y libgl1 libglib2.0-0
RUN pip install --no-cache-dir -r requirements.txt

# 앱 복사
COPY . .

# 모델/임시 폴더
RUN mkdir -p /app/weights /app/tmp

# FastAPI 실행 (YOLO API)
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5000"]


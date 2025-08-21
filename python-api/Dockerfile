# backend/python-api/Dockerfile
FROM python:3.10-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libsm6 \
    libxext6 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip --default-timeout=1200 install --no-cache-dir -r requirements.txt
COPY app.py .
COPY weights/best.pt ./weights/best.pt
EXPOSE 5000
CMD ["python", "app.py"]
# backend/python-api/Dockerfile
# Use a slim Python image
FROM python:3.10-slim

# Set the working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libsm6 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

# Copy the requirements file and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application code and weights
COPY app.py .
COPY weights/best.pt ./weights/best.pt

# Expose the Flask port
EXPOSE 5000

# Run the Flask app
CMD ["python", "app.py"]

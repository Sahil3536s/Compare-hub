# CompareHub ML Service — Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install dependencies first (layer caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app/ ./app/
COPY training/ ./training/

# Create directories for models and data
RUN mkdir -p models data reports

# Mount point for pre-trained model (injected via volume or copy at deploy time)
VOLUME ["/app/models"]

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

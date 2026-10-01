FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY services/ai-learning-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY services/ai-learning-service/main.py ./main.py
COPY services/ai-learning-service/app ./app
COPY services/ai-learning-service/docker-entrypoint.sh ./docker-entrypoint.sh

RUN useradd --create-home --uid 10001 appuser \
    && chown -R appuser:appuser /app \
    && chmod +x /app/docker-entrypoint.sh \
    && command -v setpriv

EXPOSE 8000
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

"""
ai-learning-service
Adaptive Learning & DeepTutor Runtime Engine
"""
from fastapi import FastAPI

app = FastAPI(
    title="IELTSPath AI Learning Service",
    description="Adaptive Learning, DeepTutor Mastery Path, and Tutor Runtime",
    version="1.0.0",
)


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": "ai-learning-service"}


@app.get("/api/ai-learning/health")
async def api_health_check():
    return {"status": "UP", "service": "ai-learning-service"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)


"""FastAPI dependencies shared by the path and tutor routes."""

from fastapi import Depends

from app.application.path_orderer import PathOrderer
from app.application.path_service import PathService
from app.clients.content_service import ContentServiceClient
from app.clients.user_service import UserServiceClient
from app.config import LlmSettings, Settings, get_settings
from app.learning.ordering_llm import OrderingLlm
from app.llm.client import ChatCompletionsClient
from app.persistence.postgres_learning_store import PostgresLearningStore
from app.practice.store import PracticeStore
from app.tutor.engine import Chat, TutorEngine
from app.tutor.memory import Complete, LearnerMemoryService, LearnerMemoryStore
from app.tutor.session_store import TutorSessionStore


def get_path_service(settings: Settings = Depends(get_settings)) -> PathService:
    store = PostgresLearningStore(settings.database_url.get_secret_value())
    return PathService(
        store,
        UserServiceClient(settings.user_service_base_url),
        ContentServiceClient(settings.content_service_base_url),
        orderer=PathOrderer(store, OrderingLlm()),
    )


def get_content_client(settings: Settings = Depends(get_settings)) -> ContentServiceClient:
    return ContentServiceClient(settings.content_service_base_url)


def get_tutor_sessions(settings: Settings = Depends(get_settings)) -> TutorSessionStore:
    return TutorSessionStore(settings.database_url.get_secret_value())


def get_practice_store(settings: Settings = Depends(get_settings)) -> PracticeStore:
    return PracticeStore(settings.database_url.get_secret_value())


def get_learner_memory_store(settings: Settings = Depends(get_settings)) -> LearnerMemoryStore:
    return LearnerMemoryStore(settings.database_url.get_secret_value())


def get_tutor_engine(settings: Settings = Depends(get_settings)) -> TutorEngine:
    url = settings.database_url.get_secret_value()
    return TutorEngine(TutorSessionStore(url), PostgresLearningStore(url), practice=PracticeStore(url),
                       memory=LearnerMemoryService(LearnerMemoryStore(url)))


def get_tutor_chat() -> Chat | None:
    """The configured chat model, read per request like path ordering; ``None`` when no model or key is set."""
    settings = LlmSettings()
    return ChatCompletionsClient(settings).chat if settings.configured else None


def get_memory_complete() -> Complete | None:
    """The configured text completion client, using the same model and key as tutor chat."""
    settings = LlmSettings()
    return ChatCompletionsClient(settings).complete if settings.configured else None

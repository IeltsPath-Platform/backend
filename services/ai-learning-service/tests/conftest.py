"""Suite-wide safety: no test may reach a real LLM provider.

``LlmSettings`` also reads a local ``.env``; environment values take precedence, so blanking the
model and key here leaves the LLM unconfigured unless a test points it at a local stub.
"""

import os

os.environ["AI_LEARNING_LLM_MODEL"] = ""
os.environ["AI_LEARNING_LLM_API_KEY"] = ""

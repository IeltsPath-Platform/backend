"""The service must not import or build against DeepTutor.

``third_party/deeptutor`` is a reference clone to read while porting. The mastery
engine lives in ``app.mastery``; nothing in the service may import ``deeptutor`` or
put the clone on the build or import path.
"""

import ast
from pathlib import Path
import unittest

SERVICE = Path(__file__).resolve().parents[1]
REPOSITORY = SERVICE.parents[1]

# What each build or configuration file may not mention. The README may credit DeepTutor as the
# origin of the ported engine, but must not tell anyone to build or test against the clone.
FORBIDDEN_BUILD_REFERENCES: dict[Path, tuple[str, ...]] = {
    SERVICE / "Dockerfile": ("third_party", "deeptutor"),
    SERVICE / "docker-entrypoint.sh": ("third_party", "deeptutor"),
    SERVICE / "requirements.txt": ("third_party", "deeptutor"),
    SERVICE / "requirements-test.txt": ("third_party", "deeptutor"),
    REPOSITORY / "docker-compose.yml": ("third_party", "deeptutor"),
    SERVICE / "README.md": ("third_party", "pythonpath"),
}


def _imports_deeptutor(source: str, filename: str = "<source>") -> bool:
    for node in ast.walk(ast.parse(source, filename=filename)):
        if isinstance(node, ast.Import) and any(
            alias.name == "deeptutor" or alias.name.startswith("deeptutor.") for alias in node.names
        ):
            return True
        if isinstance(node, ast.ImportFrom) and node.level == 0 and node.module and (
            node.module == "deeptutor" or node.module.startswith("deeptutor.")
        ):
            return True
    return False


def _python_sources() -> list[Path]:
    files = [SERVICE / "main.py"]
    for root in (SERVICE / "app", SERVICE / "tests"):
        files.extend(path for path in root.rglob("*.py") if "__pycache__" not in path.parts)
    return files


class NoDeepTutorDependencyTest(unittest.TestCase):
    def test_no_python_file_imports_deeptutor(self):
        importing = [
            path.relative_to(SERVICE).as_posix()
            for path in _python_sources()
            if _imports_deeptutor(path.read_text(encoding="utf-8"), str(path))
        ]
        self.assertEqual(importing, [])

    def test_the_guard_sees_an_import_when_there_is_one(self):
        self.assertTrue(_imports_deeptutor("from deeptutor.learning import policy"))
        self.assertTrue(_imports_deeptutor("def f():\n    import deeptutor.services.llm"))
        self.assertFalse(_imports_deeptutor("from app.mastery import policy  # ported from deeptutor"))

    def test_build_and_configuration_do_not_reference_the_clone(self):
        offending = [
            f"{path.relative_to(REPOSITORY).as_posix()}: {word}"
            for path, words in FORBIDDEN_BUILD_REFERENCES.items()
            for word in words
            if word in path.read_text(encoding="utf-8").lower()
        ]
        self.assertEqual(offending, [])

    def test_the_image_copies_only_the_service_directory(self):
        copied = [line for line in (SERVICE / "Dockerfile").read_text(encoding="utf-8").splitlines()
                  if line.strip().upper().startswith(("COPY", "ADD"))]
        self.assertTrue(copied)
        sources = [[token for token in line.split()[1:-1] if not token.startswith("--")] for line in copied]
        self.assertTrue(all(source and all(path.startswith("services/ai-learning-service/") for path in source)
                            for source in sources), copied)


if __name__ == "__main__":
    unittest.main()

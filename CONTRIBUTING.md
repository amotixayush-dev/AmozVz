# Contributing to AmozVz

Thank you for your interest in contributing to **AmozVz**!

AmozVz is an open-source project dedicated to making natural voice dictation accessible, private, and intelligent for everyone.

---

## Getting Started

### 1. Fork & Clone
```bash
git clone https://github.com/amotixayush-dev/AmozVz.git
cd AmozVz
```

### 2. Android App Development
- Open the `android` folder in **Android Studio Hedgehog / Jellyfish / Ladybug or newer**.
- Android SDK Platform 34/35.
- Run tests:
  ```bash
  cd android
  ./gradlew testDebugUnitTest
  ```
- Build APK:
  ```bash
  ./gradlew assembleDebug
  ```

### 3. Server & AI Agent Development
```bash
cd server
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
pip install pytest

# Run tests
PYTHONPATH=. python3 -m unittest discover -s tests -p "test_*.py" -v
```

---

## Pull Request Guidelines

1. **Branch Naming**: `feature/your-feature-name` or `fix/issue-description`.
2. **Commit Messages**: Clear, descriptive commit messages (e.g. `feat: add support for Italian hesitation markers`).
3. **Tests**: Ensure both Python unit tests and Android unit tests pass.
4. **Documentation**: Update relevant markdown docs in `/docs` if modifying APIs or architecture.

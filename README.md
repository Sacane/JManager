# 💰 JManager — Simplified Personal Finance Management

JManager is a modern and user-friendly application designed to help individuals manage their personal finances: transaction tracking, forecasts, multiple booklets, custom tags and analytical visualizations.

This README provides an overview for developers and users, describes the architecture and lists commands to run the project locally.

---

## 🌟 Key Features

- Transaction management: create, update and delete transactions.
- Forecasted transactions: differentiate current balances from forecasted balances.
- Multi-booklets: separate accounts/registers independently.
- Custom tags and search: fine-grained categorization of transactions.
- Recurring rules: scheduling of regular transactions.
- CSV import and batch processing.
- Dashboards and metrics: charts and reports to analyze financial history.

---

## 🏗️ Project Architecture

The project follows a hexagonal architecture (clear separation between domain, infrastructure and UI) and is organized into Gradle modules:

- `client/`: Nuxt 3 (Vue 3) frontend
- `domain/`: business logic (Kotlin)
- `infra/`: infrastructure and Spring Boot application

---

## ⚙️ Technical Stack (extracted from project configs)

Backend
- Language: Kotlin (Kotlin plugin v2.0.0)
- Framework: Spring Boot 3.4.0
- Build: Gradle (wrapper included)
- DB migrations: Flyway (11.14.1)
- Auth: JJWT (0.12.6)
- Tests: JUnit / Spring Boot Test / Testcontainers (1.21.3)
- Coverage: JaCoCo (0.8.12)

Frontend
- Framework: Nuxt 3 (Vue 3)
- Package manager: pnpm (see `client/package.json`)
- Styling: UnoCSS, PrimeVue

Infra & quality tools
- SonarQube (Gradle plugin configured)
- ShadowJar (to build a standalone executable)
- Testcontainers for integration tests
- Actuator, Micrometer, Springdoc OpenAPI for observability and docs (added in infra)

---

## 🔢 Versioning Strategy

The project version is stored in `gradle.properties` (`version=x.y.z`) and is automatically bumped on pushes to `master`.

Commit type to bump mapping:

- `fix:` -> patch bump (`x.y.z+1`)
- `feat:`, `chore:`, `patch:` -> minor bump (`x.y+1.0`)
- `release:` -> major bump (`x+1.0.0`)

The CI updates `gradle.properties`, commits the new version, and builds `executables/Jmanager-<version>.jar`.

---

## 📦 Prerequisites

Install the following tools on your machine:

- JDK 21 (or newer)
- Git
- Node.js (recommended: 18+)
- pnpm
- Docker (optional — recommended for PostgreSQL and SonarQube locally)
- Gradle wrapper (provided in the repo; use the included `gradlew` or `gradlew.bat`)

---

## 🚀 Run the project locally

1) Clone the repository

```bash
git clone <repo-url>
cd JManager
```

2) Start PostgreSQL locally

- Option A — Docker (recommended):

```bash
docker run --rm -e POSTGRES_USER=jmanager -e POSTGRES_PASSWORD=jmanager -e POSTGRES_DB=jmanager -p 5432:5432 postgres:15
```

- Option B — local DB: configure a PostgreSQL instance and update configuration files accordingly.

3) Build and run the backend (`infra`)

```bash
# from the project root (Unix/macOS)
./gradlew :infra:bootRun
# on Windows (cmd.exe / PowerShell)
gradlew.bat :infra:bootRun

# or to create a standalone JAR
./gradlew :infra:shadowJar
java -jar executables/Jmanager-<version>.jar
```

4) Start the frontend (`client`)

```bash
cd client
pnpm install
pnpm run dev
```

The frontend is configured to call the backend API by default — adjust `client/env.dev.config.json` / `client/env.config.json` if needed.

---

## 🧪 Tests

- Run all unit and integration tests:

```bash
# use gradle wrapper
./gradlew test
# or on Windows
gradlew.bat test
```

- Integration tests that rely on Testcontainers require Docker to be running.

---

## 🚚 Deployment Dispatch (CI)

On `master`, the workflow:

- bumps the version,
- builds the versioned JAR,
- publishes it as a GitHub Release asset (`v<version>`),
- triggers the Deploy repository through `repository_dispatch` (`jmanager_deploy`).

Payload sent to Deploy includes:

- `jar_version`
- `jar_download_url`
- `jar_file_name`

---

## 🛠️ Infrastructure tools & dependencies (see `infra/build.gradle.kts`)

The `infra` module uses several libraries to support operation and development:

- Spring Boot Starter Web / Data JPA / Security
- Flyway for schema migrations
- JJWT for JWT handling
- Testcontainers, H2 and Rest-Assured for tests
- JaCoCo for coverage
- ShadowJar to package the application
- Actuator, DevTools, Validation, Micrometer Prometheus and Springdoc OpenAPI for monitoring and documentation

---

## 🧩 Contributing

Contributions are welcome — open an issue to discuss a feature or bug.

- Fork the repository
- Create a branch feature/your-feature
- Submit a merge request with tests and a clear description

---
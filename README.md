# CompareHub — Universal Comparison Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)]()
[![React](https://img.shields.io/badge/React-18.3-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)]()
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)]()

> **CompareHub** is a production-grade, multi-engine comparison platform that aggregates and analyzes real-time prices across **E-Commerce Shopping** (Amazon, Flipkart, Croma), **Airlines & Flights**, and **On-Demand Rides** (Uber, Ola, Rapido) in one unified interface.

---

## 🏛️ System Architecture

```
                                  +-----------------------+
                                  |   React 18 + Vite     |
                                  | (Vercel / Nginx SPA)  |
                                  +-----------+-----------+
                                              |
                                     HTTPS / REST JSON
                                              |
                                  +-----------v-----------+
                                  |   Spring Boot 3.3.4   |
                                  | (Render/Railway/AWS)  |
                                  +-----+-----+-----+-----+
                                        |     |     |
               +------------------------+     |     +-------------------------+
               |                              |                               |
       +-------v--------+             +-------v--------+              +-------v--------+
       |   PostgreSQL   |             |  Managed Redis |              | External APIs  |
       | Flyway V1 - V4 |             | 2-Tier Caching |              | Multi-Provider |
       +----------------+             +----------------+              +----------------+
```

---

## 🚀 Key Features

- **Multi-Merchant Shopping Comparison**: Concurrent aggregation across Amazon, Flipkart, Croma, and OpenCommerce feeds with normalized product DTOs.
- **Flight Fare Intelligence**: Route durations, multi-stop paths, airline badges, and lowest fare filters.
- **Ride & Cab Fare Estimates**: Pickup ETA, estimated fare ranges, and direct booking deeplinks.
- **Deterministic Best Value Ranking**: Weighted multi-factor normalization algorithms (Price, Rating, Discount, Delivery, ETA).
- **Interactive Price Trend Analysis**: 7D / 30D / 90D price history charts with dynamic averages and price-drop notifications.
- **Resilient Fault Isolation**: Resilience4j circuit breakers and timeouts ensure third-party provider outages never crash comparisons.
- **Security Hardened**: BCrypt (cost factor 12), strict CORS origins, rate limiting (10 req/min auth, 30 req/min search), CSP & HSTS security headers.
- **Fully Automated Testing**: 82 JUnit 5 backend tests + 18 Vitest frontend tests (100% mocked, zero external API costs).

---

## 💻 Tech Stack

- **Frontend**: React 18, Vite, Tailwind CSS, Axios, React Router, Vitest, React Testing Library, JSDOM.
- **Backend**: Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Security, Flyway, Resilience4j, Lettuce Redis, Caffeine, Lombok, JUnit 5, Mockito.
- **Database & Cache**: PostgreSQL 16 (Flyway migrations `V1` to `V4`), Redis 7 / Caffeine in-memory cache.
- **Deployment**: Vercel (Frontend SPA), Render / Railway / AWS (Backend Spring Boot), Docker & Docker Compose.

---

## 🛠️ Local Development Setup

### 1. Prerequisites
- **Java 21 JDK** (e.g. Eclipse Temurin or OpenJDK)
- **Node.js 20+** and **npm**
- **PostgreSQL 16+** (running on port 5432)
- **Redis 7+** (optional, fallback to Caffeine enabled)
- **Docker & Docker Compose** (optional)

---

### 2. Environment Variables Matrix

#### Backend (`backend/.env` or system environment):
| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `PORT` | Server HTTP port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` or `prod`) | `dev` |
| `DB_URL` / `DATABASE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/comparehub` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_ENABLED` | Enable/disable Redis caching | `true` |
| `JWT_SECRET` | 256-bit hexadecimal signing key | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | `http://localhost:5173,http://localhost:3000` |

#### Frontend (`frontend/.env`):
| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | Backend Spring Boot API base path | `http://localhost:8080/api` |

---

### 3. Running Backend Locally

1. Create PostgreSQL database:
   ```sql
   CREATE DATABASE comparehub;
   ```
2. Navigate to `backend/` directory:
   ```powershell
   cd backend
   ```
3. Run Spring Boot application (Flyway migrations will run automatically):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
4. Verify backend health endpoint:
   ```bash
   curl http://localhost:8080/api/health
   ```
   **Response:**
   ```json
   {
     "status": "UP",
     "service": "CompareHub Backend",
     "timestamp": "2026-09-03T10:00:00Z",
     "database": "CONNECTED",
     "cache": "ACTIVE"
   }
   ```

5. From now on, you don't need to run Maven manually. You can start everything with:

docker compose up -d

stop everything with:

docker compose down

and check status with:

docker ps

If you changed code and want Docker to rebuild it:

docker compose up -d --build


---

### 4. Running Frontend Locally

1. Navigate to `frontend/` directory:
   ```powershell
   cd frontend
   ```
2. Install npm dependencies:
   ```powershell
   npm install
   ```
3. Start Vite development server:
   ```powershell
   npm run dev
   ```
4. Access the web app at **`http://localhost:5173`**.

---

### 5. Running with Docker Compose

Run the entire platform (PostgreSQL, Redis, Backend, Frontend) with a single command:
```bash
docker-compose up --build
```
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`

---

## 🧪 Testing

All automated tests use isolated mocks with **zero external API calls**.

### Run Backend Tests (JUnit 5 + Mockito + MockMvc):
```powershell
cd backend
.\mvnw.cmd test
```
*Result: 82 tests run, 0 failures, 0 errors, 100% BUILD SUCCESS.*

### Run Frontend Tests (Vitest + React Testing Library):
```powershell
cd frontend
npm run test:run
```
*Result: 7 test files passed, 18 tests passed, 100% SUCCESS.*

### Run Full Test Suite in a Single Command:
```powershell
cd backend; .\mvnw.cmd test; cd ../frontend; npm run test:run
```

---

## 📦 Building for Production

### Build Backend JAR:
```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
```
*Output: `backend/target/comparehub-backend-0.0.1-SNAPSHOT.jar`*

### Build Frontend Static Bundle:
```powershell
cd frontend
npm run build
```
*Output: `frontend/dist/`*

---

## 🌐 Production Deployment

### 1. Frontend Deployment to Vercel

1. Push your repository to GitHub.
2. Sign in to [Vercel](https://vercel.com) and click **"Add New Project"**.
3. Import the repository and set the **Root Directory** to `frontend`.
4. Build Settings:
   - **Framework Preset**: `Vite`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Environment Variables:
   - `VITE_API_BASE_URL`: `https://your-backend.onrender.com/api`
6. Click **Deploy**. Vercel will use `frontend/vercel.json` to handle client-side React Router routing rewrites automatically.

---

### 2. Backend Deployment to Render / Railway

#### Option A: Dockerfile (Recommended)
1. In Render or Railway, select **"New Web Service"** and connect your repository.
2. Set **Root Directory** to `backend`.
3. Choose **Dockerfile** as the build runtime.
4. Set Environment Variables:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: `jdbc:postgresql://<managed-postgres-host>:5432/<dbname>`
   - `SPRING_DATASOURCE_USERNAME`: `<db-user>`
   - `SPRING_DATASOURCE_PASSWORD`: `<db-password>`
   - `REDIS_URL`: `rediss://default:<password>@<managed-redis-host>:6379`
   - `JWT_SECRET`: `<your-random-256-bit-key>`
   - `CORS_ALLOWED_ORIGINS`: `https://your-frontend.vercel.app`
5. Set Health Check Path: `/api/health`
6. Deploy!

#### Option B: Native Maven / Java 21
1. **Build Command**: `./mvnw clean package -DskipTests`
2. **Start Command**: `java -jar target/*.jar`

---

### 3. Managed PostgreSQL Database Setup

1. Create a managed PostgreSQL database (on Render, Railway, Supabase, Neon, or AWS RDS).
2. Note your connection credentials: Host, Port (5432), Database Name, Username, and Password.
3. Configure `DATABASE_URL` as JDBC format:
   `jdbc:postgresql://<host>:5432/<database>?sslmode=require`
4. On startup, Spring Boot and Flyway will automatically execute migrations `V1` through `V4`.

---

## 🔒 Security & Hardening Checklist

- [x] **No Secrets in Code**: All sensitive credentials read dynamically via environment variables.
- [x] **Strict CORS**: Restricted strictly to configured frontend production domains.
- [x] **Rate Limiting**: Sliding-window rate limit on authentication (10 req/min) and search endpoints (30 req/min).
- [x] **BCrypt Hashing**: Password hashing with cost factor 12.
- [x] **Resilient Fallbacks**: Third-party provider timeouts and circuit breakers prevent cascade failures.
- [x] **No Java Stack Traces**: Centralized exception handler sanitizes all API error responses.
- [x] **Security Headers**: HSTS, CSP, X-Frame-Options: DENY, X-Content-Type-Options: nosniff.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

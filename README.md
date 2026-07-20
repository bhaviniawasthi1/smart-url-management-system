<div align="center">
  <h1>⚡ LynkForge</h1>
  <p><strong>Shorten. Track. Own Your Links.</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Java-22-orange?logo=openjdk" alt="Java 22">
    <img src="https://img.shields.io/badge/Spring_Boot-3.4.1-brightgreen?logo=spring" alt="Spring Boot 3.4.1">
    <img src="https://img.shields.io/badge/JWT-HS256-ff69b4?logo=jsonwebtokens" alt="JWT">
    <img src="https://img.shields.io/badge/Docker-ready-blue?logo=docker" alt="Docker">
    <img src="https://img.shields.io/badge/license-MIT-green" alt="MIT">
  </p>
  <br>
</div>

---

**LynkForge** is a smart URL management platform built with Java 22 and Spring Boot 3. It turns long, messy links into short, clean, trackable ones. You can password-protect them, set them to auto-expire, and see exactly who clicked them — all from one dashboard.

### ✨ Features

- **🔗 URL Shortening** — Convert long URLs into short, shareable links using secure Base62 encoding (7 characters, ~3.5 trillion combinations)
- **🔒 Password Protection** — Secure sensitive links behind a BCrypt-hashed password. Only the right people get in
- **⏰ Link Expiration** — Set links to auto-expire after 1 hour, 24 hours, 7 days, or 30 days
- **📊 Click Analytics** — Track total clicks, daily/weekly/monthly stats, IP addresses, user agents, and referrers per URL
- **🛡️ Admin Controls** — Manage users, view platform-wide stats, enable/disable accounts
- **⚡ Blazing Fast** — Built on Spring Boot 3 with stateless JWT auth, in-memory rate limiting, and embedded Tomcat

---

## 🚀 Quick Start (Local Development)

### Prerequisites

- [JDK 22](https://jdk.java.net/22/) (`java -version` should show `22`)
- [Maven 3.9+](https://maven.apache.org/) (or use the included `mvnw.cmd` wrapper)

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/lynkforge.git
cd lynkforge

# 2. Build and run (no setup needed — uses H2 in-memory database)
.\mvnw.cmd spring-boot:run

# 3. Open in browser
http://localhost:8080
```

That's it. No database installation, no environment variables needed for development.

### Default Configuration (dev)

| Setting | Value |
|---------|-------|
| Database | H2 in-memory (data lost on restart) |
| JWT Secret | Base64-encoded default key |
| Token Expiry | 24 hours |
| Rate Limit | 60 req/min general, 10 req/min for URL creation |
| Short URL Domain | `http://localhost:8080` |

---

## 🐳 Docker

```bash
# Build and run with Docker
docker build -t lynkforge .
docker run -p 8080:8080 lynkforge
```

---

## 🔧 Configuration via Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | *(embedded default)* | Base64-encoded 256-bit key for JWT signing |
| `JWT_EXPIRATION_MS` | `86400000` | JWT expiry in milliseconds (24h) |
| `JWT_ISSUER` | `smart-url-management-system` | JWT issuer claim |
| `SHORT_URL_DOMAIN` | `http://localhost:8080` | Domain used in generated short URLs |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile (`dev` for H2, `mysql` for MySQL) |
| `DB_URL` | *(H2 default)* | JDBC URL for MySQL |
| `DB_USERNAME` | — | MySQL username |
| `DB_PASSWORD` | — | MySQL password |
| `PORT` | `8080` | Server port (Render sets this automatically) |

---

## 📡 API Overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | Public | Register a new user |
| `POST` | `/api/v1/auth/login` | Public | Login and receive JWT |
| `GET` | `/r/{shortCode}` | Public | Redirect to original URL |
| `GET` | `/r/{shortCode}/password` | Public | Password entry page |
| `POST` | `/api/v1/urls` | JWT | Create a short URL |
| `GET` | `/api/v1/urls` | JWT | List user's URLs |
| `GET` | `/api/v1/urls/{id}` | JWT | Get URL by ID |
| `PUT` | `/api/v1/urls/{id}` | JWT | Update a URL |
| `DELETE` | `/api/v1/urls/{id}` | JWT | Delete a URL |
| `POST` | `/api/v1/urls/{shortCode}/verify-password` | Public | Verify password-protected URL |
| `GET` | `/api/v1/analytics/urls/{id}` | JWT | Per-URL analytics |
| `GET` | `/api/v1/analytics/summary` | JWT | Dashboard summary stats |
| `GET` | `/api/v1/admin/users` | Admin | List all users |
| `PUT` | `/api/v1/admin/users/{id}/status` | Admin | Enable/disable a user |
| `DELETE` | `/api/v1/admin/users/{id}/urls/{urlId}` | Admin | Delete any user's URL |
| `GET` | `/api/v1/admin/platform-stats` | Admin | Platform-wide statistics |

Full interactive API documentation is available at `/swagger-ui.html` when the app is running.

---

## 📁 Project Structure

```
src/main/java/com/smarturl/
├── SmartUrlApplication.java        # Entry point
├── config/                         # Spring configuration classes
│   ├── SecurityConfig.java         # JWT filter, role-based auth, CORS, headers
│   ├── SwaggerConfig.java          # OpenAPI / Swagger UI setup
│   ├── WebConfig.java              # CORS mapping
│   ├── JpaAuditingConfig.java      # Created-at / updated-at timestamps
│   ├── RateLimiterRegistrationConfig.java
│   ├── ReservedAliasesConfig.java  # Reserved short codes
│   └── SchedulingConfig.java       # Scheduled cleanup tasks
├── controller/                     # REST and view controllers
│   ├── AuthenticationController.java
│   ├── UrlController.java
│   ├── RedirectController.java
│   ├── RedirectViewController.java
│   ├── AnalyticsController.java
│   ├── AdminController.java
│   └── HomeController.java
├── dto/                            # Request/Response DTOs (11 classes)
├── entity/                         # JPA entities (User, ShortUrl, ClickEvent)
├── exception/                      # Custom exceptions + GlobalExceptionHandler
├── repository/                     # Spring Data JPA repositories
├── security/                       # JWT authentication filter
├── service/                        # Business logic layer
│   ├── AuthenticationService.java
│   ├── UrlService.java
│   ├── AnalyticsService.java
│   └── AdminService.java
├── util/                           # Utilities (JWT, Base62, Rate Limiter)
└── validation/                     # Custom @Password annotation + validator
```

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language** | Java 22 | Records, pattern matching, sealed classes |
| **Framework** | Spring Boot 3.4.1 | Auto-configuration, embedded Tomcat |
| **Security** | Spring Security + JJWT 0.12.6 | Stateless JWT authentication |
| **Database** | H2 (dev) / MySQL (prod) | Zero-setup dev, production-ready prod |
| **ORM** | Spring Data JPA (Hibernate) | Automated repository generation |
| **Frontend** | Thymeleaf + Vanilla CSS | Server-rendered templates with client-side JS |
| **API Docs** | SpringDoc OpenAPI 2.7.0 | Interactive Swagger UI |
| **Build** | Maven + Wrapper | Dependency management |
| **Container** | Docker multi-stage | Minimal JRE 22 production image |

---

## 🚢 Deploy on Render.com

1. Push this repo to GitHub
2. Go to [render.com](https://render.com) → **New Web Service** → connect your repo
3. Render auto-detects the Dockerfile — click **Create**
4. Add environment variables:
   - `JWT_SECRET` — generate a 256-bit Base64 key
   - `SHORT_URL_DOMAIN` — `https://your-app.onrender.com`
   - `SPRING_PROFILES_ACTIVE` — `dev`
5. Your app is live at `https://your-app.onrender.com`

---

## 🔐 Security Highlights

- **BCrypt password hashing** (adaptive, ~200ms per check)
- **Stateless JWT** — no server-side sessions, scales horizontally
- **Rate limiting** — IP-based sliding window (60 req/min, 10 req/min for URL creation)
- **Security headers** — X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Cache-Control
- **Input validation** — Jakarta Bean Validation + custom `@Password` annotation
- **Ownership checks** — users can only access their own URLs
- **Reserved alias protection** — prevents short code collision with system routes

---

## 🧪 Running Tests

```bash
.\mvnw.cmd test
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <p>Built by <strong>Bhavini Awasthi</strong></p>
  <p>
    <a href="https://www.linkedin.com/in/bhaviniawasthi/">LinkedIn</a>
  </p>
</div>

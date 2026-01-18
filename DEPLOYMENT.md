# Deployment Guide - Force Sub Bot

Ada 3 cara deployment yang tersedia:

## 🚀 Option 1: Full Stack (Bot + MongoDB + Redis) - RECOMMENDED

Untuk production dengan semua dependencies dalam Docker.

### Setup
```bash
# 1. Setup environment
cp .env.example .env
nano .env

# 2. Start semua services
docker-compose up -d

# 3. Lihat logs
docker-compose logs -f
```

**Includes:**
- ✅ Forcesub Bot
- ✅ MongoDB Database
- ✅ Redis Cache
- ✅ Auto health checks
- ✅ Auto restart

**File:** `docker-compose.yml`

---

## 📦 Option 2: Bot + MongoDB Only

Untuk setup tanpa Redis (jika tidak dibutuhkan caching).

### Setup
```bash
# 1. Setup environment
cp .env.example .env
nano .env

# 2. Start bot dan MongoDB
docker-compose -f docker-compose.yml up -d forcesub-bot mongodb

# 3. Lihat logs
docker-compose logs -f forcesub-bot
```

**Includes:**
- ✅ Forcesub Bot
- ✅ MongoDB Database
- ❌ Redis (disabled)

**Note:** Comment out Redis configuration di `application.properties` jika tidak digunakan.

---

## 🔧 Option 3: Standalone Bot (External Database)

Untuk deployment dimana MongoDB sudah ada di server/cloud terpisah.

### Prerequisites
MongoDB harus sudah running di:
- Local: `localhost:27017`
- Remote: URL MongoDB Atlas/Cloud

### Setup

**A. Dengan Docker Compose:**
```bash
# 1. Setup environment dengan MongoDB URI
cp .env.example .env
nano .env

# Set MONGODB_URI ke external database
MONGODB_URI=mongodb://your-server:27017/forcesub
# atau
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/forcesub

# 2. Start hanya bot
docker-compose -f docker-compose.standalone.yml up -d

# 3. Lihat logs
docker-compose -f docker-compose.standalone.yml logs -f
```

**B. Dengan Docker Run (Manual):**
```bash
# 1. Build JAR
mvn clean package -DskipTests

# 2. Build Docker image
docker build -f Dockerfile.simple -t forcesub-bot .

# 3. Run container
docker run -d \
  --name forcesub-bot \
  --restart unless-stopped \
  -p 8080:8080 \
  -e BOT_TOKEN="your_bot_token" \
  -e SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/forcesub" \
  -e DEFAULT_DATABASE_ID="-1002161694809" \
  -e ADMIN_ID="5761795187" \
  -v $(pwd)/td-bot:/app/td-bot \
  forcesub-bot

# 4. Lihat logs
docker logs -f forcesub-bot
```

**C. Tanpa Docker (Direct JAR):**
```bash
# 1. Build JAR
mvn clean package -DskipTests

# 2. Set environment variables
export BOT_TOKEN="your_bot_token"
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/forcesub"
export DEFAULT_DATABASE_ID="-1002161694809"

# 3. Run JAR
java -Xmx512m -Xms256m -jar target/forcesub-bot.jar
```

---

## 🗂️ File Structure

```
forcesub-bot/
├── docker-compose.yml              # Full stack (bot + mongodb + redis)
├── docker-compose.standalone.yml   # Standalone (external database)
├── Dockerfile                      # Multi-stage build (from source)
├── Dockerfile.simple              # Simple build (from JAR)
├── .env.example                   # Environment template
└── td-bot/                        # TDLib session data (auto-created)
```

---

## 📋 Environment Variables

### Required
```bash
BOT_TOKEN=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
```

### Optional
```bash
# Telegram API (uses example if not set)
TELEGRAM_API_ID=12345678
TELEGRAM_API_HASH=abcdef1234567890

# Application
DEFAULT_DATABASE_ID=-1002161694809
ADMIN_ID=5761795187
FORCESUB_SUBSCRIPTION_TTL_MINUTES=2

# JVM Memory
MAX_HEAP=512m
MIN_HEAP=256m
YOUNG_GEN=128m

# MongoDB (standalone only)
MONGODB_URI=mongodb://localhost:27017/forcesub
```

---

## 🔍 Common Commands

### Full Stack (docker-compose.yml)
```bash
# Start
docker-compose up -d

# Stop
docker-compose down

# Logs
docker-compose logs -f forcesub-bot

# Restart
docker-compose restart forcesub-bot

# Update & rebuild
docker-compose up -d --build
```

### Standalone (docker-compose.standalone.yml)
```bash
# Start
docker-compose -f docker-compose.standalone.yml up -d

# Stop
docker-compose -f docker-compose.standalone.yml down

# Logs
docker-compose -f docker-compose.standalone.yml logs -f

# Restart
docker-compose -f docker-compose.standalone.yml restart
```

### Manual Docker
```bash
# Build
docker build -f Dockerfile.simple -t forcesub-bot .

# Run
docker run -d --name forcesub-bot \
  -e BOT_TOKEN="..." \
  -e MONGODB_URI="..." \
  -v $(pwd)/td-bot:/app/td-bot \
  forcesub-bot

# Stop
docker stop forcesub-bot

# Start
docker start forcesub-bot

# Logs
docker logs -f forcesub-bot

# Remove
docker rm -f forcesub-bot
```

---

## 🗄️ Database Options

### Local MongoDB (Docker)
```bash
# docker-compose.yml sudah include MongoDB
docker-compose up -d
```

### Local MongoDB (Manual Install)
```bash
# Install MongoDB di system
# Ubuntu/Debian:
sudo apt install mongodb-org

# Start service
sudo systemctl start mongod

# Use standalone deployment
docker-compose -f docker-compose.standalone.yml up -d
```

### MongoDB Atlas (Cloud)
```bash
# 1. Buat cluster di mongodb.com/cloud/atlas
# 2. Get connection string
# 3. Set di .env
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/forcesub

# 4. Deploy
docker-compose -f docker-compose.standalone.yml up -d
```

---

## 🎯 Deployment Recommendations

| Scenario | Recommended Option | Reason |
|----------|-------------------|---------|
| **Production** | Full Stack | Semua dependencies managed |
| **VPS with low RAM** | Bot + MongoDB | Skip Redis untuk save memory |
| **Multiple Bots** | Standalone | Share MongoDB antar bots |
| **Cloud Platform** | Standalone | Use managed MongoDB |
| **Development** | Full Stack | Easy setup & teardown |

---

## 🔐 Production Checklist

- [ ] Change BOT_TOKEN di .env
- [ ] Set custom TELEGRAM_API_ID & TELEGRAM_API_HASH
- [ ] Set correct DEFAULT_DATABASE_ID
- [ ] Set ADMIN_ID
- [ ] Configure MongoDB authentication (jika external)
- [ ] Setup firewall untuk expose hanya port yang diperlukan
- [ ] Enable MongoDB backup (jika production)
- [ ] Backup td-bot/ directory secara berkala
- [ ] Monitor logs: `docker-compose logs -f`
- [ ] Setup monitoring (Prometheus/Grafana - optional)

---

## 🆘 Troubleshooting

### Bot tidak start
```bash
# Check logs
docker-compose logs forcesub-bot

# Common issues:
# - BOT_TOKEN invalid → check .env
# - MongoDB not ready → wait 10-20 seconds
# - Port 8080 used → change port in docker-compose
```

### MongoDB connection failed
```bash
# Full stack: check MongoDB container
docker-compose ps mongodb
docker-compose logs mongodb

# Standalone: check MongoDB URI
docker-compose -f docker-compose.standalone.yml logs forcesub-bot
```

### Out of memory
```bash
# Increase heap in .env
MAX_HEAP=1g
MIN_HEAP=512m

# Restart
docker-compose restart forcesub-bot
```

### Clean restart
```bash
# Remove all data
docker-compose down -v
rm -rf td-bot/

# Start fresh
docker-compose up -d
```

---

## 📊 Monitoring

```bash
# Resource usage
docker stats

# Container health
docker-compose ps

# Application logs
docker-compose logs -f forcesub-bot

# MongoDB logs
docker-compose logs -f mongodb

# Disk usage
du -sh td-bot/
```

---

## 🔄 Updates

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build

# Or for standalone
docker-compose -f docker-compose.standalone.yml up -d --build
```
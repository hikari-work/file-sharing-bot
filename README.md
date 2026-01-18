# Force-Sub Bot
Bot Telegram untuk menyimpan dan membagikan file/posting melalui link khusus dengan fitur Force Subscribe.
Bot ini dibangun dengan Java Spring Boot dan TDLight untuk performa optimal.

## ⚠️ Disclaimer
```
Saya tidak bertanggung jawab atas penyalahgunaan bot ini.
Bot ini dimaksudkan untuk membantu menyimpan dan membagikan file yang dapat diakses melalui link khusus.
Gunakan bot ini dengan risiko Anda sendiri, dan gunakan bot ini dengan bijak.
```

## ✨ Features

### Core Features
- ✅ **Force Subscribe** - Wajibkan user join channel sebelum akses konten
- ✅ **Multiple Channels** - Support banyak channel untuk force subscribe
- ✅ **Smart Caching** - Cache status subscription dengan TTL configurable
- ✅ **Batch Messages** - Kirim banyak pesan dalam satu link
- ✅ **Deep Linking** - Generate link khusus untuk setiap konten
- ✅ **Admin Management** - Kelola admin dengan mudah
- ✅ **Channel Management** - Tambah/hapus channel force sub secara dinamis

### Technical Features
- ⚡ **Non-blocking I/O** - Full reactive programming dengan Spring WebFlux
- 🚀 **High Performance** - Menggunakan TDLight (optimized TDLib)
- 💾 **MongoDB Database** - NoSQL untuk fleksibilitas
- ⚡ **Redis Cache** - Caching untuk performa optimal
- 🐳 **Docker Ready** - Multiple deployment options
- 🔄 **Auto Restart** - Container restart otomatis jika crash
- 📊 **Health Checks** - Built-in health monitoring

### User Features
- 📝 **Custom Messages** - Customize welcome & force sub messages
- 🎨 **Flexible Buttons** - Inline keyboard yang dapat disesuaikan
- 🔗 **Share Links** - Generate link untuk share konten
- 📱 **User Friendly** - Interface yang mudah digunakan

## 🛠 Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Reactive**: Spring WebFlux, Reactor
- **Telegram Library**: TDLight (TDLib wrapper)
- **Database**: MongoDB
- **Cache**: Redis
- **Build Tool**: Maven
- **Container**: Docker & Docker Compose

## 📋 Prerequisites

### Untuk Docker Deployment
- Docker & Docker Compose
- 512MB RAM minimum (1GB recommended)
- Telegram Bot Token dari [@BotFather](https://t.me/BotFather)

### Untuk Development
- JDK 21+
- Maven 3.8+
- MongoDB 7+
- Redis 7+ (optional)

## 🚀 Quick Start

### Option 1: Full Stack dengan Docker (Recommended)

```bash
# 1. Clone repository
git clone https://github.com/yourusername/forcesub-bot.git
cd forcesub-bot

# 2. Setup environment
cp .env.example .env
nano .env  # Edit dan isi BOT_TOKEN

# 3. Start semua services
chmod +x start-full.sh
./start-full.sh

# 4. Lihat logs
docker-compose logs -f forcesub-bot
```

### Option 2: Standalone (External Database)

```bash
# 1. Setup environment
cp .env.example .env
nano .env  # Edit BOT_TOKEN dan MONGODB_URI

# 2. Start bot
chmod +x start-standalone.sh
./start-standalone.sh

# 3. Lihat logs
docker-compose -f docker-compose.standalone.yml logs -f
```

### Option 3: Manual Development

```bash
# 1. Install dependencies
mvn clean install

# 2. Setup MongoDB & Redis (local)
# Install MongoDB: https://www.mongodb.com/docs/manual/installation/
# Install Redis: https://redis.io/docs/getting-started/installation/

# 3. Configure application.properties
nano src/main/resources/application.properties

# 4. Run application
mvn spring-boot:run
```

## ⚙️ Configuration

### Required Environment Variables

```bash
# Telegram Bot (REQUIRED)
BOT_TOKEN=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11

# Application
DEFAULT_DATABASE_ID=-1002161694809  # Channel ID untuk database
ADMIN_ID=5761795187                  # User ID admin
```

### Optional Environment Variables

```bash
# Telegram API (recommended untuk production)
TELEGRAM_API_ID=12345678
TELEGRAM_API_HASH=abcdef1234567890abcdef1234567890

# Force Subscribe
FORCESUB_SUBSCRIPTION_TTL_MINUTES=2  # TTL cache subscription (minutes)

# JVM Memory (Docker)
MAX_HEAP=512m
MIN_HEAP=256m
YOUNG_GEN=128m

# Database (Standalone only)
MONGODB_URI=mongodb://localhost:27017/forcesub
```

### Mendapatkan Credentials

1. **Bot Token**:
    - Buka [@BotFather](https://t.me/BotFather)
    - Kirim `/newbot`
    - Ikuti instruksi
    - Copy token yang diberikan

2. **API ID & API Hash** (Optional tapi recommended):
    - Buka https://my.telegram.org
    - Login dengan nomor Telegram
    - Pilih "API Development Tools"
    - Create new application
    - Copy API ID dan API Hash

3. **Channel/Group ID**:
    - Tambahkan bot ke channel/group
    - Forward pesan dari channel ke [@userinfobot](https://t.me/userinfobot)
    - Copy ID yang ditampilkan (contoh: -1002161694809)

4. **User ID**:
    - Kirim pesan ke [@userinfobot](https://t.me/userinfobot)
    - Copy ID Anda

## 📦 Deployment Options

### 1️⃣ Full Stack (Bot + MongoDB + Redis)
**Recommended untuk production**

```bash
docker-compose up -d
```

**Includes**:
- ✅ Force Sub Bot
- ✅ MongoDB Database
- ✅ Redis Cache
- ✅ Auto health checks
- ✅ Auto restart

**Requirements**: 1GB RAM

---

### 2️⃣ Bot + MongoDB Only
**Untuk VPS dengan RAM terbatas**

```bash
docker-compose up -d forcesub-bot mongodb
```

**Includes**:
- ✅ Force Sub Bot
- ✅ MongoDB Database
- ❌ Redis (disabled)

**Requirements**: 600MB RAM

---

### 3️⃣ Standalone (External Database)
**Untuk shared database atau cloud database**

```bash
# Edit .env dengan MONGODB_URI
nano .env

# Start bot
docker-compose -f docker-compose.standalone.yml up -d
```

**Includes**:
- ✅ Force Sub Bot only
- ❌ MongoDB (external)
- ❌ Redis (optional)

**Requirements**: 300MB RAM

**Database Options**:
- Local MongoDB
- MongoDB Atlas (Cloud)
- Shared MongoDB server

## 🎮 Bot Commands

### User Commands
- `/start` - Memulai bot
- `/start <code>` - Akses konten dengan kode

## 📝 Usage Guide

### Setup Bot

1. **Tambahkan bot ke Channel Database**:
   ```
   - Buka channel Anda
   - Add bot sebagai administrator
   - Berikan semua permission
   ```

2. **Tambahkan bot ke Force Subscribe Channel/Group**:
   ```
   - Buka channel/group untuk force sub
   - Add bot sebagai administrator
   - Bot akan otomatis detect member
   ```

3. **Set Admin**:
   ```
   - Kirim /admins ke bot
   - Pilih "Add Admin"
   - Forward pesan dari admin atau input User ID
   ```

### Membuat Link Share

1. **Save Message ke Database**:
   ```
   - Forward pesan ke bot di channel database
   - Bot akan generate kode unik
   ```

2. **Generate Link**:
   ```
   - Klik tombol "Get Link" pada reply bot
   - Copy link yang diberikan
   - Share link tersebut
   ```

3. **User Access**:
   ```
   - User klik link
   - Bot cek subscription
   - Jika belum subscribe → tampil tombol join
   - Jika sudah subscribe → konten dikirim
   ```

## 🔧 Advanced Configuration

### Custom Welcome Message

Edit di database atau melalui admin panel:

```
Selamat datang {mention}!

Bot ini untuk berbagi file/posting.
Kirimkan link untuk akses konten.

👤 Your ID: {id}
```

**Available Variables**:
- `{mention}` - Mention user
- `{first}` - First name
- `{last}` - Last name
- `{id}` - User ID
- `{username}` - Username

### Custom Force Subscribe Message

```
⚠️ Anda harus join channel terlebih dahulu!

Setelah join, klik "Coba Lagi" di bawah.
```

### Subscription Cache TTL

Default: 2 menit

Edit di `.env`:
```bash
FORCESUB_SUBSCRIPTION_TTL_MINUTES=5  # 5 menit
```

Atau di `application.properties`:
```properties
forcesub.subscription.ttl.minutes=5
```

**Rekomendasi**:
- Development: 1-2 menit
- Production: 5-10 menit
- High traffic: 15-30 menit

## 📊 Monitoring

### Docker Logs

```bash
# Real-time logs
docker-compose logs -f forcesub-bot

# Last 100 lines
docker-compose logs --tail=100 forcesub-bot

# Logs with timestamp
docker-compose logs -f -t forcesub-bot
```

### Resource Usage

```bash
# All containers
docker stats

# Specific container
docker stats forcesub-bot
```

### Health Check

```bash
# Service status
docker-compose ps

# Application health endpoint
curl http://localhost:8080/actuator/health
```

### Database Operations

```bash
# MongoDB shell
docker exec -it forcesub-mongodb mongosh forcesub

# Backup database
docker exec forcesub-mongodb mongodump --db forcesub --out /data/backup

# Redis CLI
docker exec -it forcesub-redis redis-cli
```

## 🐛 Troubleshooting

### Bot tidak start

```bash
# Check logs
docker-compose logs forcesub-bot

# Common issues:
# 1. BOT_TOKEN invalid
# 2. MongoDB not ready (tunggu 10-20 detik)
# 3. Port 8080 sudah digunakan
```

**Solution**:
```bash
# Restart services
docker-compose restart

# Clean restart
docker-compose down
docker-compose up -d
```

### MongoDB connection failed

```bash
# Check MongoDB
docker-compose ps mongodb
docker-compose logs mongodb

# Restart MongoDB
docker-compose restart mongodb
```

### Out of Memory

```bash
# Edit .env
MAX_HEAP=1g
MIN_HEAP=512m

# Restart
docker-compose restart forcesub-bot
```

### Clean Start

```bash
# Remove all data
docker-compose down -v
rm -rf td-bot/

# Start fresh
docker-compose up -d
```

## 🔄 Updates

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build

# Or for standalone
docker-compose -f docker-compose.standalone.yml up -d --build
```

## 📚 Documentation

- [Deployment Guide](DEPLOYMENT.md) - Detailed deployment instructions
- [Docker Setup](DOCKER_SETUP.md) - Docker configuration guide
- [API Documentation](docs/API.md) - API endpoints (if available)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🏷 Support

- 📢 Follow Channel [@YourChannel](https://t.me/yourchannel) untuk update bot
- 💬 Gabung Group [@YourGroup](https://t.me/yourgroup) untuk diskusi dan bantuan
- 🐛 Report bug di [Issues](https://github.com/yourusername/forcesub-bot/issues)

## 👨🏻‍💻 Credits

- [TDLight](https://github.com/tdlight-team/tdlight-java) - Optimized TDLib wrapper
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Project Reactor](https://projectreactor.io/) - Reactive programming library

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

**Force-Sub-Bot** is Free Software: You can use, study, share and improve it at your will. Specifically you can redistribute and/or modify it under the terms of the [GNU General Public License](https://www.gnu.org/licenses/gpl.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/forcesub-bot&type=Date)](https://star-history.com/#yourusername/forcesub-bot&Date)

---

**⭐ Berikan bintang pada repo ini jika Anda menyukainya! ⭐**

Made with ❤️ using Java & Spring Boot
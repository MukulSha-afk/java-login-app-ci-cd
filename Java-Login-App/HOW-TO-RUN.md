# How to run Java-Login-App (Windows PowerShell)

You have 2 ways. **Option A (Docker, recommended)** needs nothing except Docker.
**Option B (local Maven)** needs JDK 11 and MySQL.

## What was created for you

| File | Why |
|---|---|
| `schema.sql` | Creates `UserDB.Employee(username UNIQUE, password VARCHAR(255))` for BCrypt hashes |
| `Dockerfile` | Builds `dptweb-1.0.war` on JDK 11 and runs it |
| `docker-compose.yml` | Starts MySQL 8 + app wired together, schema auto-loaded |
| `.env.example` | Copy to `.env` to change DB password |
| `src/main/webapp/pages/logout.jsp` | View for the `/logout` mapping in `login.java` |
| `src/main/java/com/dpt/demo/SecurityConfig.java` | Already in repo: permits `/login`, `/register`, disables default form login so your JSPs work |

Your code is already fixed: `login.java` / `register.java` use `PreparedStatement`
(no SQL injection), BCrypt password hashing, per-request errors + `HttpSession`.
`application.properties` reads `SPRING_DATASOURCE_*` env vars (no hardcoded RDS password).

## Option A - Docker (do this)

```powershell
cd D:\DevOps-Project-01\Java-Login-App
Copy-Item .env.example .env   # optional, defaults work
docker compose up --build
```

Wait for:
`java-login-mysql ... ready for connections` then `Tomcat started on port(s): 8080`.

Open:
- http://localhost:8080/ -> home
- http://localhost:8080/register -> create user (password min 8 chars, stored as BCrypt)
- http://localhost:8080/login -> login with that user -> `Welcome <username>`
- http://localhost:8080/logout

Stop:
```powershell
docker compose down        # keep DB data
docker compose down -v     # also wipe DB and start fresh
docker compose logs -f app # watch app logs
docker compose logs -f db  # watch mysql logs
```

## Option B - Local Maven (no Docker)

1. Install JDK 11 and MySQL 8, then:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11"  # adjust path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version   # must say 11
```

2. Create DB (in MySQL Workbench / CLI):
```sql
SOURCE D:/DevOps-Project-01/Java-Login-App/schema.sql;
```

3. Run:
```powershell
cd D:\DevOps-Project-01\Java-Login-App
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/UserDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "rootpass"   # your mysql root password
mvn clean package -DskipTests
mvn spring-boot:run
# or: java -jar target\dptweb-1.0.war
```

Same URLs as above.

## What YOU have to do (checklist)

- [ ] Pick A or B above (A = just `docker compose up --build`)
- [ ] Open `/register`, make a user, then `/login`
- [ ] Delete `d:\DevOps-Project-01\fixer.py` (my temp helper) before `git add`
- [ ] Never commit real passwords: use `.env` (untracked) not `application.properties`
- [ ] Rotate the old RDS/JFrog passwords found earlier (`Admin123`, `Admin123!`) - they were in git history

## If it fails

- `port 8080 already in use` -> `netstat -ano | findstr 8080` then kill PID, or change compose to `"8081:8080"`
- `Communications link failure` (Option B) -> MySQL not running / wrong password / DB missing. Check `docker ps` (A) or MySQL service (B)
- `Unsupported class file / cglib` -> you ran with Java 21. Switch `JAVA_HOME` to JDK 11 (Docker already uses 11)
- `Registration failed` -> username already exists (UNIQUE) or password < 8 chars
- Old plaintext users still log in once (backward-compat), new users are BCrypt - re-register old users to migrate

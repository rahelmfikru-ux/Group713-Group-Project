# Project & Task Manager

COMP713 Assessment 3, Group 713. Managers assign tasks on a website; employees see and update them in an Android app. Changes appear on the website live (WebSocket), and every task change is sent through RabbitMQ to a separate consumer that keeps an activity log.

- **Repository:** https://github.com/rahelmfikru-ux/Group713-Group-Project
- **Final version:** tag `v1.0-submission`
- **Team:** Denny Gunawan (ozzyvess), Malachi Natana (KingzCall), Rahel Fikru (rahelmfikru-ux)
- **Based on:** our Assessment 2 Project & Task Manager (original README in `docs/`)

## What you need

- Java 21 and Maven
- Docker Desktop
- Android Studio
- Git Bash (Windows only, to run the `.sh` scripts)

## How to run

Run each step in its own terminal, starting from the repository folder.

**1. Start RabbitMQ**
```bash
docker compose up -d
```

**2. Start the API and website**
```bash
cd COMP713-Project-Task-Manager
./run-app.sh                                              # Mac/Linux
powershell -ExecutionPolicy Bypass -File .\run-app.ps1    # Windows
```
Then open http://localhost:8080/project-task-manager/

**3. Start the message consumer**
```bash
cd task-event-consumer
mvn compile exec:java
```

**4. Load demo data**
```bash
cd COMP713-Project-Task-Manager
./scripts/seed-demo-data.sh
```

**5. Run the Android app**
Open the `EmployeeTaskManager` folder in Android Studio and press Run. To give the emulator a location, run `adb emu geo fix 174.7633 -36.8485`.

The database is created automatically. To start fresh, stop everything and delete `COMP713-Project-Task-Manager/data/`.

## Ports and settings

| Port | What |
|---|---|
| 8080 | API, website and WebSocket |
| 5672 | RabbitMQ |
| 15672 | RabbitMQ web UI (login `guest` / `guest`) |

No settings are needed for a local run. Optional environment variables: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` (API and consumer), and `DB_URL` (consumer).

The Android app connects to `10.0.2.2:8080`, which is the emulator's address for your computer. For a real phone, change `BASE_URL` in `ApiClient.java` to your computer's IP address.

## Test accounts

| Name | Role | Use it on |
|---|---|---|
| Morgan Lee | Manager | Website |
| Alex Chen | Employee | Android (choose under "I am") |
| Priya Patel | Employee | Android |

There are no passwords.

## How to use it

1. **Website:** open a project, create a task and assign it to Alex Chen.
2. **Android:** choose Alex Chen. The task appears. Open it and change its status.
3. **Website:** the status updates by itself, and the Activity log shows the event.
4. **Android:** tap **Check in with my location**, or type a location if GPS isn't available. The website shows the check-in.

## Tests

With everything running (on Windows, use Git Bash):
```bash
cd COMP713-Project-Task-Manager
./tests/api-checks.sh           # 17 checks, last result 17/17 passed
./tests/response-time-test.sh   # target p95 under 500 ms, last result 7.5 ms and 10 ms
```
Results are saved in `tests/results/`. The website's Live updates panel also measures WebSocket delivery time (target under 1 second).

## Known limitations

- No login. Anyone can act as any user. This is a classroom prototype only.
- The API and consumer share one database file, so they must run on the same computer.
- If RabbitMQ is down, task changes are still saved but don't appear in the activity log.
- Check-ins are not sent through RabbitMQ.
- Response times were measured on one computer, so they don't include network delay.
- There's no edit or delete for projects, tasks or users.
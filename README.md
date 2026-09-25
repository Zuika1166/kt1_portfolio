# Ktor Task API

Небольшой REST API на Ktor для работы с задачами.

## Запуск

Нужен JDK 17 или новее.

Linux / macOS:

```bash
./gradlew run
```

Windows:

```bat
gradlew.bat run
```

Сервер запускается на `http://localhost:8080`.

## Маршруты

- `GET /` — информация о сервере
- `GET /tasks` — список задач
- `GET /tasks/{id}` — задача по id
- `POST /tasks` — создать задачу
- `DELETE /tasks/{id}` — удалить задачу

Для `GET /tasks` можно использовать параметры:

- `completed=true` или `completed=false`
- `limit=1..100`

Пример POST-запроса:

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Ktor","completed":false}'
```

## Тесты

```bash
./gradlew test
```

## GitHub

```bash
git init
git add .
git commit -m "Ktor task API"
git branch -M main
git remote add origin https://github.com/USERNAME/ktor-task-api.git
git push -u origin main
```

# Ktor 

Кт1 Kotlin 

## Запуск
Нужен JDK 17 

Команда запуска ( linux \ macos )

```bash
./gradlew run
```

Сервер запускается на http://localhost:8080

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

## Проведение тестов

```bash
./gradlew test
```


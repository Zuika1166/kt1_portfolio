package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class Task(
    val id: Long,
    val title: String,
    val completed: Boolean
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val completed: Boolean = false
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class ApiInfo(
    val message: String,
    val endpoints: List<String>
)

class TaskRepository {
    private val nextId = AtomicLong(1)
    private val tasks = ConcurrentHashMap<Long, Task>()

    fun all(completed: Boolean?, limit: Int): List<Task> =
        tasks.values
            .asSequence()
            .sortedBy { it.id }
            .filter { completed == null || it.completed == completed }
            .take(limit)
            .toList()

    fun find(id: Long): Task? = tasks[id]

    fun create(request: CreateTaskRequest): Task {
        val id = nextId.getAndIncrement()
        return Task(id, request.title.trim(), request.completed).also { tasks[id] = it }
    }

    fun delete(id: Long): Boolean = tasks.remove(id) != null
}

fun Application.module(repository: TaskRepository = TaskRepository()) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = false
                ignoreUnknownKeys = false
            }
        )
    }

    install(StatusPages) {
        exception<SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid JSON body"))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
        }
    }

    routing {
        get("/") {
            call.respond(
                ApiInfo(
                    message = "Ktor Task API is running",
                    endpoints = listOf(
                        "GET /tasks?completed=true|false&limit=1..100",
                        "GET /tasks/{id}",
                        "POST /tasks",
                        "DELETE /tasks/{id}"
                    )
                )
            )
        }

        route("/tasks") {
            get {
                val completed = when (val raw = call.request.queryParameters["completed"]) {
                    null -> null
                    "true" -> true
                    "false" -> false
                    else -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Query parameter 'completed' must be 'true' or 'false'")
                        )
                        return@get
                    }
                }

                val limit = when (val raw = call.request.queryParameters["limit"]) {
                    null -> 100
                    else -> raw.toIntOrNull()?.takeIf { it in 1..100 } ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Query parameter 'limit' must be an integer from 1 to 100")
                        )
                        return@get
                    }
                }

                call.respond(repository.all(completed, limit))
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()?.takeIf { it > 0 }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Path parameter 'id' must be a positive integer"))
                    return@get
                }

                val task = repository.find(id)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Task with id=$id was not found"))
                    return@get
                }

                call.respond(task)
            }

            post {
                val request = call.receive<CreateTaskRequest>()
                val title = request.title.trim()

                if (title.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Field 'title' must not be blank"))
                    return@post
                }
                if (title.length > 100) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Field 'title' must contain at most 100 characters"))
                    return@post
                }

                val created = repository.create(request.copy(title = title))
                call.respond(HttpStatusCode.Created, created)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()?.takeIf { it > 0 }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Path parameter 'id' must be a positive integer"))
                    return@delete
                }

                if (!repository.delete(id)) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Task with id=$id was not found"))
                    return@delete
                }

                call.respondText("", status = HttpStatusCode.NoContent)
            }
        }
    }
}

package com.example

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class ApplicationTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test
    fun `post get filter and delete task`() = testApplication {
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        val createdResponse = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Learn Ktor", completed = false))
        }
        assertEquals(HttpStatusCode.Created, createdResponse.status)
        val created = createdResponse.body<Task>()
        assertEquals("Learn Ktor", created.title)

        val byId = client.get("/tasks/${created.id}")
        assertEquals(HttpStatusCode.OK, byId.status)
        assertEquals(created, byId.body<Task>())

        val filtered = client.get("/tasks?completed=false&limit=10")
        assertEquals(HttpStatusCode.OK, filtered.status)
        assertTrue(filtered.body<List<Task>>().any { it.id == created.id })

        val deleted = client.delete("/tasks/${created.id}")
        assertEquals(HttpStatusCode.NoContent, deleted.status)

        val missing = client.get("/tasks/${created.id}")
        assertEquals(HttpStatusCode.NotFound, missing.status)
    }

    @Test
    fun `invalid parameters return bad request`() = testApplication {

        assertEquals(HttpStatusCode.BadRequest, client.get("/tasks?completed=yes").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/tasks?limit=0").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/tasks/abc").status)
    }

    @Test
    fun `blank title returns bad request`() = testApplication {
        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `malformed json returns bad request`() = testApplication {
        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("{not-json}")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}

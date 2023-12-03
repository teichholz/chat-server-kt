# chat-server-kt

## Description
A simple message server I wrote to get some understanding of Websockets and Kotlin + [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html).

Features:
- Uses two Websockets, one to receive messages from the client and anothr to send messages to the client. Makes sure that all messages will eventually reach the client.
- Uses HTTP basic authentication.
- Uses [JOOQ](https://www.jooq.org/) behind the scenens, to implement a simple database access layer.
- Uses [Testcontainers](https://testcontainers.com/) for easy integration tests

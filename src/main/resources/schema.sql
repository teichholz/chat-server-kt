CREATE SCHEMA IF NOT EXISTS CHAT;

CREATE TABLE IF NOT EXISTS CHAT.receiver (
    id SERIAL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS CHAT.message (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    date TIMESTAMP NOT NULL,
    sender INT REFERENCES CHAT.receiver(id),
    receiver INT REFERENCES CHAT.receiver(id)
);

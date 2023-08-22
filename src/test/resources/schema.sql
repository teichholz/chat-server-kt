CREATE SCHEMA IF NOT EXISTS CHAT;

CREATE TABLE IF NOT EXISTS CHAT.receiver (
    id SERIAL PRIMARY KEY,
    name TEXT
);

CREATE TABLE IF NOT EXISTS CHAT.message (
    id SERIAL PRIMARY KEY,
    sender INT REFERENCES CHAT.receiver(id),
    content TEXT,
    receiver INT REFERENCES CHAT.receiver(id),
    sent BOOLEAN DEFAULT FALSE
);

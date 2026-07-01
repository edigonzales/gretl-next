CREATE TABLE IF NOT EXISTS gruesse (
    id INTEGER PRIMARY KEY,
    text VARCHAR(100) NOT NULL
);

INSERT OR IGNORE INTO gruesse (id, text) VALUES (1, 'Hallo GRETL!');
INSERT OR IGNORE INTO gruesse (id, text) VALUES (2, 'Willkommen in der GRETL VS Code Extension.');

CREATE TABLE IF NOT EXISTS rohdaten (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100),
    kategorie VARCHAR(50),
    wert DECIMAL(10, 2)
);

INSERT OR IGNORE INTO rohdaten (id, name, kategorie, wert) VALUES (1, 'Alfa', 'A', 100.00);
INSERT OR IGNORE INTO rohdaten (id, name, kategorie, wert) VALUES (2, 'Bravo', 'B', 200.00);
INSERT OR IGNORE INTO rohdaten (id, name, kategorie, wert) VALUES (3, 'Charlie', 'A', 150.00);
INSERT OR IGNORE INTO rohdaten (id, name, kategorie, wert) VALUES (4, 'Delta', 'B', 300.00);

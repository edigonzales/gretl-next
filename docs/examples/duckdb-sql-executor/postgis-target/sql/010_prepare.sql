CREATE SCHEMA IF NOT EXISTS result;

CREATE TABLE result.points AS
SELECT
    1::INTEGER AS id,
    'first'::VARCHAR AS name,
    ST_GeomFromText('POINT(2600000 1200000)') AS geom
UNION ALL
SELECT
    2::INTEGER AS id,
    'second'::VARCHAR AS name,
    ST_GeomFromText('POINT(2600010 1200010)') AS geom;

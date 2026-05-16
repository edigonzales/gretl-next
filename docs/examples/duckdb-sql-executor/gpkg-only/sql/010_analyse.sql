CREATE SCHEMA IF NOT EXISTS result;

CREATE TABLE result.analyse AS
SELECT
    row_number() OVER () AS id,
    bezeichnung,
    ST_Area(mpoly)::INTEGER AS area_m2,
    mpoly
FROM input.abbaustelle
LIMIT 5;

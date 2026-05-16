CREATE SCHEMA IF NOT EXISTS result;

CREATE TABLE result.analyse AS
SELECT
    row_number() OVER () AS abbaustelle_id,
    g.*,
    ST_Area(ST_Intersection(a.mpoly, g.geometrie)) AS overlap_area_m2
FROM input.abbaustelle a
JOIN pub.gemeinden g
ON ST_Intersects(a.mpoly, g.geometrie);

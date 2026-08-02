LOAD spatial;
CREATE TABLE points AS
SELECT * FROM (VALUES (1, 2600000.0, 1200000.0), (2, 2600001.0, 1200001.0)) AS values(id, x, y);
CREATE TABLE point_geometries AS
SELECT id, ST_Point(x, y) AS geom, ST_X(ST_Point(x, y)) AS point_x, ST_Y(ST_Point(x, y)) AS point_y
FROM points;
CREATE TABLE p2_colors (id integer primary key, name text not null);
INSERT INTO p2_colors VALUES (1, 'red'), (2, 'blue');

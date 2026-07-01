CREATE TABLE IF NOT EXISTS auswertung AS
SELECT kategorie,
       COUNT(*)  AS anzahl,
       SUM(wert) AS summe_wert,
       AVG(wert) AS durchschnitt
FROM rohdaten
GROUP BY kategorie;

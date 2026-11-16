USE gasolineras;
SELECT E.nombre AS Empresa, COUNT(*) AS Total_Estaciones
FROM ESTACION ES
JOIN EMPRESA E ON ES.id_empresa = E.id_empresa
WHERE ES.tipo = 'Maritimo'
GROUP BY E.nombre
ORDER BY Total_Estaciones DESC
LIMIT 1;
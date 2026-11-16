USE gasolineras;
SELECT PR.nombre AS Provincia, ES.localidad, E.nombre AS Empresa, P.precio
FROM ESTACION ES
JOIN MUNICIPIO M ON ES.id_municipio = M.id_municipio
JOIN PROVINCIA PR ON M.id_provincia = PR.id_provincia
JOIN EMPRESA E ON ES.id_empresa = E.id_empresa
JOIN PRECIO P ON ES.id_estacion = P.id_estacion
JOIN CARBURANTE C ON P.id_carburante = C.id_carburante
WHERE ES.tipo = 'Maritimo'
  AND C.nombre = 'Gasolina 95 ES'
ORDER BY P.precio DESC
LIMIT 1;
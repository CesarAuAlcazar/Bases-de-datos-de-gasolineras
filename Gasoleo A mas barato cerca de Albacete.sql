USE gasolineras;
SELECT ES.localidad, ES.direccion, E.nombre AS Empresa, ES.margen, P.precio,
       (6371 * ACOS(
           COS(RADIANS(38.9943)) * COS(RADIANS(ES.latitud)) * COS(RADIANS(ES.longitud) - RADIANS(-1.8585)) + 
           SIN(RADIANS(38.9943)) * SIN(RADIANS(ES.latitud))
       )) AS Distancia_km
FROM ESTACION ES
JOIN EMPRESA E ON ES.id_empresa = E.id_empresa
JOIN PRECIO P ON ES.id_estacion = P.id_estacion
JOIN CARBURANTE C ON P.id_carburante = C.id_carburante
WHERE C.nombre = 'Gasóleo A'
  AND ES.tipo = 'Terrestre'
HAVING Distancia_km <= 10
ORDER BY P.precio ASC
LIMIT 1;
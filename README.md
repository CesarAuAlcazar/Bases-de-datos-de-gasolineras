⛽ Gasolineras España - Diseño de Base de Datos Relacional e Ingesta de datos

Este repositorio contiene la solución para la actividad de diseño de una base de datos relacional y carga de datos mediante Java, desarrollada para la asignatura Bases de Datos Avanzadas.

El objetivo es procesar los datos abiertos del Ministerio para la Transición Ecológica y el Reto Demográfico sobre los precios de carburantes en España, tanto de estaciones de servicio terrestres como de postes marítimos, y realizar una serie de consultas analíticas.
📋 Tabla de Contenidos

    Tecnologías utilizadas

    Estructura del repositorio

    Diseño de la Base de Datos

    Proceso de Ingesta (Java)

    Consultas SQL

    Incidencias resueltas y notas de calidad

    Cómo ejecutar el proyecto

🛠 Tecnologías utilizadas

    Java (JDK 24 / LTS) – Lenguaje para el proceso de ingesta.

    Maven – Gestión de dependencias (MySQL Connector/J).

    MySQL 8 desplegado sobre Docker – Sistema Gestor de Bases de Datos.

    DBeaver – Cliente SQL para gestión y validación de datos.

    IntelliJ IDEA – IDE de desarrollo.

    Diagrams.net (draw.io) – Elaboración del diagrama Entidad/Relación.

<img width="969" height="742" alt="Diagrama ER" src="https://github.com/user-attachments/assets/a13e3b13-def2-43b9-870a-99abfc095081" />

🗄 Diseño de la Base de Datos

El esquema relacional está normalizado para evitar redundancias y facilitar las consultas. Se compone de 6 tablas:

    PROVINCIA: Catálogo de provincias.

    MUNICIPIO: Catálogo de municipios, con FK a PROVINCIA.

    EMPRESA: Catálogo de rótulos/empresas operadoras (REPSOL, CEPSA, BP, etc.).

    CARBURANTE: Catálogo de tipos de combustible (Gasolina 95 E5, Gasóleo A, etc.).

    ESTACION: Entidad central. Contiene localización (dirección, coordenadas), margen, horario y un campo tipo que distingue entre 'Terrestre' y 'Maritimo'.

    PRECIO: Tabla que resuelve la relación muchos-a-muchos entre ESTACION y CARBURANTE. Su clave primaria es compuesta (id_estacion, id_carburante).

    💡 Decisión de diseño: Se optó por una tabla PRECIO en lugar de tener decenas de columnas en ESTACION (como vienen en el CSV original), lo que permite que cada estación solo tenga registros de los carburantes que realmente oferta.

☕ Proceso de Ingesta (Java)

La ingesta se realiza con una única clase IngestaDatos.java que utiliza JDBC para conectarse a MySQL.
Características principales:

    Conexión JDBC mediante DriverManager.getConnection(...).

    Lectura línea a línea de los CSV con BufferedReader y split(";").

    Tratamiento de decimales: se reemplaza la coma decimal (,) por punto (.) antes de insertar en la base de datos.

    Prevención de duplicados en catálogos (PROVINCIA, MUNICIPIO, EMPRESA) mediante el uso de Map<String, Integer> en memoria. Solo se inserta si no existe previamente.

    Gestión de errores por línea: si una fila tiene un formato incorrecto, se captura la excepción y se continúa con la siguiente, garantizando que la ingesta no se detenga.

    Detección dinámica del rótulo: al no tener un índice fijo garantizado en ambos CSV, se implementó un bucle que localiza el primer texto no numérico de la fila para identificar la empresa.

Dependencia Maven (pom.xml)
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
</dependency>

🔍 Consultas SQL

    Empresa con más estaciones de servicio terrestres.

    Empresa con más estaciones de servicio marítimas.

    Estación con el precio más bajo de Gasolina 95 E5 en la Comunidad de Madrid (localización, empresa y margen).

    Estación con el precio más bajo de Gasóleo A a menos de 10 km del centro de Albacete, usando la fórmula de Haversine para calcular la distancia geográfica.

    Provincia de la estación marítima con el precio más caro de Gasolina 95 E5.

🧩 Incidencias resueltas y notas de calidad

Durante el desarrollo se identificaron y resolvieron varios retos técnicos:

    Codificación de caracteres (encoding): Los CSV originales presentaban problemas con tildes y eñes (por ejemplo, CORUąA en lugar de CORUÑA). Se documentó la incidencia y se recomienda establecer la codificación UTF-8 al leer los ficheros en futuras iteraciones.

    Formato decimal regional: Los precios y coordenadas usan coma (,) como separador decimal. Se gestionó con String.replace(",", ".") antes de convertirlos a BigDecimal.

    Nombres de carburantes no coincidentes: El CSV contiene "Gasolina 95 ES" (con S final) en lugar de "Gasolina 95 E5". Las consultas se adaptaron a la nomenclatura real cargada.

    Clasificación de estaciones: El campo tipo se almacenó como 'Terrestre' y 'Maritimo' (masculino) en coherencia con la ingesta, ajustando las consultas a estos valores.

    Índice dinámico del rótulo: Para evitar dependencias frágiles de índices fijos en columnas, se implementó un mecanismo de búsqueda del rótulo por patrón de texto.

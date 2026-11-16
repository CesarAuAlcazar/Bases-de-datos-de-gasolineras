CREATE DATABASE IF NOT EXISTS gasolineras;
USE gasolineras;

CREATE TABLE PROVINCIA (
    id_provincia INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE MUNICIPIO (
    id_municipio INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    id_provincia INT NOT NULL,
    FOREIGN KEY (id_provincia) REFERENCES PROVINCIA(id_provincia)
);

CREATE TABLE EMPRESA (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE CARBURANTE (
    id_carburante INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE ESTACION (
    id_estacion INT AUTO_INCREMENT PRIMARY KEY,
    id_municipio INT NOT NULL,
    id_empresa INT NOT NULL,
    localidad VARCHAR(150),
    codigo_postal VARCHAR(10),
    direccion VARCHAR(250),
    margen VARCHAR(5),
    latitud DECIMAL(10,6),
    longitud DECIMAL(10,6),
    horario VARCHAR(250),
    fecha_actualizacion DATETIME,
    tipo VARCHAR(20),
    FOREIGN KEY (id_municipio) REFERENCES MUNICIPIO(id_municipio),
    FOREIGN KEY (id_empresa) REFERENCES EMPRESA(id_empresa)
);

CREATE TABLE PRECIO (
    id_estacion INT NOT NULL,
    id_carburante INT NOT NULL,
    fecha_actualizacion DATETIME,
    precio DECIMAL(10,4) NOT NULL,
    PRIMARY KEY (id_estacion, id_carburante),
    FOREIGN KEY (id_estacion) REFERENCES ESTACION(id_estacion),
    FOREIGN KEY (id_carburante) REFERENCES CARBURANTE(id_carburante)
);
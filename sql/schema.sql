-- MySQL 8 design reference only. Runtime tables are created by HsqlDatabase
-- using HSQLDB-compatible DDL; this file is not executed by the application.

CREATE TABLE categoria (
    id CHAR(36) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE proveedor (
    id CHAR(36) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(40) NOT NULL,
    correo_electronico VARCHAR(254) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ubicacion (
    id CHAR(36) NOT NULL,
    sede VARCHAR(120) NOT NULL,
    ambiente VARCHAR(120) NOT NULL,
    area VARCHAR(120) NOT NULL,
    piso VARCHAR(40) NOT NULL,
    es_almacen BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    KEY ix_ubicacion_sede (sede)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuario (
    id CHAR(36) NOT NULL,
    nombre_usuario VARCHAR(80) NOT NULL,
    nombre_usuario_normalizado VARBINARY(320)
        GENERATED ALWAYS AS (LOWER(TRIM(nombre_usuario))) STORED,
    hash_contrasena VARBINARY(64) NOT NULL,
    sal_contrasena VARBINARY(32) NOT NULL,
    rol ENUM('ADMINISTRADOR', 'ALMACENERO', 'AUDITOR') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuario_nombre_normalizado (nombre_usuario_normalizado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE equipo (
    id CHAR(36) NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    codigo_normalizado VARBINARY(320) GENERATED ALWAYS AS (LOWER(TRIM(codigo))) STORED,
    numero_serie VARCHAR(40) NOT NULL,
    serie_normalizada VARBINARY(160) GENERATED ALWAYS AS (LOWER(TRIM(numero_serie))) STORED,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(120) NOT NULL,
    categoria_id CHAR(36) NOT NULL,
    proveedor_id CHAR(36) NOT NULL,
    estado ENUM('DISPONIBLE', 'EN_USO', 'EN_MANTENIMIENTO', 'DE_BAJA', 'ANULADO') NOT NULL,
    presente_en_almacen BOOLEAN NOT NULL,
    ubicacion_actual_id CHAR(36) NULL,
    ultima_ubicacion_almacen_id CHAR(36) NULL,
    destino_salida VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_equipo_codigo_normalizado (codigo_normalizado),
    UNIQUE KEY uq_equipo_serie_normalizada (serie_normalizada),
    KEY ix_equipo_categoria (categoria_id),
    CONSTRAINT fk_equipo_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_equipo_proveedor FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_equipo_ubicacion_actual FOREIGN KEY (ubicacion_actual_id)
        REFERENCES ubicacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_equipo_ultima_ubicacion FOREIGN KEY (ultima_ubicacion_almacen_id)
        REFERENCES ubicacion (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE configuracion_stock_minimo (
    id CHAR(36) NOT NULL,
    categoria_id CHAR(36) NOT NULL,
    sede VARCHAR(120) NOT NULL,
    sede_normalizada VARBINARY(480) GENERATED ALWAYS AS (LOWER(TRIM(sede))) STORED,
    minimo INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_minimo_categoria_sede (categoria_id, sede_normalizada),
    CONSTRAINT ck_minimo_no_negativo CHECK (minimo >= 0),
    CONSTRAINT fk_minimo_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE movimiento (
    id CHAR(36) NOT NULL,
    secuencia BIGINT NOT NULL AUTO_INCREMENT,
    fecha_hora DATETIME(6) NOT NULL,
    tipo ENUM('INGRESO', 'SALIDA', 'TRASLADO', 'CAMBIO_UBICACION', 'CAMBIO_ESTADO', 'ANULACION_ALTA') NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    equipo_id CHAR(36) NOT NULL,
    responsable_id CHAR(36) NOT NULL,
    responsable_nombre_snapshot VARCHAR(80) NOT NULL,
    equipo_categoria_id_snapshot CHAR(36) NOT NULL,
    equipo_categoria_nombre_snapshot VARCHAR(100) NOT NULL,
    equipo_codigo_snapshot VARCHAR(80) NOT NULL,
    equipo_serie_snapshot VARCHAR(120) NOT NULL,
    equipo_marca_snapshot VARCHAR(100) NOT NULL,
    equipo_modelo_snapshot VARCHAR(120) NOT NULL,
    equipo_proveedor_nombre_snapshot VARCHAR(150) NOT NULL,
    origen_ubicacion_id_snapshot CHAR(36) NULL,
    origen_sede_snapshot VARCHAR(120) NULL,
    origen_ambiente_snapshot VARCHAR(120) NULL,
    origen_area_snapshot VARCHAR(120) NULL,
    origen_piso_snapshot VARCHAR(40) NULL,
    origen_es_almacen_snapshot BOOLEAN NULL,
    destino_ubicacion_id_snapshot CHAR(36) NULL,
    destino_sede_snapshot VARCHAR(120) NULL,
    destino_ambiente_snapshot VARCHAR(120) NULL,
    destino_area_snapshot VARCHAR(120) NULL,
    destino_piso_snapshot VARCHAR(40) NULL,
    destino_es_almacen_snapshot BOOLEAN NULL,
    destino_externo VARCHAR(255) NULL,
    estado_anterior ENUM('DISPONIBLE', 'EN_USO', 'EN_MANTENIMIENTO', 'DE_BAJA', 'ANULADO') NULL,
    estado_nuevo ENUM('DISPONIBLE', 'EN_USO', 'EN_MANTENIMIENTO', 'DE_BAJA', 'ANULADO') NULL,
    presencia_anterior BOOLEAN NOT NULL,
    presencia_nueva BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_movimiento_secuencia (secuencia),
    KEY ix_movimiento_equipo_fecha (equipo_id, fecha_hora, secuencia),
    KEY ix_movimiento_fecha (fecha_hora, secuencia),
    CONSTRAINT fk_movimiento_equipo FOREIGN KEY (equipo_id)
        REFERENCES equipo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_responsable FOREIGN KEY (responsable_id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE rol (
                     id_rol UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                     nombre_rol VARCHAR(50) NOT NULL UNIQUE,
                     descripcion VARCHAR(255)
);

CREATE TABLE usuario (
                         id_usuario UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         id_rol UUID NOT NULL,
                         nombre VARCHAR(100) NOT NULL,
                         apellido VARCHAR(100) NOT NULL,
                         correo VARCHAR(150) NOT NULL UNIQUE,
                         password_hash TEXT NOT NULL,
                         telefono VARCHAR(20),
                         fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',

                         CONSTRAINT fk_usuario_rol
                             FOREIGN KEY (id_rol)
                                 REFERENCES rol(id_rol),

                         CONSTRAINT chk_usuario_estado
                             CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'))
);

CREATE INDEX idx_usuario_id_rol ON usuario(id_rol);

CREATE TABLE producto (
                          id_producto UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          nombre VARCHAR(150) NOT NULL,
                          marca VARCHAR(100) NOT NULL,
                          descripcion TEXT,
                          precio NUMERIC(10,2) NOT NULL,
                          stock INTEGER NOT NULL,
                          disponibilidad BOOLEAN NOT NULL DEFAULT TRUE,
                          imagen_url TEXT,
                          fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT chk_producto_precio
                              CHECK (precio >= 0),

                          CONSTRAINT chk_producto_stock
                              CHECK (stock >= 0)
);

CREATE INDEX idx_producto_marca ON producto(marca);
CREATE INDEX idx_producto_disponibilidad ON producto(disponibilidad);

CREATE TABLE carrito (
                         id_carrito UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         id_usuario UUID NOT NULL,
                         estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
                         fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_carrito_usuario
                             FOREIGN KEY (id_usuario)
                                 REFERENCES usuario(id_usuario),

                         CONSTRAINT chk_carrito_estado
                             CHECK (estado IN ('ACTIVO', 'COMPRADO', 'ABANDONADO'))
);

CREATE INDEX idx_carrito_id_usuario ON carrito(id_usuario);

CREATE TABLE detalle_carrito (
                                 id_detalle_carrito UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 id_carrito UUID NOT NULL,
                                 id_producto UUID NOT NULL,
                                 cantidad INTEGER NOT NULL,
                                 precio_unitario NUMERIC(10,2) NOT NULL,
                                 subtotal NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,

                                 CONSTRAINT fk_detalle_carrito_carrito
                                     FOREIGN KEY (id_carrito)
                                         REFERENCES carrito(id_carrito)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_detalle_carrito_producto
                                     FOREIGN KEY (id_producto)
                                         REFERENCES producto(id_producto),

                                 CONSTRAINT chk_detalle_carrito_cantidad
                                     CHECK (cantidad > 0),

                                 CONSTRAINT chk_detalle_carrito_precio
                                     CHECK (precio_unitario >= 0),

                                 CONSTRAINT uq_detalle_carrito_producto
                                     UNIQUE (id_carrito, id_producto)
);

CREATE INDEX idx_detalle_carrito_id_carrito ON detalle_carrito(id_carrito);
CREATE INDEX idx_detalle_carrito_id_producto ON detalle_carrito(id_producto);

CREATE TABLE pedido (
                        id_pedido UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        id_usuario UUID NOT NULL,
                        id_carrito UUID NOT NULL,
                        total NUMERIC(10,2) NOT NULL,
                        estado_pedido VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
                        fecha_pedido TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_pedido_usuario
                            FOREIGN KEY (id_usuario)
                                REFERENCES usuario(id_usuario),

                        CONSTRAINT fk_pedido_carrito
                            FOREIGN KEY (id_carrito)
                                REFERENCES carrito(id_carrito),

                        CONSTRAINT chk_pedido_total
                            CHECK (total >= 0),

                        CONSTRAINT chk_pedido_estado
                            CHECK (estado_pedido IN ('PENDIENTE', 'PAGADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO'))
);

CREATE INDEX idx_pedido_id_usuario ON pedido(id_usuario);
CREATE INDEX idx_pedido_id_carrito ON pedido(id_carrito);
CREATE INDEX idx_pedido_estado ON pedido(estado_pedido);

CREATE TABLE detalle_pedido (
                                id_detalle_pedido UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                id_pedido UUID NOT NULL,
                                id_producto UUID NOT NULL,
                                cantidad INTEGER NOT NULL,
                                precio_unitario NUMERIC(10,2) NOT NULL,
                                subtotal NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,

                                CONSTRAINT fk_detalle_pedido_pedido
                                    FOREIGN KEY (id_pedido)
                                        REFERENCES pedido(id_pedido)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_detalle_pedido_producto
                                    FOREIGN KEY (id_producto)
                                        REFERENCES producto(id_producto),

                                CONSTRAINT chk_detalle_pedido_cantidad
                                    CHECK (cantidad > 0),

                                CONSTRAINT chk_detalle_pedido_precio
                                    CHECK (precio_unitario >= 0)
);

CREATE INDEX idx_detalle_pedido_id_pedido ON detalle_pedido(id_pedido);
CREATE INDEX idx_detalle_pedido_id_producto ON detalle_pedido(id_producto);

CREATE TABLE pago (
                      id_pago UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      id_pedido UUID NOT NULL,
                      metodo_pago VARCHAR(50) NOT NULL,
                      estado_pago VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
                      monto NUMERIC(10,2) NOT NULL,
                      referencia_pago VARCHAR(180),
                      fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT fk_pago_pedido
                          FOREIGN KEY (id_pedido)
                              REFERENCES pedido(id_pedido),

                      CONSTRAINT chk_pago_monto
                          CHECK (monto >= 0),

                      CONSTRAINT chk_pago_metodo
                          CHECK (metodo_pago IN ('STRIPE', 'TRANSFERENCIA', 'EFECTIVO_ENTREGA', 'PAGO_TIENDA')),

                      CONSTRAINT chk_pago_estado
                          CHECK (estado_pago IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'))
);

CREATE INDEX idx_pago_id_pedido ON pago(id_pedido);
CREATE INDEX idx_pago_estado ON pago(estado_pago);
INSERT INTO rol (nombre_rol, descripcion)
VALUES
    ('ADMIN', 'Administrador del sistema'),
    ('CLIENTE', 'Usuario comprador')
    ON CONFLICT (nombre_rol) DO NOTHING;

INSERT INTO producto (
    nombre,
    marca,
    descripcion,
    precio,
    stock,
    disponibilidad,
    imagen_url
)
VALUES
    (
        'Laptop Lenovo ThinkPad T14',
        'Lenovo',
        'Laptop empresarial con buen rendimiento para programación y trabajo profesional.',
        18500.00,
        10,
        TRUE,
        'thinkpad-t14.jpg'
    ),
    (
        'Laptop ASUS TUF Gaming A15',
        'ASUS',
        'Laptop gamer con buena potencia para desarrollo, diseño y videojuegos.',
        23500.00,
        7,
        TRUE,
        'asus-tuf-a15.jpg'
    ),
    (
        'MacBook Air M4',
        'Apple',
        'Laptop ligera, silenciosa y fluida para productividad, programación y uso diario.',
        24999.00,
        5,
        TRUE,
        'macbook-air-m4.jpg'
    );
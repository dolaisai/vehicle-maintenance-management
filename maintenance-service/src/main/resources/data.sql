INSERT INTO maintenance_interval_templates (
    version,
    template_name,
    make,
    model,
    oil_change_interval_miles,
    cabin_filter_change_interval_miles,
    spark_plug_change_interval_miles,
    tire_rotation_interval_months
)
VALUES
    (0,'Subaru WRX Factory','subaru','wrx',3000,15000,60000,6),
    (0,'Datsun 280zx Factory','datsun','280zx',5000,15000,60000,6),
    (0, 'Toyota Camry Factory', 'toyota', 'camry', 5000, 15000, 120000, 6),
    (0, 'Honda Civic Factory', 'honda', 'civic', 5000, 15000, 100000, 6),
    (0, 'Ford F-150 Factory', 'ford', 'f-150', 7500, 20000, 100000, 6),
    (0, 'Chevrolet Silverado Factory', 'chevrolet', 'silverado', 7500, 20000, 100000, 6),
    (0, 'BMW 3 Series Factory', 'bmw', '3 series', 10000, 15000, 60000, 6),
    (0, 'Audi A4 Factory', 'audi', 'a4', 10000, 15000, 60000, 6),
    (0, 'Mercedes C-Class Factory', 'mercedes-benz', 'c-class', 10000, 20000, 60000, 6),
    (0, 'Jeep Wrangler Factory', 'jeep', 'wrangler', 5000, 15000, 30000, 6),
    (0, 'Mazda CX-5 Factory', 'mazda', 'cx-5', 5000, 15000, 100000, 6);
alter table charge
add column foreign_power_related bool NULL,
add column domestic_violence_related bool null;

alter table charge_history
add column foreign_power_related bool NULL,
add column domestic_violence_related bool null;
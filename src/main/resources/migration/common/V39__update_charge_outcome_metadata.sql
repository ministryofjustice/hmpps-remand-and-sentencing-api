ALTER TABLE charge_outcome
ADD COLUMN disposition_code VARCHAR NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN charge_status VARCHAR NOT NULL DEFAULT 'ACTIVE';

update charge_outcome set disposition_code = 'FINAL', charge_status = 'ACTIVE' where nomis_code = '1002';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '1015';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '1019';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '1057';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'ACTIVE' where nomis_code = '1081';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'ACTIVE' where nomis_code = '1115';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'ACTIVE' where nomis_code = '1116';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'ACTIVE' where nomis_code = '1510';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2004';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2006';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'INACTIVE' where nomis_code = '2008';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2050';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2051';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2053';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2060';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2061';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'INACTIVE' where nomis_code = '2063';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4001';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4004';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4012';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4016';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4506';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4530';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4531';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4560';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4565';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '4588';
update charge_outcome set disposition_code = 'INTERIM', charge_status = 'ACTIVE' where nomis_code = '5500';
update charge_outcome set disposition_code = 'FINAL', charge_status = 'ACTIVE' where nomis_code = 'G';






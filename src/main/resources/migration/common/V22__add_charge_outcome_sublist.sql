ALTER TABLE charge_outcome ADD COLUMN is_sub_list bool not null default false;

update charge_outcome set is_sub_list = true where outcome_uuid in (
'76ec0de0-da17-44a8-9184-790770d16d46',
'86776327-7e1f-4830-bd4e-69168b3b0197',
'f9042ecc-6bed-4872-bbed-1b0938bcecd7',
'e4c69c8a-9320-4126-9101-5674191ff37e',
'0e9437fa-d9bc-4588-8105-dc932163c624',
'6ff42a7a-6e88-42ef-8671-5438bd221324',
'11acbed7-58d9-4efb-9609-ff4a52fc61e1',
'6d2eb21d-ec02-48fa-9fcd-02e73b8e45ca',
'8fc275c5-dbfc-45b7-b18c-efb9e2bb22f1',
'16a76712-0312-4069-a595-ce2d0698bcd5');
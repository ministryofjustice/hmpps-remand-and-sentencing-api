ALTER TABLE appearance_outcome ADD COLUMN is_sub_list bool not null default false;

update appearance_outcome set is_sub_list = true where outcome_uuid in (
'788fa741-4597-477a-9eec-2301456104d0',
'138b7f83-93d0-4e5a-bc4a-5caf06558820',
'05591ff8-36ee-4f14-af08-a3c940b51fa9',
'1709089f-0cf8-4544-8229-a2251e671241',
'876e9469-d8e3-412c-8f5a-6e8ae190bd2f',
'1f9cdf32-8e28-498e-8e6a-065df21657dd',
'6f929f8a-6a35-47b8-99f7-584ca61d1690',
'379f3c62-0c85-4492-8c29-765671c7ae87',
'80dc6ca8-9be5-44f3-add6-2b725041ca80');
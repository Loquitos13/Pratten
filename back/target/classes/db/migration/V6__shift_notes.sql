-- Notas opcionais no clock in/out (visíveis ao empregado e à gestão).
ALTER TABLE work_shifts ADD COLUMN clock_in_notes VARCHAR(500);
ALTER TABLE work_shifts ADD COLUMN clock_out_notes VARCHAR(500);

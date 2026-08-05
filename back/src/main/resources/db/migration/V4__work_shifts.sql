-- Turnos de trabalho (clock in/out) e remoção do perfil CASHIER.
CREATE TABLE work_shifts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    staff_member_id UUID NOT NULL REFERENCES staff_members (id),
    clock_in_at TIMESTAMP WITH TIME ZONE NOT NULL,
    clock_out_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_work_shifts_staff_active
    ON work_shifts (tenant_id, staff_member_id)
    WHERE clock_out_at IS NULL;

UPDATE staff_members SET role = 'WAITER' WHERE role = 'CASHIER';

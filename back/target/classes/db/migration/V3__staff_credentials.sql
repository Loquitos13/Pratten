-- Login credentials for staff members.

ALTER TABLE staff_members
    ADD COLUMN password_hash VARCHAR(100);

-- Email must be unique within a tenant when present (login identifier).
CREATE UNIQUE INDEX uq_staff_email_per_tenant
    ON staff_members (tenant_id, LOWER(email))
    WHERE email IS NOT NULL;

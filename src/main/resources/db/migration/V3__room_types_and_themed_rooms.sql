-- V3: introduce room types (PRIVATE / THEMED) and seed the fixed large-scale themed places
-- (LEDGER-008). THEMED rooms have no human host and hold anonymous crowds (capacity up to 2000),
-- so host_user_id becomes nullable.

ALTER TABLE rooms ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';
ALTER TABLE rooms ALTER COLUMN type DROP DEFAULT;
ALTER TABLE rooms ALTER COLUMN host_user_id DROP NOT NULL;

INSERT INTO rooms
    (created_at, updated_at, name, host_user_id, capacity, type, status, invite_code,
     current_member_count, version)
VALUES
    (now(), now(), '분위기 좋은 카페',  NULL, 2000, 'THEMED', 'ACTIVE', 'cafe-vibe',  0, 0),
    (now(), now(), '제주 바다속 카페',  NULL, 2000, 'THEMED', 'ACTIVE', 'jeju-sea',   0, 0),
    (now(), now(), '심야 도서관',      NULL, 2000, 'THEMED', 'ACTIVE', 'night-lib',  0, 0),
    (now(), now(), '루프탑 스터디',     NULL, 2000, 'THEMED', 'ACTIVE', 'rooftop',    0, 0),
    (now(), now(), '숲속 오두막',      NULL, 2000, 'THEMED', 'ACTIVE', 'forest-hut', 0, 0);

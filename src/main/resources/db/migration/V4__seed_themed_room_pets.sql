-- V4: give each seeded THEMED room a pet so the atomic growth update always has a row to hit
-- (private rooms get their pet created when the room is created). LEDGER-008.
INSERT INTO room_pets (created_at, updated_at, room_id, growth_points, level)
SELECT now(), now(), id, 0, 1
FROM rooms
WHERE type = 'THEMED';

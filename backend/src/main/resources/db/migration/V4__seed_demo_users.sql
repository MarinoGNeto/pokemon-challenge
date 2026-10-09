-- ADR-008 / ADR-011: demo credentials so the reviewer can sign in on first start. DEMO-ONLY, documented in README:
--   admin / Admin#2026  (ADMIN: may also delete local Pokemon)
--   user  / User#2026   (USER: may sync and edit)
-- Hashes generated with the application's own BCryptPasswordEncoder (cost 10); raw passwords are never stored.

insert into users (username, email, password_hash, role, created_at)
values ('admin', 'admin@pokemon.example', '$2a$10$NDDEeEyfC/FwciqtzU4aBOKptWzkTAtGVb6bttLGNdCIc78X3jqgi', 'ADMIN',
        timestamptz '2026-10-09 16:30:00+00'),
       ('user',  'user@pokemon.example',  '$2a$10$jf68YfW68AATnA8sUwd3L.YgP8wt7EVwECNpuiSJ35cVA/3AB/qza', 'USER',
        timestamptz '2026-10-09 16:30:00+00');

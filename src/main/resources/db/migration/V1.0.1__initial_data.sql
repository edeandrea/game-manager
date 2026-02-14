INSERT INTO events (id, name, description, event_date)
VALUES (nextval('events_seq'), 'DevNexus 2026', 'Annual event held by the Atlanta Java Users Group', '2026-03-04 00:00:00+00');

INSERT INTO games (id, time_to_complete, event_id, game_date, email, first_name, last_name)
VALUES (nextval('games_seq'), 30000000000, currval('events_seq'), '2026-03-05 00:00:00+00', 'eric.deandrea@ibm.com', 'Eric', 'Deandrea');

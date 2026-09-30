-- Near-me search: an instructor's rounded location is indexed only after they opt in.
ALTER TABLE instructors
    ADD COLUMN location_search_opt_in BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN instructors.location_search_opt_in IS
    'Owner opt-in to appear in near-me search; only a verified, opted-in instructor with coordinates gets a (2 dp rounded) _geo point in the instructors index';

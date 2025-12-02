-- Adds a visibility flag to catalog items so only public mappings are exposed to storefronts.

ALTER TABLE commerce_catalog_item
    ADD COLUMN publicly_visible BOOLEAN NOT NULL DEFAULT TRUE;

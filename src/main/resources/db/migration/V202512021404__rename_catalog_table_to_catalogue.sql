-- Renames commerce_catalog_item to commerce_catalogue_item to align spelling.

ALTER TABLE commerce_catalog_item RENAME TO commerce_catalogue_item;

-- Optional: indexes/constraints keep existing names for compatibility.

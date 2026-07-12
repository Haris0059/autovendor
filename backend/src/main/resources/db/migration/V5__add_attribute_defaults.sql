-- Per-mapping default values for the OLX category's attributes, keyed by the
-- OLX attribute name (e.g. {"vrsta": "Za keramiku"}). Null on pre-step-9 rows.
ALTER TABLE category_mappings
    ADD COLUMN attribute_defaults JSONB;

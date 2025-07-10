CREATE TABLE IF NOT EXISTS cauldrons
(
    id TEXT PRIMARY KEY,
    world TEXT NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    ingredients TEXT NOT NULL,
    completed INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS player_settings
(
    id TEXT PRIMARY KEY,
    modifier_count INTEGER NOT NULL,
    effect_count INTEGER NOT NULL
);
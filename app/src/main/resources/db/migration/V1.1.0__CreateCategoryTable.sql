CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL,
    user_id UUID NULL,
    parent_id INTEGER NULL REFERENCES category(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE', 'EXCLUDED'))
);

CREATE INDEX idx_category_uuid ON category(uuid);
CREATE INDEX idx_category_parent_id ON category(parent_id);
CREATE INDEX idx_category_user_id ON category(user_id);

-- System Categories: user_id NULL means global / not user-scoped
-- parent_id NULL means top-level Category; otherwise Subcategory

INSERT INTO category (name, type, user_id, parent_id) VALUES
    ('HOUSING',               'EXPENSE',  NULL, NULL),
    ('FOOD & DINING',         'EXPENSE',  NULL, NULL),
    ('TRANSPORT',             'EXPENSE',  NULL, NULL),
    ('HEALTH',                'EXPENSE',  NULL, NULL),
    ('SUBSCRIPTIONS',         'EXPENSE',  NULL, NULL),
    ('TRAVEL',                'EXPENSE',  NULL, NULL),
    ('ENTERTAINMENT',         'EXPENSE',  NULL, NULL),
    ('MISCELLANEOUS',         'EXPENSE',  NULL, NULL),
    ('INCOME',                'INCOME',   NULL, NULL),
    ('SAVINGS & INVESTMENTS', 'INCOME',   NULL, NULL),
    ('TRANSFERS',             'EXCLUDED', NULL, NULL);

-- HOUSING subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('RENT'), ('UTILITIES'), ('WATER'), ('ELECTRICITY'), ('INTERNET'), ('APPLIANCES')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'HOUSING' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- FOOD & DINING subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('RESTAURANTS'), ('GROCERIES'), ('COFFEE & SNACKS')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'FOOD & DINING' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- TRANSPORT subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('CAR'), ('GASOLINE'), ('CAR TOOL'), ('PUBLIC TRANSPORT')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'TRANSPORT' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- HEALTH subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('HEALTH & WELLNESS'), ('GYM'), ('PSYCHOLOGY')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'HEALTH' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- SUBSCRIPTIONS subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('NETFLIX'), ('PRIME'), ('APPLE'), ('NESPRESSO'), ('MOBILE')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'SUBSCRIPTIONS' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- TRAVEL subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('FLIGHTS'), ('ACCOMMODATION')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'TRAVEL' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- ENTERTAINMENT subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('FUN MONEY'), ('HOBBIES')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'ENTERTAINMENT' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- MISCELLANEOUS subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXPENSE', NULL, c.id
FROM (VALUES ('OTHERS')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'MISCELLANEOUS' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- INCOME subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'INCOME', NULL, c.id
FROM (VALUES ('SALARY'), ('FREELANCE'), ('OTHER INCOME')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'INCOME' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- SAVINGS & INVESTMENTS subcategories
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'INCOME', NULL, c.id
FROM (VALUES ('SAVINGS'), ('INVESTMENTS')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'SAVINGS & INVESTMENTS' AND parent_id IS NULL AND user_id IS NULL) AS c;

-- TRANSFERS subcategories (EXCLUDED - internal transfers, no budgeting impact)
INSERT INTO category (name, type, user_id, parent_id)
SELECT sub.name, 'EXCLUDED', NULL, c.id
FROM (VALUES ('TRANSFER BETWEEN ACCOUNTS'), ('FINANCIAL EXPENSES')) AS sub(name)
CROSS JOIN (SELECT id FROM category WHERE name = 'TRANSFERS' AND parent_id IS NULL AND user_id IS NULL) AS c;
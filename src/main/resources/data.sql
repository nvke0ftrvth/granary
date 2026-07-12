-- Users
INSERT INTO users (id, username, email, password) VALUES
(1, 'testuser', 'test@example.com', 'password123'),
(2, 'janedoe',  'jane@example.com', 'password456');

-- Recipes
INSERT INTO recipes (id, title, description, user_id, updated_at) VALUES
(1, 'Spaghetti Carbonara',  'A classic Italian pasta dish',        1, CURRENT_TIMESTAMP),
(2, 'Chicken Stir Fry',     'Quick and easy weeknight dinner',     1, CURRENT_TIMESTAMP),
(3, 'Blueberry Pancakes',   'Fluffy pancakes with fresh berries',  2, CURRENT_TIMESTAMP),
(4, 'Caesar Salad',         'Crisp romaine with homemade dressing',2, CURRENT_TIMESTAMP);

-- Recipe Images
INSERT INTO recipe_image (id, recipe_id, image_url, filename, display_order) VALUES
(1, 1, '/images/carbonara.jpg',  'carbonara.jpg',  0),
(2, 2, '/images/stirfry.jpg',    'stirfry.jpg',    0),
(3, 3, '/images/pancakes.jpg',   'pancakes.jpg',   0),
(4, 3, '/images/pancakes2.jpg',  'pancakes2.jpg',  1);

-- Ingredients
INSERT INTO recipe_ingredients (recipe_id, name, quantity, measurement) VALUES
(1, '200g spaghetti', '200', 'grams'),
(1, '100g pancetta', '100', 'grams'),
(1, '2 large eggs', '2', NULL),
(1, '50g parmesan', '50', 'grams'),
(2, '300g chicken breast', '300', 'grams'),
(2, '2 tbsp soy sauce', '2', 'tbsp'),
(2, '1 bell pepper', '1', NULL);

-- Steps
INSERT INTO recipe_steps (recipe_id, instruction, step_order) VALUES
(1, 'Boil pasta in salted water',           0),
(1, 'Fry pancetta until crispy',            1),
(1, 'Mix eggs and parmesan in a bowl',      2),
(1, 'Combine off the heat',                 3),
(2, 'Slice chicken into strips',            0),
(2, 'Heat oil in a wok',                    1),
(2, 'Stir fry chicken for 5 minutes',       2),
(2, 'Add vegetables and soy sauce',         3);

-- Tags
INSERT INTO recipe_tags (recipe_id, tag) VALUES
(1, 'italian'),
(1, 'pasta'),
(2, 'asian'),
(2, 'quick');
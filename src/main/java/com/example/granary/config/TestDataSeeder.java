package com.example.granary.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.granary.model.Bookmark;
import com.example.granary.model.Comment;
import com.example.granary.model.CommentVote;
import com.example.granary.model.Ingredient;
import com.example.granary.model.Recipe;
import com.example.granary.model.Step;
import com.example.granary.model.User;
import com.example.granary.repo.BookmarkRepository;
import com.example.granary.repo.CommentRepository;
import com.example.granary.repo.CommentVoteRepository;
import com.example.granary.repo.RecipeRepository;
import com.example.granary.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Opt-in test-data fixture: 10 users, 15 recipes, comments with replies and
 * votes, and bookmarks. Only runs under the "seed-test-data" profile, e.g.
 * {@code SPRING_PROFILES_ACTIVE=dev,seed-test-data}. All seeded users share
 * the password "TestPass123!".
 */
@Slf4j
@Component
@Profile("seed-test-data")
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private static final String SEED_PASSWORD = "TestPass123!";
    private static final String MARKER_USERNAME = "chef_amara";

    private static final String[] COMMENT_TEMPLATES = {
        "Made this last night and it turned out amazing!",
        "This is now in my regular rotation, thank you!",
        "Great balance of flavors, will definitely make again.",
        "Simple instructions and the results were fantastic.",
        "My family loved this, even the picky eaters!",
        "Turned out perfectly on the first try.",
        "Love how easy this was to follow.",
        "This recipe exceeded my expectations!"
    };

    private static final String[] REPLY_TEMPLATES = {
        "Totally agree, this one's a keeper!",
        "Glad it worked out for you!",
        "I had the same experience, so good.",
        "Nice, I'll have to try that tip next time.",
        "Thanks for confirming, making this again soon.",
        "Same here, this recipe never disappoints."
    };

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PasswordEncoder passwordEncoder;

    private int commentCounter = 0;

    private record RecipeEntry(Recipe recipe, int ownerIndex) {
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(MARKER_USERNAME).isPresent()) {
            log.info("Test data seed: {} already exists, skipping", MARKER_USERNAME);
            return;
        }

        List<User> users = seedUsers();
        List<RecipeEntry> recipeEntries = seedRecipes(users);
        seedCommentsAndVotes(users, recipeEntries);
        seedBookmarks(users, recipeEntries);

        log.info("Test data seed: created {} users, {} recipes", users.size(), recipeEntries.size());
    }

    private List<User> seedUsers() {
        List<User> users = new ArrayList<>();
        users.add(user("chef_amara", "amara.chef@example.com"));
        users.add(user("baker_leo", "leo.bakes@example.com"));
        users.add(user("spice_nina", "nina.spice@example.com"));
        users.add(user("grillmaster_theo", "theo.grill@example.com"));
        users.add(user("veggie_priya", "priya.veggie@example.com"));
        users.add(user("pasta_marco", "marco.pasta@example.com"));
        users.add(user("dessert_yuki", "yuki.dessert@example.com"));
        users.add(user("brothy_sam", "sam.broth@example.com"));
        users.add(user("taco_elena", "elena.tacos@example.com"));
        users.add(user("curry_raj", "raj.curry@example.com"));
        return users;
    }

    private List<RecipeEntry> seedRecipes(List<User> u) {
        List<RecipeEntry> entries = new ArrayList<>();

        entries.add(entry(u, 0, "Lemon Herb Roast Chicken",
            "A juicy whole roast chicken brightened with lemon, garlic, and fresh herbs.",
            "1 hour 30 minutes", List.of("dinner", "chicken", "roast"),
            List.of(
                ing("Whole chicken", "lb", "4"),
                ing("Lemon", "whole", "2"),
                ing("Garlic cloves", "cloves", "6"),
                ing("Fresh rosemary", "sprigs", "3"),
                ing("Olive oil", "tbsp", "3")),
            List.of(
                step(1, "Preheat oven to 425F and pat the chicken dry."),
                step(2, "Stuff the cavity with lemon halves, garlic, and rosemary."),
                step(3, "Rub the skin with olive oil, salt, and pepper."),
                step(4, "Roast for 1 hour 15 minutes until the juices run clear."),
                step(5, "Rest for 10 minutes before carving."))));

        entries.add(entry(u, 0, "Garlic Butter Mashed Potatoes",
            "Creamy mashed potatoes loaded with roasted garlic and butter.",
            "40 minutes", List.of("side", "potatoes", "comfort food"),
            List.of(
                ing("Russet potatoes", "lb", "3"),
                ing("Butter", "tbsp", "6"),
                ing("Garlic cloves", "cloves", "4"),
                ing("Whole milk", "cup", "1")),
            List.of(
                step(1, "Peel and quarter the potatoes, then boil until fork-tender."),
                step(2, "Roast the garlic cloves until soft and golden."),
                step(3, "Drain potatoes and mash with butter and roasted garlic."),
                step(4, "Stir in warm milk until smooth, then season to taste."))));

        entries.add(entry(u, 1, "Classic Sourdough Loaf",
            "A crusty artisan sourdough with an open, airy crumb.",
            "24 hours (mostly proofing)", List.of("bread", "baking", "sourdough"),
            List.of(
                ing("Bread flour", "g", "500"),
                ing("Active sourdough starter", "g", "100"),
                ing("Water", "g", "350"),
                ing("Salt", "g", "10")),
            List.of(
                step(1, "Mix flour, starter, and water, then rest for 30 minutes."),
                step(2, "Add salt and perform a series of stretch and folds."),
                step(3, "Bulk ferment at room temperature for 6-8 hours."),
                step(4, "Shape the loaf and cold proof overnight in the fridge."),
                step(5, "Bake in a preheated Dutch oven at 475F for 40 minutes."))));

        entries.add(entry(u, 1, "Cinnamon Sugar Pull-Apart Bread",
            "Buttery, gooey pull-apart bread rolled in cinnamon sugar.",
            "2 hours", List.of("bread", "dessert", "baking"),
            List.of(
                ing("All-purpose flour", "g", "450"),
                ing("Sugar", "g", "100"),
                ing("Cinnamon", "tbsp", "2"),
                ing("Butter", "g", "115"),
                ing("Yeast", "tsp", "2")),
            List.of(
                step(1, "Make a soft enriched dough and let it rise for 1 hour."),
                step(2, "Roll out the dough and brush with melted butter."),
                step(3, "Sprinkle generously with cinnamon sugar."),
                step(4, "Cut into strips, stack, and place in a loaf pan."),
                step(5, "Bake at 350F for 35-40 minutes until golden."))));

        entries.add(entry(u, 2, "Fiery Thai Basil Stir-Fry",
            "A fast, spicy stir-fry with ground pork, chilies, and Thai basil.",
            "20 minutes", List.of("dinner", "spicy", "thai", "stir-fry"),
            List.of(
                ing("Ground pork", "lb", "1"),
                ing("Thai chilies", "whole", "4"),
                ing("Garlic cloves", "cloves", "5"),
                ing("Thai basil leaves", "cup", "1"),
                ing("Fish sauce", "tbsp", "2")),
            List.of(
                step(1, "Pound garlic and chilies into a rough paste."),
                step(2, "Stir-fry the paste in hot oil until fragrant."),
                step(3, "Add pork and cook until browned."),
                step(4, "Season with fish sauce and a touch of sugar."),
                step(5, "Toss in basil leaves off heat until wilted."))));

        entries.add(entry(u, 3, "Smoky BBQ Pulled Pork",
            "Low-and-slow smoked pork shoulder shredded and tossed in BBQ sauce.",
            "8 hours", List.of("bbq", "pork", "smoker"),
            List.of(
                ing("Pork shoulder", "lb", "5"),
                ing("BBQ dry rub", "cup", "0.5"),
                ing("BBQ sauce", "cup", "1.5"),
                ing("Apple cider vinegar", "cup", "0.25")),
            List.of(
                step(1, "Coat the pork shoulder generously with dry rub."),
                step(2, "Smoke at 225F until internal temp reaches 195F."),
                step(3, "Rest, then shred the pork with two forks."),
                step(4, "Toss with vinegar and BBQ sauce before serving."))));

        entries.add(entry(u, 3, "Charred Corn Elote",
            "Grilled Mexican street corn with cotija cheese and chili powder.",
            "25 minutes", List.of("side", "grilling", "mexican"),
            List.of(
                ing("Corn on the cob", "ears", "6"),
                ing("Mayonnaise", "cup", "0.5"),
                ing("Cotija cheese", "cup", "0.5"),
                ing("Chili powder", "tsp", "1"),
                ing("Lime", "whole", "2")),
            List.of(
                step(1, "Grill corn over high heat until charred in spots."),
                step(2, "Brush with mayonnaise while still warm."),
                step(3, "Roll in cotija cheese and dust with chili powder."),
                step(4, "Finish with a squeeze of fresh lime."))));

        entries.add(entry(u, 4, "Chickpea Coconut Curry",
            "A cozy, plant-based curry with chickpeas simmered in coconut milk.",
            "35 minutes", List.of("vegan", "curry", "dinner"),
            List.of(
                ing("Chickpeas", "cans", "2"),
                ing("Coconut milk", "can", "1"),
                ing("Curry powder", "tbsp", "2"),
                ing("Onion", "whole", "1"),
                ing("Diced tomatoes", "can", "1")),
            List.of(
                step(1, "Saute onion until soft, then stir in curry powder."),
                step(2, "Add tomatoes and simmer for 5 minutes."),
                step(3, "Stir in chickpeas and coconut milk."),
                step(4, "Simmer for 20 minutes until thickened."),
                step(5, "Serve over rice with fresh cilantro."))));

        entries.add(entry(u, 5, "Creamy Mushroom Risotto",
            "Rich, creamy Arborio rice risotto with sauteed wild mushrooms.",
            "45 minutes", List.of("dinner", "italian", "vegetarian"),
            List.of(
                ing("Arborio rice", "cup", "1.5"),
                ing("Mixed mushrooms", "lb", "1"),
                ing("Vegetable stock", "cup", "5"),
                ing("Parmesan cheese", "cup", "0.75"),
                ing("White wine", "cup", "0.5")),
            List.of(
                step(1, "Saute mushrooms until golden, then set aside."),
                step(2, "Toast the rice in butter until translucent."),
                step(3, "Deglaze with white wine and let it absorb."),
                step(4, "Add warm stock one ladle at a time, stirring constantly."),
                step(5, "Fold in mushrooms and parmesan just before serving."))));

        entries.add(entry(u, 5, "Classic Spaghetti Carbonara",
            "Silky carbonara made with eggs, pecorino, and crispy guanciale.",
            "25 minutes", List.of("dinner", "italian", "pasta"),
            List.of(
                ing("Spaghetti", "lb", "1"),
                ing("Guanciale", "oz", "6"),
                ing("Eggs", "whole", "3"),
                ing("Pecorino Romano", "cup", "1"),
                ing("Black pepper", "tsp", "1")),
            List.of(
                step(1, "Cook spaghetti in salted water until al dente."),
                step(2, "Render guanciale in a pan until crisp."),
                step(3, "Whisk eggs with pecorino and black pepper."),
                step(4, "Toss hot pasta with guanciale off heat."),
                step(5, "Stir in the egg mixture until glossy and creamy."))));

        entries.add(entry(u, 6, "Matcha Cheesecake",
            "A silky baked cheesecake with earthy matcha swirled throughout.",
            "5 hours (with chilling)", List.of("dessert", "baking", "matcha"),
            List.of(
                ing("Cream cheese", "oz", "24"),
                ing("Sugar", "cup", "1"),
                ing("Eggs", "whole", "3"),
                ing("Matcha powder", "tbsp", "2"),
                ing("Graham crackers", "cup", "1.5")),
            List.of(
                step(1, "Press graham cracker crust into a springform pan."),
                step(2, "Beat cream cheese and sugar until smooth."),
                step(3, "Add eggs one at a time, then fold in matcha."),
                step(4, "Bake at 325F in a water bath for 55 minutes."),
                step(5, "Chill for at least 4 hours before slicing."))));

        entries.add(entry(u, 7, "Slow-Simmered Beef Ramen",
            "A deeply savory beef broth ramen topped with soft-boiled eggs.",
            "4 hours", List.of("soup", "ramen", "japanese"),
            List.of(
                ing("Beef short ribs", "lb", "2"),
                ing("Ramen noodles", "servings", "4"),
                ing("Soy sauce", "cup", "0.5"),
                ing("Green onions", "bunch", "1"),
                ing("Soft-boiled eggs", "whole", "4")),
            List.of(
                step(1, "Sear short ribs, then simmer in water for 3 hours."),
                step(2, "Season the broth with soy sauce and aromatics."),
                step(3, "Cook ramen noodles separately until just tender."),
                step(4, "Assemble bowls with broth, noodles, beef, and egg."))));

        entries.add(entry(u, 7, "Ginger Chicken Noodle Soup",
            "A soothing chicken noodle soup with plenty of fresh ginger.",
            "50 minutes", List.of("soup", "comfort food", "chicken"),
            List.of(
                ing("Chicken thighs", "lb", "1.5"),
                ing("Egg noodles", "oz", "8"),
                ing("Fresh ginger", "in", "3"),
                ing("Carrots", "whole", "2"),
                ing("Chicken stock", "cup", "8")),
            List.of(
                step(1, "Simmer chicken thighs in stock with sliced ginger."),
                step(2, "Remove chicken, shred, and set aside."),
                step(3, "Add carrots and noodles to the simmering broth."),
                step(4, "Return shredded chicken to the pot before serving."))));

        entries.add(entry(u, 8, "Street-Style Fish Tacos",
            "Crispy battered fish tacos with cabbage slaw and chipotle crema.",
            "40 minutes", List.of("dinner", "mexican", "seafood"),
            List.of(
                ing("White fish fillets", "lb", "1.5"),
                ing("Corn tortillas", "whole", "8"),
                ing("Cabbage", "cup", "2"),
                ing("Sour cream", "cup", "0.5"),
                ing("Chipotle in adobo", "tbsp", "1")),
            List.of(
                step(1, "Batter and fry the fish until golden and crisp."),
                step(2, "Whisk sour cream with minced chipotle for the crema."),
                step(3, "Warm the tortillas on a hot skillet."),
                step(4, "Assemble tacos with fish, slaw, and crema."))));

        entries.add(entry(u, 9, "Butter Chicken",
            "Tender chicken simmered in a rich, spiced tomato-butter sauce.",
            "1 hour", List.of("curry", "indian", "dinner"),
            List.of(
                ing("Chicken thighs", "lb", "2"),
                ing("Butter", "tbsp", "4"),
                ing("Tomato puree", "cup", "1.5"),
                ing("Heavy cream", "cup", "0.5"),
                ing("Garam masala", "tbsp", "1")),
            List.of(
                step(1, "Marinate chicken in yogurt and spices for 30 minutes."),
                step(2, "Sear the chicken and set aside."),
                step(3, "Simmer tomato puree with butter and garam masala."),
                step(4, "Stir in cream, then return chicken to the sauce."),
                step(5, "Simmer until the chicken is cooked through."))));

        return entries;
    }

    private void seedCommentsAndVotes(List<User> u, List<RecipeEntry> entries) {
        User engagedA = u.get(2);
        User engagedB = u.get(6);

        for (int i = 0; i < entries.size(); i++) {
            RecipeEntry entry = entries.get(i);
            User commenter1 = u.get((entry.ownerIndex() + 1) % u.size());
            User commenter2 = u.get((entry.ownerIndex() + 4) % u.size());

            for (User commenter : List.of(commenter1, commenter2)) {
                Comment topLevel = comment(commenter, entry.recipe(), nextText(COMMENT_TEMPLATES), null);

                for (User engaged : List.of(engagedA, engagedB)) {
                    if (engaged.getUsername().equals(commenter.getUsername())) {
                        continue;
                    }
                    int voteValue = (commentCounter % 5 == 0) ? -1 : 1;
                    vote(topLevel, engaged, voteValue);
                    comment(engaged, entry.recipe(), nextText(REPLY_TEMPLATES), topLevel);
                }
            }
        }
    }

    private void seedBookmarks(List<User> u, List<RecipeEntry> entries) {
        for (int i = 0; i < u.size(); i++) {
            User bookmarker = u.get(i);
            int saved = 0;
            int offset = 2;
            while (saved < 3) {
                RecipeEntry candidate = entries.get((i + offset) % entries.size());
                if (candidate.ownerIndex() != i) {
                    bookmarkRepository.save(Bookmark.builder()
                        .user(bookmarker)
                        .recipe(candidate.recipe())
                        .createdAt(LocalDateTime.now().minusDays(offset))
                        .build());
                    saved++;
                }
                offset++;
            }
        }
    }

    private RecipeEntry entry(List<User> u, int ownerIndex, String title, String description, String prepTime,
            List<String> tags, List<Ingredient> ingredients, List<Step> steps) {
        Recipe recipe = new Recipe(title, u.get(ownerIndex));
        recipe.setDescription(description);
        recipe.setPrepTime(prepTime);
        recipe.setTags(tags);
        recipe.setIngredients(ingredients);
        recipe.setSteps(steps);
        recipe.setUpdated(LocalDateTime.now());
        return new RecipeEntry(recipeRepository.save(recipe), ownerIndex);
    }

    private User user(String username, String email) {
        User u = new User(username, email, passwordEncoder.encode(SEED_PASSWORD));
        return userRepository.save(u);
    }

    private Ingredient ing(String name, String measurement, String quantity) {
        return new Ingredient(name, measurement, new BigDecimal(quantity));
    }

    private Step step(int order, String instruction) {
        return new Step(instruction, order);
    }

    private Comment comment(User author, Recipe recipe, String content, Comment parent) {
        commentCounter++;
        Comment c = Comment.builder()
            .user(author)
            .recipe(recipe)
            .content(content)
            .parent(parent)
            .deleted(false)
            .createdAt(LocalDateTime.now().minusHours(commentCounter))
            .build();
        return commentRepository.save(c);
    }

    private void vote(Comment comment, User voter, int value) {
        commentVoteRepository.save(CommentVote.builder()
            .comment(comment)
            .user(voter)
            .value(value)
            .build());
    }

    private String nextText(String[] pool) {
        return pool[commentCounter % pool.length];
    }
}

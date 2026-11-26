package com.example.expensetracker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import com.example.expensetracker.config.AppConfig;
import com.example.expensetracker.dao.BudgetDao;
import com.example.expensetracker.dao.CategoryDao;
import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.dao.UserDao;
import com.example.expensetracker.dao.jdbc.JdbcBudgetDao;
import com.example.expensetracker.dao.jdbc.JdbcCategoryDao;
import com.example.expensetracker.dao.jdbc.JdbcTransactionDao;
import com.example.expensetracker.dao.jdbc.JdbcUserDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.dto.BudgetRequest;
import com.example.expensetracker.dto.CategoryRequest;
import com.example.expensetracker.dto.RegisterRequest;
import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.TransactionType;
import com.example.expensetracker.security.JwtUtil;
import com.example.expensetracker.security.PasswordHasher;
import com.example.expensetracker.service.AuthService;
import com.example.expensetracker.service.BudgetService;
import com.example.expensetracker.service.CategoryService;
import com.example.expensetracker.service.TransactionService;

/**
 * Fills the database with three demo users, each with a different spending pattern (Jan to Nov 2026).
 * Goes through the services, so the same rules apply as for the API. Users that already exist are skipped.
 *
 *   java -cp target/expense-tracker.jar com.example.expensetracker.SeedData
 *
 * Logins: yashmit@test.com / yashmit_123, raghav@test.com / raghav_123, vatsal@test.com / vatsal_123
 */
public class SeedData {

    private static final Logger log = Logger.getLogger(SeedData.class.getName());

    private static final YearMonth FIRST_MONTH = YearMonth.of(2026, 1);
    private static final YearMonth LAST_MONTH = YearMonth.of(2026, 11);
    private static final int LAST_DAY_OF_LAST_MONTH = 25;

    private final UserDao userDao;
    private final CategoryService categoryService;
    private final AuthService authService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;

    // per user while seeding
    private Long userId;
    private Map<String, Long> categoryIds;
    private Random random;

    private SeedData(AppConfig config) {
        Database database = new Database(config.require("db.path"));
        database.initSchema();

        userDao = new JdbcUserDao(database);
        CategoryDao categoryDao = new JdbcCategoryDao(database);
        TransactionDao transactionDao = new JdbcTransactionDao(database);
        BudgetDao budgetDao = new JdbcBudgetDao(database);

        // the token secret doesn't matter here, tokens from register() are thrown away
        JwtUtil jwtUtil = new JwtUtil("seed-only-secret-not-used-for-real-logins", 1);
        categoryService = new CategoryService(database, categoryDao, transactionDao, budgetDao);
        authService = new AuthService(database, userDao, new PasswordHasher(), jwtUtil, categoryService);
        transactionService = new TransactionService(transactionDao, categoryService);
        budgetService = new BudgetService(budgetDao, transactionDao, categoryService);
    }

    public static void main(String[] args) throws Exception {
        Main.setUpLogging();
        SeedData seed = new SeedData(new AppConfig());
        seed.yashmit();
        seed.raghav();
        seed.vatsal();
        log.info("Seeding done");
    }

    // ---- Yashmit: salaried, steady spending, tight food budget, gym habit ------------------------

    private void yashmit() {
        if (!startUser("Yashmit Vaish", "yashmit@test.com", "yashmit_123", 11L)) {
            return;
        }
        addCategory("Gym", TransactionType.EXPENSE);
        addCategory("Interest", TransactionType.INCOME);

        for (YearMonth m = FIRST_MONTH; !m.isAfter(LAST_MONTH); m = m.plusMonths(1)) {
            income(m, 1, "Salary", 85000, "Monthly salary");
            if (m.getMonthValue() % 3 == 0) {
                income(m, 20, "Freelance", between(8000, 15000), "Website project");
            }
            if (m.getMonthValue() % 6 == 0) {
                income(m, 30, "Interest", between(1200, 1800), "Savings interest");
            }

            expense(m, 3, "Rent", 22000, "Flat rent");
            expense(m, 5, "Gym", 2500, "Gym membership");
            expense(m, 8, "Bills & Utilities", between(1800, 2600), "Electricity + wifi");
            expense(m, 12, "Bills & Utilities", 599, "Phone recharge");
            for (int i = 0; i < 9; i++) {
                expense(m, day(m), "Food", between(180, 950), pick("Swiggy", "Zomato", "Groceries", "Lunch with team", "Cafe"));
            }
            for (int i = 0; i < 5; i++) {
                expense(m, day(m), "Transport", between(90, 450), pick("Uber", "Metro card", "Rapido", "Petrol"));
            }
            if (random.nextInt(3) > 0) {
                expense(m, day(m), "Shopping", between(1200, 6000), pick("Clothes", "Amazon order", "Shoes", "Headphones"));
            }
            if (random.nextBoolean()) {
                expense(m, day(m), "Entertainment", between(300, 1500), pick("Movie", "Netflix", "Concert", "Bowling"));
            }
        }
        expense(LAST_MONTH, 22, "Health", 3200, "Dentist");

        budget("2026-11", null, 40000);
        budget("2026-11", "Food", 5000);
        budget("2026-11", "Shopping", 4000);
        budget("2026-11", "Transport", 2000);
        budget("2026-10", null, 40000);
        budget("2026-10", "Food", 5000);
    }

    // ---- Raghav: student + freelancer, irregular income, eats out a lot, over budget ------------

    private void raghav() {
        if (!startUser("Raghav", "raghav@test.com", "raghav_123", 22L)) {
            return;
        }
        addCategory("Books", TransactionType.EXPENSE);
        addCategory("Coffee", TransactionType.EXPENSE);
        addCategory("Pocket Money", TransactionType.INCOME);

        for (YearMonth m = FIRST_MONTH; !m.isAfter(LAST_MONTH); m = m.plusMonths(1)) {
            income(m, 1, "Pocket Money", 15000, "From home");
            int gigs = random.nextInt(3);
            for (int i = 0; i < gigs; i++) {
                income(m, day(m), "Freelance", between(3000, 12000), pick("Logo design", "App bug fixes", "Tutoring"));
            }
            if (m.getMonthValue() == 3 || m.getMonthValue() == 10) {
                income(m, 15, "Gifts", between(2000, 5000), "Birthday / Diwali");
            }

            expense(m, 2, "Rent", 8000, "PG rent");
            for (int i = 0; i < 14; i++) {
                expense(m, day(m), "Food", between(80, 600), pick("Canteen", "Maggi point", "Dominos", "Swiggy", "Momos"));
            }
            for (int i = 0; i < 8; i++) {
                expense(m, day(m), "Coffee", between(60, 280), pick("Starbucks", "Third Wave", "Chai + snacks"));
            }
            for (int i = 0; i < 4; i++) {
                expense(m, day(m), "Transport", between(40, 300), pick("Auto", "Bus pass", "Rapido"));
            }
            expense(m, day(m), "Entertainment", between(400, 2500), pick("BGMI UC", "Movie", "Spotify + YouTube", "Trip with friends"));
            if (random.nextBoolean()) {
                expense(m, day(m), "Books", between(250, 1800), pick("DSA book", "Novel", "Course on Udemy"));
            }
        }
        // a transaction moved to Uncategorized, like after deleting a category
        expense(LAST_MONTH, 18, Category.UNCATEGORIZED, 750, "Something I forgot to label");

        budget("2026-11", null, 22000);
        budget("2026-11", "Food", 3000);       // will be over
        budget("2026-11", "Coffee", 1000);     // will be over
        budget("2026-11", "Entertainment", 2000);
    }

    // ---- Vatsal: high earner, travel and investments, mostly under budget -----------------------

    private void vatsal() {
        if (!startUser("Vatsal", "vatsal@test.com", "vatsal_123", 33L)) {
            return;
        }
        addCategory("Travel", TransactionType.EXPENSE);
        addCategory("Investments", TransactionType.EXPENSE);
        addCategory("Dividends", TransactionType.INCOME);

        for (YearMonth m = FIRST_MONTH; !m.isAfter(LAST_MONTH); m = m.plusMonths(1)) {
            income(m, 1, "Salary", 140000, "Monthly salary");
            if (m.getMonthValue() == 3) {
                income(m, 25, "Salary", 120000, "Annual bonus");
            }
            if (m.getMonthValue() % 4 == 0) {
                income(m, 18, "Dividends", between(2500, 6000), "Stock dividends");
            }

            expense(m, 4, "Rent", 38000, "2BHK rent");
            expense(m, 5, "Investments", 25000, "Mutual fund SIP");
            expense(m, 10, "Bills & Utilities", between(3500, 5500), "Electricity, gas, internet");
            expense(m, 15, "Health", 1800, "Health insurance");
            for (int i = 0; i < 7; i++) {
                expense(m, day(m), "Food", between(400, 3500), pick("Fine dining", "Groceries (BigBasket)", "Brunch", "Zomato"));
            }
            for (int i = 0; i < 3; i++) {
                expense(m, day(m), "Transport", between(300, 1500), pick("Uber Premier", "Fuel", "Parking"));
            }
            expense(m, day(m), "Shopping", between(2000, 12000), pick("Gadgets", "Clothes", "Home decor", "Gifts"));
            if (m.getMonthValue() == 5 || m.getMonthValue() == 8 || m.getMonthValue() == 11) {
                expense(m, 12, "Travel", between(18000, 45000), pick("Goa trip", "Manali trip", "Flights to Bangalore"));
            }
        }

        budget("2026-11", null, 135000);
        budget("2026-11", "Food", 15000);
        budget("2026-11", "Shopping", 10000);
        budget("2026-11", "Travel", 40000);
        budget("2026-10", null, 120000);
    }

    // ---- helpers ------------------------------------------------------------------------------

    /** Registers the user, or returns false if they already exist. randomSeed keeps the data the same on every run. */
    private boolean startUser(String name, String email, String password, long randomSeed) {
        if (userDao.existsByEmail(email)) {
            log.info(email + " already exists, skipping");
            return false;
        }
        userId = authService.register(new RegisterRequest(name, email, password)).user().id();
        random = new Random(randomSeed);
        categoryIds = new HashMap<>();
        refreshCategories();
        log.info("Seeding " + email);
        return true;
    }

    private void addCategory(String name, TransactionType type) {
        categoryService.create(userId, new CategoryRequest(name, type));
        refreshCategories();
    }

    private void refreshCategories() {
        for (Category c : categoryService.getAll(userId, null)) {
            categoryIds.put(c.getType() + ":" + c.getName(), c.getId());
        }
    }

    private void income(YearMonth month, int day, String category, double amount, String description) {
        add(TransactionType.INCOME, month, day, category, amount, description);
    }

    private void expense(YearMonth month, int day, String category, double amount, String description) {
        add(TransactionType.EXPENSE, month, day, category, amount, description);
    }

    private void add(TransactionType type, YearMonth month, int day, String category, double amount, String description) {
        int lastDay = month.equals(LAST_MONTH) ? LAST_DAY_OF_LAST_MONTH : month.lengthOfMonth();
        LocalDate date = month.atDay(Math.min(day, lastDay));
        Long categoryId = categoryIds.get(type + ":" + category);
        transactionService.create(userId, new TransactionRequest(
                type, BigDecimal.valueOf(amount), date, categoryId, description));
    }

    private void budget(String month, String category, double amount) {
        Long categoryId = category == null ? null : categoryIds.get("EXPENSE:" + category);
        budgetService.create(userId, new BudgetRequest(categoryId, month, BigDecimal.valueOf(amount)));
    }

    /** A random whole-rupee amount between min and max. */
    private double between(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private int day(YearMonth month) {
        int lastDay = month.equals(LAST_MONTH) ? LAST_DAY_OF_LAST_MONTH : month.lengthOfMonth();
        return 1 + random.nextInt(lastDay);
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }
}

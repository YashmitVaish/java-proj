package com.example.expensetracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.example.expensetracker.config.AppConfig;
import com.example.expensetracker.controller.AuthController;
import com.example.expensetracker.controller.BudgetController;
import com.example.expensetracker.controller.CategoryController;
import com.example.expensetracker.controller.ExportController;
import com.example.expensetracker.controller.ReportController;
import com.example.expensetracker.controller.TransactionController;
import com.example.expensetracker.controller.UserController;
import com.example.expensetracker.dao.BudgetDao;
import com.example.expensetracker.dao.CategoryDao;
import com.example.expensetracker.dao.TransactionDao;
import com.example.expensetracker.dao.UserDao;
import com.example.expensetracker.dao.jdbc.JdbcBudgetDao;
import com.example.expensetracker.dao.jdbc.JdbcCategoryDao;
import com.example.expensetracker.dao.jdbc.JdbcTransactionDao;
import com.example.expensetracker.dao.jdbc.JdbcUserDao;
import com.example.expensetracker.db.Database;
import com.example.expensetracker.http.Cors;
import com.example.expensetracker.http.Router;
import com.example.expensetracker.security.JwtUtil;
import com.example.expensetracker.security.PasswordHasher;
import com.example.expensetracker.service.AuthService;
import com.example.expensetracker.service.BudgetService;
import com.example.expensetracker.service.CategoryService;
import com.example.expensetracker.service.ExportService;
import com.example.expensetracker.service.ReportService;
import com.example.expensetracker.service.TransactionService;
import com.example.expensetracker.service.UserService;
import com.sun.net.httpserver.HttpServer;

/**
 * Starts the app: reads the settings, prepares the database, creates every object
 * (DAOs -> services -> controllers) and starts the HTTP server.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        setUpLogging();
        Logger log = Logger.getLogger(Main.class.getName());

        // settings
        AppConfig config = new AppConfig();

        // database
        Database database = new Database(config.require("db.path"));
        database.initSchema();

        // data access layer
        UserDao userDao = new JdbcUserDao(database);
        CategoryDao categoryDao = new JdbcCategoryDao(database);
        TransactionDao transactionDao = new JdbcTransactionDao(database);
        BudgetDao budgetDao = new JdbcBudgetDao(database);

        // business logic
        PasswordHasher passwordHasher = new PasswordHasher();
        JwtUtil jwtUtil = new JwtUtil(config.require("jwt.secret"), config.getInt("jwt.expiration.hours"));
        CategoryService categoryService = new CategoryService(database, categoryDao, transactionDao, budgetDao);
        AuthService authService = new AuthService(database, userDao, passwordHasher, jwtUtil, categoryService);
        UserService userService = new UserService(userDao, passwordHasher);
        TransactionService transactionService = new TransactionService(transactionDao, categoryService);
        BudgetService budgetService = new BudgetService(budgetDao, transactionDao, categoryService);
        ReportService reportService = new ReportService(transactionDao);
        ExportService exportService = new ExportService(transactionService, Path.of("exports"));

        // endpoints
        Router router = new Router(jwtUtil, new Cors(config.getList("cors.allowed.origins")));
        new AuthController(authService).register(router);
        new UserController(userService).register(router);
        new CategoryController(categoryService).register(router);
        new TransactionController(transactionService).register(router);
        new BudgetController(budgetService).register(router);
        new ReportController(reportService).register(router);
        new ExportController(exportService).register(router);

        // HTTP server: a pool of 10 threads handles requests in parallel
        int port = config.getInt("server.port");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService threads = Executors.newFixedThreadPool(10);
        server.createContext("/", router);
        server.setExecutor(threads);
        server.start();
        log.info("Server running on http://localhost:" + port + "/api");

        // Ctrl+C: finish in-flight requests, then exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down");
            server.stop(2);
            threads.shutdown();
        }));
    }

    private static void setUpLogging() throws IOException {
        try (InputStream in = Main.class.getResourceAsStream("/logging.properties")) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
            }
        }
    }
}

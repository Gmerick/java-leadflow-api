const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
 testDir: './e2e', fullyParallel: false, workers: 1, retries: process.env.CI ? 1 : 0,
 reporter: [['list'], ['html', { open: 'never' }]],
 use: { launchOptions: process.env.BROWSER_EXECUTABLE ? { executablePath: process.env.BROWSER_EXECUTABLE } : {}, baseURL: 'http://127.0.0.1:18082', viewport: { width: 1440, height: 960 }, screenshot: 'only-on-failure', trace: 'retain-on-failure' },
 webServer: { command: 'java -jar target/app.jar --server.port=18082 "--spring.datasource.url=jdbc:h2:mem:ui;DB_CLOSE_DELAY=-1"', url: 'http://127.0.0.1:18082', reuseExistingServer: false, timeout: 90000 }
});

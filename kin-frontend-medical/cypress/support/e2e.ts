// cypress/support/e2e.ts
import './commands';

// Global configuration for e2e tests
beforeEach(() => {
  cy.clearLocalStorage();
  cy.clearCookies();
});

afterEach(() => {
  cy.clearLocalStorage();
  cy.clearCookies();
});

// Handle uncaught exceptions
Cypress.on('uncaught:exception', (err, runnable) => {
  // Return false to prevent Cypress from failing the test
  if (err.message.includes('hydration') || err.message.includes('Hydration')) {
    return false;
  }
  if (err.message.includes('next/router')) {
    return false;
  }
  return true;
});
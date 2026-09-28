// cypress/support/commands.ts
/// <reference types="cypress" />

declare global {
  namespace Cypress {
    interface Chainable {
      loginAsPhysician(email: string, password: string): Chainable<void>;
      loginAsPatient(email: string, password: string): Chainable<void>;
      selectPatient(patientName: string): Chainable<void>;
      navigateToWizard(encounterId: string): Chainable<void>;
      fillIdentificationStep(data: IdentificationData): Chainable<void>;
      fillMotiveStep(data: MotiveData): Chainable<void>;
      fillIllnessStep(data: IllnessData): Chainable<void>;
      fillHistoryStep(data: HistoryData): Chainable<void>;
      fillPhysicalExamStep(data: PhysicalExamData): Chainable<void>;
      fillDiagnosisPlanStep(data: DiagnosisPlanData): Chainable<void>;
      closeEncounter(): Chainable<void>;
    }
  }
}

interface IdentificationData {
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  sex: string;
}

interface MotiveData {
  encounterType: string;
  chiefComplaint: string;
}

interface IllnessData {
  onsetDatetime: string;
  evolutionDescription: string;
  severitySelfReported: number;
}

interface HistoryData {
  allergies: Array<{ allergen: string; severity: string }>;
}

interface PhysicalExamData {
  bpSystolic: number;
  bpDiastolic: number;
  heartRate: number;
  temperature: number;
  weightKg: number;
  heightCm: number;
}

interface DiagnosisPlanData {
  diagnoses: Array<{ cie10Code: string; type: string; isPrincipal: boolean }>;
  treatmentPlan: { conduct: string; prognosis: string };
  orders: Array<{ orderType: string; drugName?: string; cupsCode?: string }>;
}

Cypress.Commands.add('loginAsPhysician', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('[data-cy="email-input"]').type(email);
  cy.get('[data-cy="password-input"]').type(password);
  cy.get('[data-cy="login-button"]').click();
  cy.url().should('include', '/dashboard/physician');
});

Cypress.Commands.add('loginAsPatient', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('[data-cy="email-input"]').type(email);
  cy.get('[data-cy="password-input"]').type(password);
  cy.get('[data-cy="login-button"]').click();
  cy.url().should('include', '/dashboard/patient');
});

Cypress.Commands.add('selectPatient', (patientName: string) => {
  cy.contains('button', patientName).click();
});

Cypress.Commands.add('navigateToWizard', (encounterId: string) => {
  cy.visit(`/dashboard/physician/hce/${encounterId}/edit`);
  cy.contains('h1', 'Historia Clínica Electrónica').should('be.visible');
});

Cypress.Commands.add('fillIdentificationStep', (data: IdentificationData) => {
  cy.get('select[name="documentType"]').select(data.documentType);
  cy.get('input[name="documentNumber"]').type(data.documentNumber);
  cy.get('input[name="firstName"]').type(data.firstName);
  cy.get('input[name="lastName"]').type(data.lastName);
  cy.get('input[name="birthDate"]').type(data.birthDate);
  cy.get('select[name="sex"]').select(data.sex);
  cy.contains('button', 'Guardar Identificación').click();
  cy.contains('Guardado automático').should('be.visible');
});

Cypress.Commands.add('fillMotiveStep', (data: MotiveData) => {
  cy.get('select[name="encounterType"]').select(data.encounterType);
  cy.get('textarea[name="chiefComplaint"]').type(data.chiefComplaint);
  cy.contains('button', 'Guardar Motivo').click();
  cy.contains('Guardado automático').should('be.visible');
});

Cypress.Commands.add('fillIllnessStep', (data: IllnessData) => {
  cy.get('input[name="onsetDatetime"]').type(data.onsetDatetime);
  cy.get('textarea[name="evolutionDescription"]').type(data.evolutionDescription);
  cy.get('input[name="severitySelfReported"]').invoke('val', data.severitySelfReported).trigger('change');
  cy.contains('button', 'Guardar Enfermedad Actual').click();
  cy.contains('Guardado automático').should('be.visible');
});

Cypress.Commands.add('fillHistoryStep', (data: HistoryData) => {
  data.allergies.forEach((allergy) => {
    cy.contains('button', 'Agregar').click();
    cy.get('[role="dialog"]').within(() => {
      cy.get('input[name="allergen"]').type(allergy.allergen);
      cy.get('select[name="severity"]').select(allergy.severity);
      cy.contains('button', 'Guardar').click();
    });
  });
});

Cypress.Commands.add('fillPhysicalExamStep', (data: PhysicalExamData) => {
  cy.get('input[name="bpSystolic"]').type(data.bpSystolic.toString());
  cy.get('input[name="bpDiastolic"]').type(data.bpDiastolic.toString());
  cy.get('input[name="heartRate"]').type(data.heartRate.toString());
  cy.get('input[name="temperature"]').type(data.temperature.toString());
  cy.get('input[name="weightKg"]').type(data.weightKg.toString());
  cy.get('input[name="heightCm"]').type(data.heightCm.toString());
  cy.contains('button', 'Guardar Examen Físico').click();
  cy.contains('Guardado automático').should('be.visible');
});

Cypress.Commands.add('fillDiagnosisPlanStep', (data: DiagnosisPlanData) => {
  data.diagnoses.forEach((diag, index) => {
    cy.contains('button', 'Agregar Diagnóstico').click();
    cy.get('[role="dialog"]').within(() => {
      cy.get('input[name="cie10Code"]').type(diag.cie10Code);
      cy.get('select[name="type"]').select(diag.type);
      if (diag.isPrincipal) {
        cy.get('input[name="isPrincipal"]').check();
      }
      cy.contains('button', 'Guardar').click();
    });
  });

  if (data.treatmentPlan) {
    cy.get('select[name="conduct"]').select(data.treatmentPlan.conduct);
    cy.get('select[name="prognosis"]').select(data.treatmentPlan.prognosis);
    cy.contains('button', 'Guardar Todo').click();
  }

  data.orders.forEach((order) => {
    cy.contains('button', 'Agregar Orden').click();
    cy.get('[role="dialog"]').within(() => {
      cy.get('[role="radio"]').contains(order.orderType).click();
      if (order.drugName) {
        cy.get('input[name="drugName"]').type(order.drugName);
      }
      if (order.cupsCode) {
        cy.get('input[name="cupsCode"]').type(order.cupsCode);
      }
      cy.contains('button', 'Guardar').click();
    });
  });
});

Cypress.Commands.add('closeEncounter', () => {
  cy.contains('button', 'Cerrar y Firmar Consulta').click();
  cy.contains('h2', 'Confirmar Cierre de Consulta').should('be.visible');
  cy.contains('button', 'Confirmar y Firmar').click();
  cy.url().should('include', '/dashboard/physician/hce/');
  cy.url().should('not.include', '/edit');
});

export {};
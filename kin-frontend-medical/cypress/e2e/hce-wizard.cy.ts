// cypress/e2e/hce-wizard.cy.ts
/// <reference types="cypress" />

describe('HCE Wizard - Flujo completo', () => {
  const physicianEmail = 'physician@test.com';
  const physicianPassword = 'testpassword123';
  const patientName = 'Juan Pérez';
  const encounterId = 'test-encounter-id';

  beforeEach(() => {
    cy.clearLocalStorage();
    cy.clearCookies();
  });

  describe('Test 1: Wizard completo happy path', () => {
    it('debe permitir llenar los 7 pasos y cerrar la consulta', () => {
      cy.loginAsPhysician(physicianEmail, physicianPassword);
      cy.selectPatient(patientName);
      cy.navigateToWizard(encounterId);

      // Paso 1: Identificación
      cy.fillIdentificationStep({
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
      });
      cy.get('[data-cy="stepper"]').contains('Motivo').click();

      // Paso 2: Motivo
      cy.fillMotiveStep({
        encounterType: 'OUTPATIENT',
        chiefComplaint: 'Dolor abdominal en cuadrante inferior derecho',
      });
      cy.get('[data-cy="stepper"]').contains('Enf. Actual').click();

      // Paso 3: Enfermedad Actual
      cy.fillIllnessStep({
        onsetDatetime: '2024-01-15T10:00:00',
        evolutionDescription: 'Dolor progresivo de 3 días de evolución, localizado en FID',
        severitySelfReported: 7,
      });
      cy.get('[data-cy="stepper"]').contains('Antecedentes').click();

      // Paso 4: Antecedentes
      cy.fillHistoryStep({
        allergies: [{ allergen: 'Penicilina', severity: 'MODERATE' }],
      });
      cy.get('[data-cy="stepper"]').contains('Examen Físico').click();

      // Paso 5: Examen Físico
      cy.fillPhysicalExamStep({
        bpSystolic: 120,
        bpDiastolic: 80,
        heartRate: 72,
        temperature: 36.5,
        weightKg: 70,
        heightCm: 175,
      });
      cy.get('[data-cy="stepper"]').contains('Dx y Plan').click();

      // Paso 6: Diagnóstico y Plan
      cy.fillDiagnosisPlanStep({
        diagnoses: [
          { cie10Code: 'K59.0', type: 'PRINCIPAL', isPrincipal: true },
          { cie10Code: 'R10.9', type: 'SECUNDARIO', isPrincipal: false },
        ],
        treatmentPlan: { conduct: 'OUTPATIENT_TREATMENT', prognosis: 'GOOD' },
        orders: [
          { orderType: 'MEDICATION', drugName: 'Paracetamol' },
          { orderType: 'LAB_EXAM', cupsCode: '900301' },
        ],
      });
      cy.get('[data-cy="stepper"]').contains('Cierre').click();

      // Paso 7: Cierre
      cy.closeEncounter();

      // Verificar redirección a vista de encounter cerrado
      cy.url().should('include', '/dashboard/physician/hce/');
      cy.url().should('not.include', '/edit');
      cy.contains('Consulta cerrada exitosamente').should('be.visible');
    });
  });

  describe('Test 2: Error al cerrar sin diagnóstico principal', () => {
    it('debe mostrar error al intentar cerrar sin diagnóstico principal', () => {
      cy.loginAsPhysician(physicianEmail, physicianPassword);
      cy.selectPatient(patientName);
      cy.navigateToWizard(encounterId);

      // Llenar pasos mínimos sin diagnóstico principal
      cy.fillIdentificationStep({
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
      });
      cy.get('[data-cy="stepper"]').contains('Motivo').click();

      cy.fillMotiveStep({
        encounterType: 'OUTPATIENT',
        chiefComplaint: 'Dolor abdominal',
      });
      cy.get('[data-cy="stepper"]').contains('Enf. Actual').click();

      cy.fillIllnessStep({
        onsetDatetime: '2024-01-15T10:00:00',
        evolutionDescription: 'Dolor progresivo',
        severitySelfReported: 7,
      });
      cy.get('[data-cy="stepper"]').contains('Antecedentes').click();
      cy.get('[data-cy="stepper"]').contains('Examen Físico').click();

      cy.fillPhysicalExamStep({
        bpSystolic: 120,
        bpDiastolic: 80,
        heartRate: 72,
        temperature: 36.5,
        weightKg: 70,
        heightCm: 175,
      });
      cy.get('[data-cy="stepper"]').contains('Dx y Plan').click();

      // Solo diagnóstico secundario, sin principal
      cy.fillDiagnosisPlanStep({
        diagnoses: [
          { cie10Code: 'R10.9', type: 'SECUNDARIO', isPrincipal: false },
        ],
        treatmentPlan: { conduct: 'OUTPATIENT_TREATMENT', prognosis: 'GOOD' },
        orders: [],
      });
      cy.get('[data-cy="stepper"]').contains('Cierre').click();

      // Verificar error en paso de cierre
      cy.contains('Faltan requisitos para cerrar').should('be.visible');
      cy.contains('Falta diagnóstico PRINCIPAL').should('be.visible');
      cy.get('button').contains('Cerrar y Firmar Consulta').should('be.disabled');
    });
  });

  describe('Test 3: Error al cerrar sin plan de manejo', () => {
    it('debe mostrar error al intentar cerrar sin plan de manejo', () => {
      cy.loginAsPhysician(physicianEmail, physicianPassword);
      cy.selectPatient(patientName);
      cy.navigateToWizard(encounterId);

      cy.fillIdentificationStep({
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
      });
      cy.get('[data-cy="stepper"]').contains('Motivo').click();

      cy.fillMotiveStep({
        encounterType: 'OUTPATIENT',
        chiefComplaint: 'Dolor abdominal',
      });
      cy.get('[data-cy="stepper"]').contains('Enf. Actual').click();

      cy.fillIllnessStep({
        onsetDatetime: '2024-01-15T10:00:00',
        evolutionDescription: 'Dolor progresivo',
        severitySelfReported: 7,
      });
      cy.get('[data-cy="stepper"]').contains('Antecedentes').click();
      cy.get('[data-cy="stepper"]').contains('Examen Físico').click();

      cy.fillPhysicalExamStep({
        bpSystolic: 120,
        bpDiastolic: 80,
        heartRate: 72,
        temperature: 36.5,
        weightKg: 70,
        heightCm: 175,
      });
      cy.get('[data-cy="stepper"]').contains('Dx y Plan').click();

      // Con diagnóstico principal pero SIN plan de manejo
      cy.fillDiagnosisPlanStep({
        diagnoses: [
          { cie10Code: 'K59.0', type: 'PRINCIPAL', isPrincipal: true },
        ],
        treatmentPlan: undefined as any,
        orders: [],
      });
      cy.get('[data-cy="stepper"]').contains('Cierre').click();

      // Verificar error en paso de cierre
      cy.contains('Faltan requisitos para cerrar').should('be.visible');
      cy.contains('Falta plan de manejo').should('be.visible');
      cy.get('button').contains('Cerrar y Firmar Consulta').should('be.disabled');
    });
  });

  describe('Test 4: Autoguardado funciona', () => {
    it('debe autoguardar y recuperar datos al refrescar', () => {
      cy.loginAsPhysician(physicianEmail, physicianPassword);
      cy.selectPatient(patientName);
      cy.navigateToWizard(encounterId);

      cy.fillIdentificationStep({
        documentType: 'CC',
        documentNumber: '1234567890',
        firstName: 'Juan',
        lastName: 'Pérez',
        birthDate: '1990-01-15',
        sex: 'M',
      });

      // Esperar autoguardado (30 segundos)
      cy.contains('Guardado automático', { timeout: 35000 }).should('be.visible');

      // Refrescar página
      cy.reload();

      // Verificar que los datos persisten
      cy.get('select[name="documentType"]').should('have.value', 'CC');
      cy.get('input[name="documentNumber"]').should('have.value', '1234567890');
      cy.get('input[name="firstName"]').should('have.value', 'Juan');
      cy.get('input[name="lastName"]').should('have.value', 'Pérez');
    });
  });

  describe('Test 5: Ownership - médico no puede editar encounter ajeno', () => {
    it('debe mostrar error 403 al intentar editar encounter de otro médico', () => {
      // Login como médico A
      cy.loginAsPhysician('physicianA@test.com', 'password123');
      cy.selectPatient(patientName);
      cy.navigateToWizard(encounterId);

      // Verificar que puede acceder
      cy.contains('h1', 'Historia Clínica Electrónica').should('be.visible');

      // Cerrar sesión
      cy.clearLocalStorage();
      cy.clearCookies();

      // Login como médico B
      cy.loginAsPhysician('physicianB@test.com', 'password123');
      cy.selectPatient(patientName);

      // Intentar navegar al mismo encounter
      cy.visit(`/dashboard/physician/hce/${encounterId}/edit`);

      // Debe mostrar error 403 o redirigir
      cy.url().should('not.include', '/edit');
      cy.contains('No autorizado').should('be.visible');
    });
  });
});
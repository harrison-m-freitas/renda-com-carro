import test from 'node:test';
import assert from 'node:assert/strict';

import {
  allocationMethodDescription,
  classificationDescription,
  paymentDescription
} from '../../main/resources/static/js/expense-choice-descriptions.js';

test('expense choices: classification help only describes the active option', () => {
  assert.equal(
    classificationDescription('PROFESSIONAL'),
    'Todo o valor será considerado custo da operação.'
  );
  assert.equal(
    classificationDescription('PERSONAL'),
    'O valor não reduzirá o resultado profissional.'
  );
  assert.equal(
    classificationDescription('MIXED'),
    'Uma parte será profissional e a outra pessoal.'
  );
  assert.equal(classificationDescription(''), '');
});

test('expense choices: payment help only describes the active option', () => {
  assert.equal(paymentDescription('PAID'), 'O pagamento já foi realizado.');
  assert.equal(paymentDescription('PENDING'), 'O gasto ainda será pago.');
  assert.equal(paymentDescription(''), '');
});

test('expense choices: allocation help only describes the active method', () => {
  assert.equal(
    allocationMethodDescription('MILEAGE_RATIO'),
    'Usa o fechamento confirmado ou a estimativa disponível para o mês.'
  );
  assert.equal(
    allocationMethodDescription('MANUAL_PERCENTAGE'),
    'Informe quanto do gasto pertence à operação em percentual.'
  );
  assert.equal(
    allocationMethodDescription('FIXED_AMOUNT'),
    'Informe diretamente o valor profissional do gasto.'
  );
  assert.equal(allocationMethodDescription(''), '');
});
import test from "node:test";
import assert from "node:assert/strict";
import {
  selectedVehicleValues,
  toggleVehicleField,
  vehicleSelectionLabel,
} from "../../main/resources/static/js/goal-vehicle-picker.js";

test("goal vehicle picker preserves the submitted checkbox state as the single source of truth", () => {
  const changes = [];
  const fields = [
    field("a", "Sandero 2013", true, changes),
    field("b", "Fiorino 2018", false, changes),
  ];
  assert.deepEqual(selectedVehicleValues(fields), ["a"]);
  toggleVehicleField(fields[1], Event);
  assert.deepEqual(selectedVehicleValues(fields), ["a", "b"]);
  assert.deepEqual(changes, ["b"]);
});

test("goal vehicle picker summarizes zero one and multiple vehicles", () => {
  assert.equal(vehicleSelectionLabel([]), "Selecione pelo menos um veículo");
  assert.equal(vehicleSelectionLabel(["Sandero 2013"]), "Sandero 2013");
  assert.equal(vehicleSelectionLabel(["Sandero 2013", "Fiorino 2018"]), "2 veículos selecionados");
});

function field(value, label, checked, changes) {
  return {
    value,
    checked,
    dataset: { vehicleLabel: label },
    dispatchEvent(event) { if (event.type === "change") changes.push(value); },
  };
}

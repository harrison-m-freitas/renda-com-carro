package dev.harrison.rendacomcarro.vehicle.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleFormTest {
    @Test
    void normalizesWhitespaceWithoutChangingCapitalization() {
        VehicleForm form = new VehicleForm();

        form.setName("  Sandero   principal  ");
        form.setMake("  Land\u00A0  Rover  ");
        form.setModel("  CR-V  ");

        assertThat(form.getName()).isEqualTo("Sandero principal");
        assertThat(form.getMake()).isEqualTo("Land Rover");
        assertThat(form.getModel()).isEqualTo("CR-V");
    }

    @Test
    void keepsBlankNicknameNullAndNormalizesBothPlatePresentations() {
        VehicleForm form = new VehicleForm();

        form.setName("   ");
        form.setPlate(" abc-1234 ");

        assertThat(form.getName()).isNull();
        assertThat(form.getPlate()).isEqualTo("ABC1234");

        form.setPlate("abc1d23");
        assertThat(form.getPlate()).isEqualTo("ABC1D23");
    }
}

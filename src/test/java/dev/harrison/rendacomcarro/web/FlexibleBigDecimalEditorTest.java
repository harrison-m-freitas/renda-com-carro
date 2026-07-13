package dev.harrison.rendacomcarro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.harrison.rendacomcarro.shared.web.FlexibleBigDecimalEditor;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FlexibleBigDecimalEditorTest {
    @Test
    void acceptsBrazilianAndTechnicalDecimalFormats() {
        FlexibleBigDecimalEditor editor = new FlexibleBigDecimalEditor();

        editor.setAsText("1.234,56");
        assertThat(editor.getValue()).isEqualTo(new BigDecimal("1234.56"));

        editor.setAsText("1234.56");
        assertThat(editor.getValue()).isEqualTo(new BigDecimal("1234.56"));

        editor.setAsText("248.351");
        assertThat(editor.getValue()).isEqualTo(new BigDecimal("248351"));

        editor.setAsText("75");
        assertThat(editor.getValue()).isEqualTo(new BigDecimal("75"));

        editor.setAsText("-1,00");
        assertThat(editor.getValue()).isEqualTo(new BigDecimal("-1.00"));
    }

    @Test
    void blankBecomesNullAndRenderingUsesComma() {
        FlexibleBigDecimalEditor editor = new FlexibleBigDecimalEditor();
        editor.setAsText("   ");
        assertThat(editor.getValue()).isNull();

        editor.setValue(new BigDecimal("1234.50"));
        assertThat(editor.getAsText()).isEqualTo("1234,50");
    }

    @Test
    void invalidNumberHasFriendlyMessage() {
        FlexibleBigDecimalEditor editor = new FlexibleBigDecimalEditor();

        assertThatThrownBy(() -> editor.setAsText("doze reais"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("número válido");
    }
}

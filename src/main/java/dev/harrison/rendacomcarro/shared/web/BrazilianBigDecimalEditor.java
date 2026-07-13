package dev.harrison.rendacomcarro.shared.web;

import dev.harrison.rendacomcarro.shared.domain.FlexibleDecimalParser;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

public class BrazilianBigDecimalEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        try {
            setValue(FlexibleDecimalParser.parseBrazilian(text));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe um número válido.", exception);
        }
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        if (value == null) {
            return "";
        }
        return ((BigDecimal) value).toPlainString().replace('.', ',');
    }
}

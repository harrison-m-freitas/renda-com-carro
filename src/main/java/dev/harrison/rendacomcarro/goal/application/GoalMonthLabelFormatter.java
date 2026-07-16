package dev.harrison.rendacomcarro.goal.application;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class GoalMonthLabelFormatter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
        .ofPattern("MMMM 'de' uuuu", Locale.forLanguageTag("pt-BR"));

    public String format(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Mês é obrigatório");
        }
        String raw = FORMATTER.format(month.atDay(1));
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}

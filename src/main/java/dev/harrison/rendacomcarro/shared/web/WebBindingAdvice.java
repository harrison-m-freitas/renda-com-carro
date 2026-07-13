package dev.harrison.rendacomcarro.shared.web;

import java.math.BigDecimal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class WebBindingAdvice {
    @InitBinder
    void bindFlexibleDecimals(WebDataBinder binder) {
        binder.registerCustomEditor(
            BigDecimal.class,
            new FlexibleBigDecimalEditor()
        );
    }
}

package dev.harrison.rendacomcarro.expense.application;

import dev.harrison.rendacomcarro.draft.application.FormDraftService;
import dev.harrison.rendacomcarro.draft.domain.FormDraftType;
import dev.harrison.rendacomcarro.expense.domain.AllocationMethod;
import dev.harrison.rendacomcarro.expense.domain.Expense;
import dev.harrison.rendacomcarro.expense.domain.ExpenseClassification;
import dev.harrison.rendacomcarro.expense.web.ExpenseForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseFormSubmissionService {
    private final ExpenseService expenses;
    private final FormDraftService drafts;

    public ExpenseFormSubmissionService(
        ExpenseService expenses,
        FormDraftService drafts
    ) {
        this.expenses = expenses;
        this.drafts = drafts;
    }

    @Transactional
    public Expense submit(String username, ExpenseForm form) {
        NormalizedExpense normalized = normalize(form);
        Expense created = expenses.create(new ExpenseService.CreateExpenseCommand(
            form.getVehicleId(),
            form.getOperationalDayId(),
            form.getShiftId(),
            form.getCategoryId(),
            form.getExpenseDate(),
            requireCompetence(form).atDay(1),
            normalized.paidDate(),
            form.getAmount(),
            form.getClassification(),
            normalized.allocationMethod(),
            normalized.professionalPercentage(),
            normalized.professionalFixedAmount(),
            normalized.adjustmentReason(),
            form.getNotes()
        ));
        drafts.completeAll(
            username,
            FormDraftType.EXPENSE,
            List.of(
                nullToEmpty(form.getDraftContextKey()),
                nullToEmpty(form.getPreviousDraftContextKey())
            )
        );
        return created;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private NormalizedExpense normalize(ExpenseForm form) {
        if (form.getPaymentStatus() == null) {
            throw invalid("paymentStatus", "Informe a situação do pagamento.");
        }
        LocalDate paidDate = form.getPaymentStatus() == ExpenseForm.PaymentStatus.PENDING
            ? null
            : requirePaidDate(form.getPaidDate());

        if (form.getClassification() != ExpenseClassification.MIXED) {
            return new NormalizedExpense(paidDate, null, null, null, null);
        }

        AllocationMethod method = form.getAllocationMethod();
        if (method == null) {
            throw invalid("allocationMethod", "Escolha como o gasto misto será dividido.");
        }
        return switch (method) {
            case MILEAGE_RATIO -> new NormalizedExpense(paidDate, method, null, null, null);
            case MANUAL_PERCENTAGE -> new NormalizedExpense(
                paidDate,
                method,
                requirePercentage(form.getProfessionalPercentagePercent()),
                null,
                requireReason(form.getAdjustmentReason())
            );
            case FIXED_AMOUNT -> new NormalizedExpense(
                paidDate,
                method,
                null,
                requireFixedAmount(form.getProfessionalFixedAmount(), form.getAmount()),
                requireReason(form.getAdjustmentReason())
            );
        };
    }

    private java.time.YearMonth requireCompetence(ExpenseForm form) {
        if (form.getCompetenceMonth() == null) {
            throw invalid("competenceMonth", "Informe o mês de referência.");
        }
        return form.getCompetenceMonth();
    }

    private LocalDate requirePaidDate(LocalDate paidDate) {
        if (paidDate == null) {
            throw invalid("paidDate", "Informe a data do pagamento.");
        }
        return paidDate;
    }

    private BigDecimal requirePercentage(BigDecimal percentagePercent) {
        if (percentagePercent == null) {
            throw invalid("professionalPercentagePercent", "Informe o percentual profissional.");
        }
        if (percentagePercent.signum() == 0) {
            throw invalid("professionalPercentagePercent", "Para 0%, classifique o gasto como Pessoal.");
        }
        if (percentagePercent.compareTo(new BigDecimal("100")) == 0) {
            throw invalid(
                "professionalPercentagePercent",
                "Para 100%, classifique o gasto como Profissional."
            );
        }
        if (percentagePercent.signum() < 0
            || percentagePercent.compareTo(new BigDecimal("100")) > 0) {
            throw invalid(
                "professionalPercentagePercent",
                "Informe um percentual maior que 0% e menor que 100%."
            );
        }
        return percentagePercent.movePointLeft(2);
    }

    private BigDecimal requireFixedAmount(BigDecimal fixed, BigDecimal total) {
        if (fixed == null) {
            throw invalid("professionalFixedAmount", "Informe o valor profissional.");
        }
        if (fixed.signum() == 0) {
            throw invalid(
                "professionalFixedAmount",
                "Para nenhum valor profissional, classifique o gasto como Pessoal."
            );
        }
        if (total != null && fixed.compareTo(total) == 0) {
            throw invalid(
                "professionalFixedAmount",
                "Para atribuir todo o valor à operação, classifique o gasto como Profissional."
            );
        }
        if (fixed.signum() < 0 || (total != null && fixed.compareTo(total) > 0)) {
            throw invalid(
                "professionalFixedAmount",
                "O valor profissional deve ser menor que o valor total do gasto."
            );
        }
        return fixed;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw invalid(
                "adjustmentReason",
                "Explique por que o rateio foi informado manualmente."
            );
        }
        return reason.trim().replaceAll("\\s+", " ");
    }

    private ExpenseFormValidationException invalid(String field, String message) {
        return new ExpenseFormValidationException(field, message);
    }

    private record NormalizedExpense(
        LocalDate paidDate,
        AllocationMethod allocationMethod,
        BigDecimal professionalPercentage,
        BigDecimal professionalFixedAmount,
        String adjustmentReason
    ) {
    }
}

package com.github.mustafamalikdev.banking.services.validation;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.requests.MerchantRequestBody;
import com.github.mustafamalikdev.banking.repository.TransactionRepository;
import com.github.mustafamalikdev.banking.services.PaymentValidation;
import com.github.mustafamalikdev.banking.services.security.OneTimeGeneratorService;
import com.github.mustafamalikdev.banking.services.transaction.PaymentTransactionService;
import com.github.mustafamalikdev.banking.util.FileHelper;
import com.github.mustafamalikdev.banking.util.JsonFileHelper;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public final class MerchantVerificationService extends PaymentValidation {

    private List<MerchantRequestBody> merchantData;

    @Value("classpath:models/merchants/merchant_db.json")
    private Resource resourceJson;
    private final TransactionRepository transactionRepository;
    private final OneTimeGeneratorService oneTimeGeneratorService;
    private final PaymentTransactionService paymentTransactionService;

    @Autowired
    public MerchantVerificationService(
        TransactionRepository transactionRepository,
        OneTimeGeneratorService oneTimeGeneratorService,
        PaymentTransactionService paymentTransactionService
    ) {
        this.transactionRepository = transactionRepository;
        this.oneTimeGeneratorService = oneTimeGeneratorService;
        this.paymentTransactionService = paymentTransactionService;
    }

    private boolean isMerchantValid(final MerchantRequestBody merchantInfo) {
        if (merchantInfo == null || merchantData.isEmpty()) {
            return false;
        }

        return merchantData.stream()
                .anyMatch(merchant -> (
                    Objects.equals(merchant.merchantId(), merchantInfo.merchantId()) &&
                    merchant.merchantName().equalsIgnoreCase(merchantInfo.merchantName()) &&
                    merchant.location() == merchantInfo.location() &&
                    merchant.merchantTaxId().equalsIgnoreCase(merchantInfo.merchantTaxId())
                ));
    }

    @Override
    public void reconcileData() {
        if (merchantData == null) {
            try {
                merchantData = JsonFileHelper.getList(resourceJson, MerchantRequestBody.class);
            } catch (Exception ignored) { }
        }
    }

    // notes for myself
    // Check the location of the merchant
    // Verify the merchant records using dummy data
    // also add new columns to the DB (amount, error code <used to determine what action is needed>)
    @Override
    public ErrorCode validate(String requestId) {
        super.validate(requestId);

        ErrorCode errorCode;
        TransactionModel model = transactionRepository.findByRequestId(requestId);
        System.out.println(model);
        MerchantRequestBody merchantInfo = model.getMerchant();

        if (isMerchantValid(merchantInfo)) {
            if (
                merchantInfo.location() != model.getLocation() &&
                model.getErrorCode() != ErrorCode.PAYMENT_OTP_VERIFIED.getError()
            ) {
                // overseas transactions need verification from the customer
                errorCode = ErrorCode.PAYMENT_REQUIRES_VERIFICATION;
                model.setErrorCode(errorCode);

                paymentTransactionService.updatePendingTransaction(model);
                oneTimeGeneratorService.generateSimple();

                FileHelper.writeTransactionLog(model);

                System.out.println("Transaction: " + model.getRequestId() + " requires extra verification.");
                return ErrorCode.PAYMENT_REQUIRES_VERIFICATION;
            }
        } else {
            errorCode = ErrorCode.PAYMENT_FRAUD_DETECTED;
            model.setErrorCode(errorCode);

            paymentTransactionService.updatePendingTransactionFraud(model);
            FileHelper.writeTransactionLog(model);

            System.out.println("Fraud detected! Transaction: " + model.getRequestId() + " is declined.");
            return ErrorCode.PAYMENT_FRAUD_DETECTED;
        }

        if (isMerchantValid(merchantInfo) && model.getErrorCode() == ErrorCode.PAYMENT_OTP_VERIFIED.getError()) {
            model.setErrorCode(ErrorCode.PAYMENT_OK);
            System.out.println("Payment verified!");

            FileHelper.writeTransactionLog(model);
            paymentTransactionService.updatePendingTransaction(model);
            return ErrorCode.PAYMENT_OK;
        }

        return ErrorCode.PAYMENT_UNKNOWN_ERROR;
    }
}

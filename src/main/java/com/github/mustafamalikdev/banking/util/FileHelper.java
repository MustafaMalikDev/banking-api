package com.github.mustafamalikdev.banking.util;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.requests.MerchantRequestBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileHelper {

    private static final String LOG_DIR = "/app/logs";

    public FileHelper() { }

    public static void writeLog(String contents) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));

        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, timestamp + ".txt");

        try {
            if (file.createNewFile()) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    fileOutputStream.write(contents.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not create log file: " + e.getMessage());
        }
    }

    public static void writeTransactionLog(TransactionModel data) {
        LocalDateTime nowTime = LocalDateTime.now();
        MerchantRequestBody merchant = data.getMerchant();

        String formattedLog = String.format(
                "--------------------------------------------------------------------------------\n" +
                        " SYSTEM TRANSACTION REPORT   \n" +
                        " TIMESTAMP: %-30s                                                       \n" +
                        " TRANSACTION ID: %s \n" +
                        "--------------------------------------------------------------------------------\n" +
                        " ACCOUNT DETAILS\n" +
                        "   Account Number  : %-20s  Region : %s\n" +
                        "\n" +
                        " MERCHANT DATA\n" +
                        "   Merchant Name   : %-20s  ID     : %s\n" +
                        "   Merchant Region : %s\n" +
                        "\n" +
                        " TRANSACTION SPECIFICATION\n" +
                        "   Amount Due      : GBP %.2f\n" +
                        "   Outcome         : %s (Code: %s)\n" +
                        "--------------------------------------------------------------------------------\n" +
                        " END OF TRANSMISSION LOG\n" +
                        "--------------------------------------------------------------------------------\n",
                nowTime,
                data.getRequestId(),
                data.getCustomer().accountNumber(),
                data.getLocation(),
                merchant.merchantName(),
                merchant.merchantId(),
                merchant.location(),
                data.getAmount(),
                ErrorCode.getMessage(data.getErrorCode()),
                data.getErrorCode()
        );

        FileHelper.writeLog(formattedLog);
    }
}
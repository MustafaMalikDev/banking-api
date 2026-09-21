# REST API Design

### Table of Contents:
- Technical Explanation
- Languages & Tech Stack used

---

## Technical Explanation

Important:
Since this is API is meant to simulate the effects, it cannot be
used in production, nor should be used in production. All sample data
are fabricated and should not be used for any illegal activities. I am not 
responsible for anything you do with this piece of software.

Sample data can be found in the `resources/models` folder. It contains
sample data for merchants and customers.

This is a REST API that handles transactional payments with a built-in fraud detection system.
A customer can send a POST request with a payload consisting of the following:

```json
{
    "merchant": {
        "merchantName": "NAME HERE",
        "location": "LOC_REGIN_NAME_HERE",
        "merchantId": 1234567,
        "merchantTaxId": "NAME_ID_REGION_HERE"
    },
    "customer": {
        "accountNumber": 123456789,
        "sortCode": "123456"
    },
    "requestId": "ID_HERE",
    "location": "LOC_REGION_NAME_HERE",
    "amount": 0
}
```

This payload is sent to the endpoint `POST /api/v1/payment/request`.
<br/>

### How does a customer account get verified

Sending a double charge on the same Request ID will have no effect due to idempotency being implemented.

Upon receiving the request through the controller, the `AccountVerificationService` class will check the account data
to see if it matches the data in `accounts.json`. This is meant to simulate registered customers with the bank, but 
in a simple manner.

If the details do not match, the request is immediately rejected as fraudulent activity. However, if the details are a 
match, the amount request is checked. If the amount is £150 or above, the request requires an OTP code. If the amount
is under, the entire process below is skipped and the transaction moves onto merchant verification.

#### OTP Verification

The endpoint will return HTTP 202 (ACCEPTED) and the request will be stored in Postgres (and set to `PAYMENT_REQUIRES_VERIFICATION`) 
along with being cached in Redis. Once that is done, a token is generated and sent in the HTTP 202 body. This represents 
the OTP data. The customer must send back the correct OTP code to allow the transaction to proceed to the final stages.

An OTP code is generated and stored in Redis with a TTL (time-to-live) for 5 minutes. After that, it is invalidated.

The OTP code is sent to the endpoint `POST /api/v1/payment/authorise/{payment_id}/{otp_code}`.

If the OTP code is incorrect, the request is rejected and no further action is taken. If the OTP code is correct,
the transaction data in Postgres is updated to `PAYMENT_PROCESSING` and the cache is revalidated to reflect these changes.

### How does a merchant get verified
 
Once the account verification phase has been completed, a message is pushed into a RabbitMQ queue.
Once the consumer picks up on this new message, an asynchronous task executor is scheduled and executes the
`MerchantVerificationService` class in a new thread to avoid blocking the main thread for future incoming transactions.

The merchant's details are checked against a list of trusted merchants in the dummy file `merchants.json`. If the merchant
is not found, the request is rejected and is marked as fraudulent activity. If the merchant is found, the location of the
merchant's region is compared to the region of the transaction request. If both regions do not match, the transaction is
updated to `PAYMENT_REQUIRES_VERIFICATION` and an OTP token is generated. The same process follows from the OTP Verification section.

Once verification is complete or the regions match, the transaction is completed and updated to `PAYMENT_OK`. Any messages
for this transaction are cleared from the RabbitMQ queue, removed from Redis Cache but retained in Postgres. A full log
of the transaction completed is generated as a report.

### Transaction Logging

Each stage of the transaction is logged and stored on disk until manual deletion.

**When the transaction is acknowledged and in the processing phase:**
```
--------------------------------------------------------------------------------
 SYSTEM TRANSACTION REPORT   
 TIMESTAMP: 2026-09-19T22:30:20.738800                                                           
 TRANSACTION ID: bf4ae9d9803samb9euak9 
--------------------------------------------------------------------------------
 ACCOUNT DETAILS
   Account Number  : 10482937              Region : LOC_EUROPE

 MERCHANT DATA
   Merchant Name   : Sony Group Corp       ID     : 451920
   Merchant Region : LOC_NORTH_AMERICA

 TRANSACTION SPECIFICATION
   Amount Due      : GBP 20.00
   Outcome         : Your transaction is being processed. It will be authorised once the payment information has been verified. (Code: 1)
--------------------------------------------------------------------------------
 END OF TRANSMISSION LOG
--------------------------------------------------------------------------------

```

**When the transaction is in the verification phase:**
```
--------------------------------------------------------------------------------
 SYSTEM TRANSACTION REPORT   
 TIMESTAMP: 2026-09-19T22:30:20.786750                                                           
 TRANSACTION ID: bf4ae9d9803samb9euak9 
--------------------------------------------------------------------------------
 ACCOUNT DETAILS
   Account Number  : 10482937              Region : LOC_EUROPE

 MERCHANT DATA
   Merchant Name   : Sony Group Corp       ID     : 451920
   Merchant Region : LOC_NORTH_AMERICA

 TRANSACTION SPECIFICATION
   Amount Due      : GBP 20.00
   Outcome         : This transaction requires verification. Please submit OTP code to confirm. (Code: 2)
--------------------------------------------------------------------------------
 END OF TRANSMISSION LOG
--------------------------------------------------------------------------------
```

**When the transaction is successfully completed:**
```
--------------------------------------------------------------------------------
 SYSTEM TRANSACTION REPORT   
 TIMESTAMP: 2026-09-19T22:30:27.849433                                                           
 TRANSACTION ID: bf4ae9d9803samb9euak9 
--------------------------------------------------------------------------------
 ACCOUNT DETAILS
   Account Number  : 10482937              Region : LOC_EUROPE

 MERCHANT DATA
   Merchant Name   : Sony Group Corp       ID     : 451920
   Merchant Region : LOC_NORTH_AMERICA

 TRANSACTION SPECIFICATION
   Amount Due      : GBP 20.00
   Outcome         : Transaction approved. Thank you for your purchase. (Code: -1)
--------------------------------------------------------------------------------
 END OF TRANSMISSION LOG
--------------------------------------------------------------------------------
```

These logs are generated in the maven output directory `target/classes/{local_date_time_format.txt}`

## Languages & Tech Stack Used

- Java 21 JDK
- IntelliJ IDEA Ultimate
- Spring Boot 4.1.1
- Maven
- PostgreSQL
- RabbitMQ
- Redis

---
Developed by Mustafa Malik. See LICENCE for more information.
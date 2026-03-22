package co.edu.unipiloto.fuelcontrol.models;

import java.math.BigDecimal;

public class PaymentSummaryResponse {
    private Long paymentId;
    private String fuelType;
    private BigDecimal gallons;
    private BigDecimal amount;
    private String status;
    private String message;

    public Long getPaymentId() { return paymentId; }
    public String getFuelType() { return fuelType; }
    public BigDecimal getGallons() { return gallons; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
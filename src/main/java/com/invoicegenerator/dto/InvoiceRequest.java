package com.invoicegenerator.dto;

import java.util.List;

import lombok.Data;

@Data
public class InvoiceRequest {

    // Company Details
    private String companyLogo;   // Base64 image string for logo
    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyGstin;

    // Buyer Details
    private String buyerName;
    private String buyerAddress;
    private String buyerPhone;
    private String buyerGstin;
    private String placeOfSupply;

    // Invoice Details
    private String invoiceNumber;
    private String invoiceDate;
    private String deliveryNote;
    private String paymentTerms;

    // Product List
    private List<ProductItem> products;

    // Tax Details
    private Double cgst;
    private Double sgst;
    private Double igst;
    private Double roundOff;
    private Double grandTotal;

    // Bank Details
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String branchName;

    // Declaration
    private String declaration;
}
package com.invoicegenerator.dto;

import lombok.Data;

@Data
public class ProductItem {

    private String productName;
    private String hsnSac;
    private Integer quantity;
    private Double rate;
    private Double amount;
}
package com.invoicegenerator.service;

import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Service;

import com.invoicegenerator.dto.InvoiceRequest;
import com.invoicegenerator.util.PdfGenerator;

@Service
public class InvoiceService {

    public ByteArrayInputStream generateInvoicePdf(InvoiceRequest request) {
        return PdfGenerator.generate(request);
    }
}
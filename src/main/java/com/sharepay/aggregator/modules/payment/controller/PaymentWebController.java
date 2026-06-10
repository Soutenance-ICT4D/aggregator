package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.modules.collect.repository.FundCollectionRepository;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.service.TicketPdfService;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentWebController {

    private final TransactionInRepository   transactionInRepository;
    private final FundCollectionRepository  fundCollectionRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final TicketPdfService          ticketPdfService;

    @Value("${app.website-url:https://sharepay.cm}")
    private String sharePayWebsiteUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // Page de paiement Checkout (lien marchand via sessionToken)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/checkout/{sessionToken}")
    public String checkoutPage(@PathVariable String sessionToken, Model model) {
        TransactionIn tx = transactionInRepository.findBySessionTokenWithApplication(sessionToken)
                .orElse(null);

        if (tx == null) {
            model.addAttribute("errorCode", "SESSION_NOT_FOUND");
            model.addAttribute("errorMessage", "Lien de paiement introuvable.");
            return "checkout-error";
        }

        if (tx.getType() != TransactionInType.CHECKOUT) {
            model.addAttribute("errorCode", "INVALID_SESSION");
            model.addAttribute("errorMessage", "Lien de paiement invalide.");
            return "checkout-error";
        }

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            model.addAttribute("errorCode", "ALREADY_PAID");
            model.addAttribute("errorMessage", "Ce paiement a déjà été effectué avec succès.");
            return "checkout-error";
        }

        if (tx.getStatus() != TransactionStatus.PENDING) {
            model.addAttribute("errorCode", "SESSION_CLOSED");
            model.addAttribute("errorMessage", "Cette session de paiement est terminée.");
            return "checkout-error";
        }

        if (tx.getExpiresAt() != null && tx.getExpiresAt().isBefore(OffsetDateTime.now())) {
            model.addAttribute("errorCode", "SESSION_EXPIRED");
            model.addAttribute("errorMessage", "Ce lien de paiement a expiré.");
            return "checkout-error";
        }

        model.addAttribute("sessionToken",      sessionToken);
        model.addAttribute("reference",         tx.getReference());
        model.addAttribute("amount",            tx.getAmount());
        model.addAttribute("currency",          tx.getCurrency());
        model.addAttribute("description",       tx.getDescription());
        model.addAttribute("appName",           tx.getApplication().getName());
        model.addAttribute("sharePayWebsiteUrl", sharePayWebsiteUrl);
        model.addAttribute("providers",
                paymentProviderRepository.findByCurrencyAndIsActiveTrueOrderByNameAsc(tx.getCurrency()));

        log.debug("[Checkout] Page affichée - sessionToken: {}, ref: {}", sessionToken, tx.getReference());
        return "checkout";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page de paiement Collecte (lien public via slug)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/collect/{slug}")
    public String collectPage(@PathVariable String slug, Model model) {
        FundCollection collection = fundCollectionRepository.findBySlug(slug)
                .orElse(null);

        if (collection == null || collection.getStatus() == FundCollectionStatus.DELETED) {
            model.addAttribute("errorCode", "COLLECT_NOT_FOUND");
            model.addAttribute("errorMessage", "Cette collecte est introuvable.");
            return "checkout-error";
        }

        if (collection.getStatus() == FundCollectionStatus.CLOSED) {
            model.addAttribute("errorCode", "COLLECT_CLOSED");
            model.addAttribute("errorMessage", "Cette collecte est clôturée.");
            return "checkout-error";
        }

        if (collection.getStatus() == FundCollectionStatus.EXPIRED
                || (collection.getExpiresAt() != null && collection.getExpiresAt().isBefore(OffsetDateTime.now()))) {
            model.addAttribute("errorCode", "COLLECT_EXPIRED");
            model.addAttribute("errorMessage", "Cette collecte a expiré.");
            return "checkout-error";
        }

        model.addAttribute("slug",                slug);
        model.addAttribute("title",               collection.getTitle());
        model.addAttribute("description",         collection.getDescription());
        model.addAttribute("coverImageUrl",       collection.getCoverImageUrl());
        model.addAttribute("currency",            collection.getCurrency());
        model.addAttribute("isAmountFixed",       collection.isAmountFixed());
        model.addAttribute("amount",              collection.getAmount());
        model.addAttribute("expiresAt",           collection.getExpiresAt());
        model.addAttribute("collectCustomerInfo",  collection.isCollectCustomerInfo());
        model.addAttribute("thankYouMessage",      collection.getThankYouMessage());
        model.addAttribute("sharePayWebsiteUrl",   sharePayWebsiteUrl);
        model.addAttribute("providers",
                paymentProviderRepository.findByCurrencyAndIsActiveTrueOrderByNameAsc(collection.getCurrency()));

        log.debug("[Collect] Page affichée - slug: {}", slug);
        return "collect";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reçu / Ticket de paiement (accès public via référence)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/download-ticket/{reference}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadTicket(@PathVariable String reference) {
        TransactionIn tx = transactionInRepository.findByReferenceWithDetails(reference).orElse(null);

        if (tx == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (tx.getStatus() != TransactionStatus.SUCCESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("reference",         tx.getReference());
        vars.put("merchantReference", tx.getMerchantReference());
        vars.put("amount",            tx.getAmount());
        vars.put("currency",          tx.getCurrency());
        vars.put("description",       tx.getDescription());
        vars.put("status",            tx.getStatus().name());
        vars.put("createdAt",         tx.getCreatedAt());
        vars.put("providerName",      tx.getPaymentProvider() != null ? tx.getPaymentProvider().getName() : null);
        vars.put("appName",           tx.getApplication().getName());
        vars.put("payerAccount",      tx.getPayerAccount());
        vars.put("payerName",         tx.getPayerName() != null ? tx.getPayerName() : tx.getCustomerName());
        vars.put("websiteUrl",        sharePayWebsiteUrl);

        byte[] pdf = ticketPdfService.generateReceiptPdf(vars);

        log.info("[Ticket] Reçu PDF généré - ref: {}", reference);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"recu-" + reference + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.aspsp.xs2a.service.payment;

import de.adorsys.aspsp.xs2a.domain.TppInfo;
import de.adorsys.aspsp.xs2a.domain.pis.BulkPayment;
import de.adorsys.aspsp.xs2a.domain.pis.PaymentInitialisationResponse;
import de.adorsys.aspsp.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.aspsp.xs2a.domain.pis.SinglePayment;
import de.adorsys.aspsp.xs2a.service.mapper.PaymentMapper;
import de.adorsys.aspsp.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiBulkPaymentMapper;
import de.adorsys.aspsp.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPaymentMapper;
import de.adorsys.aspsp.xs2a.spi.domain.SpiResponse;
import de.adorsys.aspsp.xs2a.spi.domain.consent.AspspConsentData;
import de.adorsys.aspsp.xs2a.spi.domain.payment.SpiPaymentInitialisationResponse;
import de.adorsys.aspsp.xs2a.spi.domain.v2.SpiBulkPayment;
import de.adorsys.aspsp.xs2a.spi.service.PaymentSpi;
import de.adorsys.aspsp.xs2a.spi.service.v2.BulkPaymentSpi;
import de.adorsys.aspsp.xs2a.spi.service.v2.SinglePaymentSpi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedirectAndEmbeddedPaymentService implements ScaPaymentService {
    private final PaymentSpi paymentSpi;
    private final PaymentMapper paymentMapper;

    private final SinglePaymentSpi singlePaymentSpi;
    private final BulkPaymentSpi bulkPaymentSpi;
    private final Xs2aToSpiPaymentMapper xs2aToSpiPaymentMapper;
    private final Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper;

    @Override
    public PaymentInitialisationResponse createSinglePayment(SinglePayment payment, TppInfo tppInfo, String paymentProduct) {
        // TODO Read and update aspspConsentData before and after initiatePayment  https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/391
        // TODO don't create AspspConsentData without consentId https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/332
        SpiResponse<SpiPaymentInitialisationResponse> response = singlePaymentSpi.initiatePayment(xs2aToSpiPaymentMapper.mapToSpiSinglePayment(payment, paymentProduct), new AspspConsentData());
        return paymentMapper.mapToPaymentInitializationResponse(response.getPayload(), response.getAspspConsentData());
    }

    @Override
    public PaymentInitialisationResponse createPeriodicPayment(PeriodicPayment payment, TppInfo tppInfo, String paymentProduct) {
        AspspConsentData aspspConsentData = new AspspConsentData(); //TODO don't create AspspConsentData without consentId https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/332
        SpiResponse<SpiPaymentInitialisationResponse> response = paymentSpi.initiatePeriodicPayment(paymentMapper.mapToSpiPeriodicPayment(payment), aspspConsentData);
        return paymentMapper.mapToPaymentInitializationResponse(response.getPayload(), response.getAspspConsentData());
    }

    @Override
    public List<PaymentInitialisationResponse> createBulkPayment(BulkPayment bulkPayment, TppInfo tppInfo, String paymentProduct) {
        AspspConsentData aspspConsentData = new AspspConsentData(); //TODO don't create AspspConsentData without consentId https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/332
        SpiBulkPayment spiBulkPayment = xs2aToSpiBulkPaymentMapper.mapToSpiBulkPayment(bulkPayment, paymentProduct);
        SpiResponse<List<SpiPaymentInitialisationResponse>> response = bulkPaymentSpi.initiatePayment(spiBulkPayment, aspspConsentData);
        AspspConsentData responseAspspConsentData = response.getAspspConsentData();

        return response.getPayload()
                   .stream()
                   .map(resp -> paymentMapper.mapToPaymentInitializationResponse(resp, responseAspspConsentData))
                   .collect(Collectors.toList());
    }
}

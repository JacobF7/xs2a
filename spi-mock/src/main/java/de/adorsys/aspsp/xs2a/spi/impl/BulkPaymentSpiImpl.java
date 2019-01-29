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

package de.adorsys.aspsp.xs2a.spi.impl;

import de.adorsys.aspsp.xs2a.spi.config.rest.AspspRemoteUrls;
import de.adorsys.aspsp.xs2a.spi.mapper.SpiBulkPaymentMapper;
import de.adorsys.aspsp.xs2a.spi.mapper.SpiPaymentMapper;
import de.adorsys.psd2.aspsp.mock.api.common.AspspTransactionStatus;
import de.adorsys.psd2.aspsp.mock.api.payment.AspspBulkPayment;
import de.adorsys.psd2.aspsp.mock.api.payment.AspspSinglePayment;
import de.adorsys.psd2.aspsp.mock.api.psu.AspspPsuData;
import de.adorsys.psd2.xs2a.component.JsonConverter;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.exception.RestException;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiScaConfirmation;
import de.adorsys.psd2.xs2a.spi.domain.common.SpiTransactionStatus;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiBulkPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiBulkPaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponseStatus;
import de.adorsys.psd2.xs2a.spi.service.BulkPaymentSpi;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BulkPaymentSpiImpl implements BulkPaymentSpi {
    private static final String TEST_ASPSP_DATA = "Test aspsp data";

    @Qualifier("aspspRestTemplate")
    private final RestTemplate aspspRestTemplate;
    private final AspspRemoteUrls aspspRemoteUrls;
    private final SpiPaymentMapper spiPaymentMapper;
    private final SpiBulkPaymentMapper spiBulkPaymentMapper;
    private final JsonConverter jsonConverter;

    @Override
    @NotNull
    public SpiResponse<SpiBulkPaymentInitiationResponse> initiatePayment(@NotNull SpiContextData spiContextData, @NotNull SpiBulkPayment payment, @NotNull AspspConsentData initialAspspConsentData) {
        try {
            AspspBulkPayment request = spiBulkPaymentMapper.mapToAspspBulkPayment(payment, SpiTransactionStatus.RCVD);
            ResponseEntity<AspspBulkPayment> aspspResponse = aspspRestTemplate.postForEntity(aspspRemoteUrls.createBulkPayment(), request, AspspBulkPayment.class);
            SpiBulkPaymentInitiationResponse response = spiBulkPaymentMapper.mapToSpiBulkPaymentResponse(aspspResponse.getBody(), payment.getPaymentProduct());

            AspspConsentData resultAspspData = initialAspspConsentData.respondWith(TEST_ASPSP_DATA.getBytes());
            List<AspspPsuData> psuDataList = aspspResponse.getBody().getPsuDataList();

            if (CollectionUtils.size(psuDataList) > 1) {
                Map<String, Boolean> authMap = psuDataList.stream()
                                                   .map(AspspPsuData::getPsuId)
                                                   .collect(Collectors.toMap(Function.identity(), id -> false));
                byte[] bytes = jsonConverter.toJson(authMap)
                                   .map(String::getBytes)
                                   .orElse(TEST_ASPSP_DATA.getBytes());

                resultAspspData = initialAspspConsentData.respondWith(bytes);
            }

            return SpiResponse.<SpiBulkPaymentInitiationResponse>builder()
                       .aspspConsentData(resultAspspData)
                       .payload(response)
                       .success();

        } catch (RestException e) {
            if (e.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return SpiResponse.<SpiBulkPaymentInitiationResponse>builder()
                           .aspspConsentData(initialAspspConsentData.respondWith(TEST_ASPSP_DATA.getBytes()))
                           .fail(SpiResponseStatus.TECHNICAL_FAILURE);
            }
            return SpiResponse.<SpiBulkPaymentInitiationResponse>builder()
                       .aspspConsentData(initialAspspConsentData.respondWith(TEST_ASPSP_DATA.getBytes()))
                       .fail(SpiResponseStatus.LOGICAL_FAILURE);
        }
    }

    @Override
    public @NotNull SpiResponse<SpiBulkPayment> getPaymentById(@NotNull SpiContextData spiContextData, @NotNull SpiBulkPayment payment, @NotNull AspspConsentData aspspConsentData) {
        try {
            ResponseEntity<List<AspspSinglePayment>> aspspResponse =
                aspspRestTemplate.exchange(aspspRemoteUrls.getPaymentById(), HttpMethod.GET, null, new ParameterizedTypeReference<List<AspspSinglePayment>>() {
                }, payment.getPaymentType().getValue(), payment.getPaymentProduct(), payment.getPaymentId());
            List<AspspSinglePayment> payments = aspspResponse.getBody();
            SpiBulkPayment spiBulkPayment = spiBulkPaymentMapper.mapToSpiBulkPayment(payments, payment.getPaymentProduct());

            return SpiResponse.<SpiBulkPayment>builder()
                       .aspspConsentData(aspspConsentData)
                       .payload(spiBulkPayment)
                       .success();

        } catch (RestException e) {

            if (e.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {

                return SpiResponse.<SpiBulkPayment>builder()
                           .aspspConsentData(aspspConsentData)
                           .fail(SpiResponseStatus.TECHNICAL_FAILURE);
            }

            return SpiResponse.<SpiBulkPayment>builder()
                       .aspspConsentData(aspspConsentData)
                       .fail(SpiResponseStatus.LOGICAL_FAILURE);
        }
    }

    @Override
    public @NotNull SpiResponse<SpiTransactionStatus> getPaymentStatusById(@NotNull SpiContextData spiContextData, @NotNull SpiBulkPayment payment, @NotNull AspspConsentData aspspConsentData) {
        try {
            ResponseEntity<AspspTransactionStatus> aspspResponse = aspspRestTemplate.getForEntity(aspspRemoteUrls.getPaymentStatus(), AspspTransactionStatus.class, payment.getPaymentId());
            SpiTransactionStatus status = spiPaymentMapper.mapToSpiTransactionStatus(aspspResponse.getBody());

            return SpiResponse.<SpiTransactionStatus>builder()
                       .aspspConsentData(aspspConsentData)
                       .payload(status)
                       .success();

        } catch (RestException e) {
            if (e.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return SpiResponse.<SpiTransactionStatus>builder()
                           .aspspConsentData(aspspConsentData)
                           .fail(SpiResponseStatus.TECHNICAL_FAILURE);
            }
            return SpiResponse.<SpiTransactionStatus>builder()
                       .aspspConsentData(aspspConsentData)
                       .fail(SpiResponseStatus.LOGICAL_FAILURE);
        }
    }

    @Override
    public @NotNull SpiResponse<SpiPaymentExecutionResponse> executePaymentWithoutSca(@NotNull SpiContextData spiContextData, @NotNull SpiBulkPayment payment, @NotNull AspspConsentData aspspConsentData) {
        SpiTransactionStatus responseStatus = SpiTransactionStatus.ACCP;
        AspspConsentData responseData = aspspConsentData;

        if (aspspConsentData.getAspspConsentData() != null) {
            Optional<Map> authMapOptional = jsonConverter.toObject(aspspConsentData.getAspspConsentData(), Map.class);
            if (authMapOptional.isPresent()) {
                Map<String, Boolean> authMap = authMapOptional.get();
                String psuId = spiContextData.getPsuData().getPsuId();

                if (!authMap.containsKey(psuId)) {
                    return SpiResponse.<SpiPaymentExecutionResponse>builder()
                               .aspspConsentData(responseData)
                               .fail(SpiResponseStatus.LOGICAL_FAILURE);
                }

                authMap.put(psuId, true);

                if (authMap.values().contains(false)) {
                    responseStatus = SpiTransactionStatus.PATC;
                }


                byte[] bytes = jsonConverter.toJson(authMap)
                                   .map(String::getBytes)
                                   .orElse(TEST_ASPSP_DATA.getBytes());
                responseData = aspspConsentData.respondWith(bytes);
            }
        }

        AspspBulkPayment request = spiBulkPaymentMapper.mapToAspspBulkPayment(payment, responseStatus);

        try {
            aspspRestTemplate.postForEntity(aspspRemoteUrls.createBulkPayment(), request, AspspBulkPayment.class);

            return SpiResponse.<SpiPaymentExecutionResponse>builder()
                       .aspspConsentData(responseData)
                       .payload(new SpiPaymentExecutionResponse(responseStatus))
                       .success();

        } catch (RestException e) {
            if (e.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return SpiResponse.<SpiPaymentExecutionResponse>builder()
                           .aspspConsentData(responseData)
                           .fail(SpiResponseStatus.TECHNICAL_FAILURE);
            }
            return SpiResponse.<SpiPaymentExecutionResponse>builder()
                       .aspspConsentData(responseData)
                       .fail(SpiResponseStatus.LOGICAL_FAILURE);
        }
    }

    @Override
    public @NotNull SpiResponse<SpiPaymentExecutionResponse> verifyScaAuthorisationAndExecutePayment(@NotNull SpiContextData spiContextData, @NotNull SpiScaConfirmation spiScaConfirmation, @NotNull SpiBulkPayment payment, @NotNull AspspConsentData aspspConsentData) {
        SpiTransactionStatus responseStatus = SpiTransactionStatus.ACCP;
        AspspConsentData responseData = aspspConsentData;

        if (aspspConsentData.getAspspConsentData() != null) {
            Optional<Map> authMapOptional = jsonConverter.toObject(aspspConsentData.getAspspConsentData(), Map.class);
            if (authMapOptional.isPresent()) {
                Map<String, Boolean> authMap = authMapOptional.get();
                String psuId = spiContextData.getPsuData().getPsuId();

                if (!authMap.containsKey(psuId)) {
                    return SpiResponse.<SpiPaymentExecutionResponse>builder()
                               .aspspConsentData(responseData)
                               .fail(SpiResponseStatus.LOGICAL_FAILURE);
                }

                authMap.put(psuId, true);

                if (authMap.values().contains(false)) {
                    responseStatus = SpiTransactionStatus.PATC;
                }


                byte[] bytes = jsonConverter.toJson(authMap)
                                   .map(String::getBytes)
                                   .orElse(TEST_ASPSP_DATA.getBytes());
                responseData = aspspConsentData.respondWith(bytes);
            }
        }

        try {
            aspspRestTemplate.exchange(aspspRemoteUrls.applyStrongUserAuthorisation(), HttpMethod.PUT, new HttpEntity<>(spiScaConfirmation), ResponseEntity.class);
            AspspBulkPayment request = spiBulkPaymentMapper.mapToAspspBulkPayment(payment, responseStatus);
            aspspRestTemplate.postForEntity(aspspRemoteUrls.createBulkPayment(), request, AspspBulkPayment.class);

            return SpiResponse.<SpiPaymentExecutionResponse>builder()
                       .payload(new SpiPaymentExecutionResponse(responseStatus))
                       .aspspConsentData(responseData)
                       .success();

        } catch (RestException e) {
            if (e.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return SpiResponse.<SpiPaymentExecutionResponse>builder()
                           .aspspConsentData(responseData)
                           .fail(SpiResponseStatus.TECHNICAL_FAILURE);
            }
            return SpiResponse.<SpiPaymentExecutionResponse>builder()
                       .aspspConsentData(responseData)
                       .fail(SpiResponseStatus.LOGICAL_FAILURE);
        }
    }
}

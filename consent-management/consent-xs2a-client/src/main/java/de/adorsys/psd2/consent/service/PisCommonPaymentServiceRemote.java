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

package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.CmsAuthorisationType;
import de.adorsys.psd2.consent.api.pis.authorisation.CreatePisAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.authorisation.GetPisAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.authorisation.UpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.consent.api.pis.authorisation.UpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.consent.api.pis.proto.CreatePisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentRequest;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentService;
import de.adorsys.psd2.consent.config.CmsRestException;
import de.adorsys.psd2.consent.config.PisCommonPaymentRemoteUrls;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// TODO discuss error handling (e.g. 400 HttpCode response) https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/498
@Slf4j
@Service
@RequiredArgsConstructor
public class PisCommonPaymentServiceRemote implements PisCommonPaymentService {
    @Qualifier("consentRestTemplate")
    private final RestTemplate consentRestTemplate;
    private final PisCommonPaymentRemoteUrls remotePisCommonPaymentUrls;

    @Override
    public Optional<CreatePisCommonPaymentResponse> createCommonPayment(PisPaymentInfo request) {
        return Optional.ofNullable(consentRestTemplate.postForEntity(remotePisCommonPaymentUrls.createPisCommonPayment(), request, CreatePisCommonPaymentResponse.class).getBody());
    }

    @Override
    public Optional<TransactionStatus> getPisCommonPaymentStatusById(String paymentId) {
        return Optional.empty();
    }

    @Override
    public Optional<PisCommonPaymentResponse> getCommonPaymentById(String paymentId) {
        return Optional.ofNullable(consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.getPisCommonPaymentById(), PisCommonPaymentResponse.class, paymentId)
                                       .getBody());
    }

    @Override
    public Optional<Boolean> updateCommonPaymentStatusById(String paymentId, TransactionStatus status) {
        HttpStatus statusCode = consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisCommonPaymentStatus(), HttpMethod.PUT,
                                                             null, Void.class, paymentId, status).getStatusCode();

        return Optional.of(statusCode == HttpStatus.OK);
    }

    @Override
    public Optional<String> getDecryptedId(String encryptedId) {
        return Optional.ofNullable(consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.getPaymentIdByEncryptedString(), String.class, encryptedId)
                                       .getBody());
    }

    @Override
    public Optional<CreatePisAuthorisationResponse> createAuthorization(String paymentId, CmsAuthorisationType authorizationType, PsuIdData psuData) {
        return Optional.ofNullable(consentRestTemplate.postForEntity(remotePisCommonPaymentUrls.createPisAuthorisation(),
                                                                     psuData, CreatePisAuthorisationResponse.class, paymentId)
                                       .getBody());
    }

    @Override
    public Optional<CreatePisAuthorisationResponse> createAuthorizationCancellation(String paymentId, CmsAuthorisationType authorizationType, PsuIdData psuData) {
        return Optional.ofNullable(consentRestTemplate.postForEntity(remotePisCommonPaymentUrls.createPisAuthorisationCancellation(),
                                                                     psuData, CreatePisAuthorisationResponse.class, paymentId)
                                       .getBody());
    }

    @Override
    public Optional<UpdatePisCommonPaymentPsuDataResponse> updateCommonPaymentAuthorisation(String authorisationId, UpdatePisCommonPaymentPsuDataRequest request) {
        return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisAuthorisation(), HttpMethod.PUT, new HttpEntity<>(request),
                                                                UpdatePisCommonPaymentPsuDataResponse.class, request.getAuthorizationId()).getBody());
    }

    @Override
    public Optional<UpdatePisCommonPaymentPsuDataResponse> updateCommonPaymentCancellationAuthorisation(String authorisationId, UpdatePisCommonPaymentPsuDataRequest request) {
        return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisCancellationAuthorisation(), HttpMethod.PUT, new HttpEntity<>(request),
                                                                UpdatePisCommonPaymentPsuDataResponse.class, request.getAuthorizationId()).getBody());
    }

    @Override
    public void updateCommonPayment(PisCommonPaymentRequest request, String paymentId) {
        consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisCommonPayment(), HttpMethod.PUT, new HttpEntity<>(request), Void.class, paymentId);
    }

    @Override
    public Optional<GetPisAuthorisationResponse> getPisCommonPaymentAuthorisationById(String authorizationId) {
        return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.getPisAuthorisationById(), HttpMethod.GET, null, GetPisAuthorisationResponse.class, authorizationId)
                                       .getBody());
    }

    @Override
    public Optional<GetPisAuthorisationResponse> getPisCommonPaymentCancellationAuthorisationById(String cancellationId) {
        return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.getPisCancellationAuthorisationById(), HttpMethod.GET, null, GetPisAuthorisationResponse.class, cancellationId)
                                       .getBody());
    }

    @Override
    public Optional<List<String>> getAuthorisationsByPaymentId(String paymentId, CmsAuthorisationType authorisationType) {
        String url = getAuthorisationSubResourcesUrl(authorisationType);
        try {
            ResponseEntity<List<String>> request = consentRestTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<String>>() {
                }, paymentId);
            return Optional.ofNullable(request.getBody());
        } catch (CmsRestException cmsRestException) {
            log.warn("No authorisation found by paymentId {}", paymentId);
        }
        return Optional.empty();
    }

    private String getAuthorisationSubResourcesUrl(CmsAuthorisationType authorisationType) {
        switch (authorisationType) {
            case CREATED:
                return remotePisCommonPaymentUrls.getAuthorisationSubResources();
            case CANCELLED:
                return remotePisCommonPaymentUrls.getCancellationAuthorisationSubResources();
            default:
                log.error("Unknown payment authorisation type {}", authorisationType);
                throw new IllegalArgumentException("Unknown payment authorisation type " + authorisationType);
        }
    }

    @Override
    public Optional<List<PsuIdData>> getPsuDataListByPaymentId(String paymentId) {
        return Optional.ofNullable(consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.getPsuDataByPaymentId(), PsuIdData[].class, paymentId)
                                   .getBody())
                   .map(Arrays::asList);
    }
}

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

package de.adorsys.psd2.xs2a.service.authorization.pis.stage;

import de.adorsys.psd2.consent.api.pis.authorisation.GetPisAuthorisationResponse;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ChallengeData;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aDecoupledUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.consent.PisAspspDataService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.consent.PisPsuDataService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.CmsToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.*;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthenticationObject;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationStatus;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentCancellationSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.*;

@Service("PIS_CANCELLATION_EMBEDDED_STARTED")
public class PisCancellationScaStartAuthorisationStage extends PisScaStage<Xs2aUpdatePisCommonPaymentPsuDataRequest, GetPisAuthorisationResponse, Xs2aUpdatePisCommonPaymentPsuDataResponse> {
    private final PisAspspDataService pisAspspDataService;
    private final Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper;
    private final SpiContextDataProvider spiContextDataProvider;
    private final ScaApproachResolver scaApproachResolver;
    private final PaymentCancellationSpi paymentCancellationSpi;
    private final SpiErrorMapper spiErrorMapper;
    private final PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted;
    private final SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper;
    private final Xs2aPisCommonPaymentService xs2aPisCommonPaymentService;
    private final PisPsuDataService pisPsuDataService;

    private static final String MESSAGE_ERROR_NO_PSU = "Please provide the PSU identification data";

    public PisCancellationScaStartAuthorisationStage(PaymentCancellationSpi paymentCancellationSpi, PisAspspDataService pisAspspDataService, PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted, CmsToXs2aPaymentMapper cmsToXs2aPaymentMapper, Xs2aToSpiPeriodicPaymentMapper xs2aToSpiPeriodicPaymentMapper, Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper, Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper, SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper, SpiErrorMapper spiErrorMapper, Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper, SpiContextDataProvider spiContextDataProvider, PisPsuDataService pisPsuDataService) {
        super(cmsToXs2aPaymentMapper, xs2aToSpiPeriodicPaymentMapper, xs2aToSpiSinglePaymentMapper, xs2aToSpiBulkPaymentMapper);
        this.pisAspspDataService = pisAspspDataService;
        this.xs2aToSpiPsuDataMapper = xs2aToSpiPsuDataMapper;
        this.spiContextDataProvider = spiContextDataProvider;
        this.scaApproachResolver = scaApproachResolver;
        this.paymentCancellationSpi = paymentCancellationSpi;
        this.spiErrorMapper = spiErrorMapper;
        this.pisCommonPaymentServiceEncrypted = pisCommonPaymentServiceEncrypted;
        this.spiToXs2aAuthenticationObjectMapper = spiToXs2aAuthenticationObjectMapper;
        this.xs2aPisCommonPaymentService = xs2aPisCommonPaymentService;
        this.pisPsuDataService = pisPsuDataService;
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse apply(Xs2aUpdatePisCommonPaymentPsuDataRequest request, GetPisAuthorisationResponse pisAuthorisationResponse) {
        return request.isUpdatePsuIdentification()
                   ? applyIdentification(request)
                   : applyAuthorisation(request, pisAuthorisationResponse);
    }

    private Xs2aUpdatePisCommonPaymentPsuDataResponse applyIdentification(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        if (!isPsuExist(request.getPsuData())) {
            ErrorHolder errorHolder = ErrorHolder.builder(MessageErrorCode.FORMAT_ERROR)
                                          .errorType(ErrorType.PIS_400)
                                          .messages(Collections.singletonList(MESSAGE_ERROR_NO_PSU))
                                          .build();
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder);
        }

        if (!isPsuDataCorrect(request.getPaymentId(), request.getPsuData())) {
            ErrorHolder errorHolder = ErrorHolder.builder(MessageErrorCode.PSU_CREDENTIALS_INVALID)
                                          .errorType(ErrorType.PIS_401)
                                          .build();
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder);
        }

        Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aUpdatePisCommonPaymentPsuDataResponse(PSUIDENTIFIED);
        response.setPsuId(request.getPsuData().getPsuId());
        return response;
    }

    private Xs2aUpdatePisCommonPaymentPsuDataResponse applyAuthorisation(Xs2aUpdatePisCommonPaymentPsuDataRequest request, GetPisAuthorisationResponse pisAuthorisationResponse) {
        PsuIdData psuData = extractPsuIdData(request);

        PaymentType paymentType = pisAuthorisationResponse.getPaymentType();
        String paymentProduct = pisAuthorisationResponse.getPaymentProduct();
        SpiPayment payment = mapToSpiPayment(pisAuthorisationResponse, paymentType, paymentProduct);

        AspspConsentData aspspConsentData = pisAspspDataService.getAspspConsentData(request.getPaymentId());

        SpiContextData spiContextData = spiContextDataProvider.provideWithPsuIdData(psuData);
        SpiPsuData spiPsuData = xs2aToSpiPsuDataMapper.mapToSpiPsuData(psuData);

        SpiResponse<SpiAuthorisationStatus> authPsuResponse = paymentCancellationSpi.authorisePsu(spiContextData, spiPsuData, request.getPassword(), payment, aspspConsentData);
        aspspConsentData = authPsuResponse.getAspspConsentData();
        pisAspspDataService.updateAspspConsentData(aspspConsentData);

        if (authPsuResponse.hasError()) {
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(authPsuResponse, ServiceType.PIS));
        }

        SpiResponse<List<SpiAuthenticationObject>> availableScaMethodsResponse = paymentCancellationSpi.requestAvailableScaMethods(contextData, payment, aspspConsentData);
        pisAspspDataService.updateAspspConsentData(availableScaMethodsResponse.getAspspConsentData());

        if (availableScaMethodsResponse.hasError()) {
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(availableScaMethodsResponse, ServiceType.PIS));
        }

        List<SpiAuthenticationObject> spiScaMethods = availableScaMethodsResponse.getPayload();

        if (CollectionUtils.isEmpty(spiScaMethods)) {
            SpiResponse<SpiResponse.VoidResponse> executePaymentResponse = paymentCancellationSpi.cancelPaymentWithoutSca(spiContextData, payment, availableScaMethodsResponse.getAspspConsentData());
            pisAspspDataService.updateAspspConsentData(executePaymentResponse.getAspspConsentData());

            if (executePaymentResponse.hasError()) {
                return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(executePaymentResponse, ServiceType.PIS));
            }

            pisCommonPaymentServiceEncrypted.updateCommonPaymentStatusById(request.getPaymentId(), TransactionStatus.RJCT);

            Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aUpdatePisCommonPaymentPsuDataResponse(FINALISED);
            response.setPsuId(spiPsuData.getPsuId());
            return response;

        } else if (isSingleScaMethod(spiScaMethods)) {
            xs2aPisCommonPaymentService.saveAuthenticationMethods(request.getAuthorisationId(), spiToXs2aAuthenticationObjectMapper.mapToXs2aListAuthenticationObject(spiScaMethods));
            SpiAuthenticationObject chosenMethod = spiScaMethods.get(0);

            if (chosenMethod.isDecoupled()) {
                scaApproachResolver.forceDecoupledScaApproach();
                return proceedDecoupledApproach(request, payment, chosenMethod, aspspConsentData);
            }

            return proceedSingleScaEmbeddedApproach(payment, chosenMethod, psuData, contextData, aspspConsentData);

        } else if (isMultipleScaMethods(spiScaMethods)) {
            xs2aPisCommonPaymentService.saveAuthenticationMethods(request.getAuthorisationId(), spiToXs2aAuthenticationObjectMapper.mapToXs2aListAuthenticationObject(spiScaMethods));
            Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aUpdatePisCommonPaymentPsuDataResponse(PSUAUTHENTICATED);
            response.setPsuId(psuData.getPsuId());
            response.setAvailableScaMethods(spiToXs2aAuthenticationObjectMapper.mapToXs2aListAuthenticationObject(spiScaMethods));
            return response;
        }
        return new Xs2aUpdatePisCommonPaymentPsuDataResponse(FAILED);
    }

    private Xs2aUpdatePisCommonPaymentPsuDataResponse proceedSingleScaEmbeddedApproach(SpiPayment payment, SpiAuthenticationObject chosenMethod, SpiPsuData psuData, SpiContextData contextData, AspspConsentData aspspConsentData) {
        SpiResponse<SpiAuthorizationCodeResult> authCodeResponse = paymentCancellationSpi.requestAuthorisationCode(contextData, chosenMethod.getAuthenticationMethodId(), payment, aspspConsentData);
        pisAspspDataService.updateAspspConsentData(authCodeResponse.getAspspConsentData());

        if (authCodeResponse.hasError()) {
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(authCodeResponse, ServiceType.PIS));
        }

        ChallengeData challengeData = mapToChallengeData(authCodeResponse.getPayload());

        Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aUpdatePisCommonPaymentPsuDataResponse(SCAMETHODSELECTED);
        response.setPsuId(psuData.getPsuId());
        response.setChosenScaMethod(spiToXs2aAuthenticationObjectMapper.mapToXs2aAuthenticationObject(chosenMethod));
        response.setChallengeData(challengeData);
        return response;
    }

    private Xs2aUpdatePisCommonPaymentPsuDataResponse proceedDecoupledApproach(Xs2aUpdatePisCommonPaymentPsuDataRequest request, SpiPayment payment, SpiAuthenticationObject chosenMethod, AspspConsentData aspspConsentData) {
        SpiResponse<SpiAuthorisationDecoupledScaResponse> spiResponse = paymentCancellationSpi.startScaDecoupled(spiContextDataProvider.provideWithPsuIdData(request.getPsuData()), request.getAuthorisationId(), chosenMethod.getAuthenticationMethodId(), payment, aspspConsentData);
        pisAspspDataService.updateAspspConsentData(spiResponse.getAspspConsentData());

        if (spiResponse.hasError()) {
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS));
        }

        Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aDecoupledUpdatePisCommonPaymentPsuDataResponse(SCAMETHODSELECTED);
        response.setPsuMessage(spiResponse.getPayload().getPsuMessage());
        response.setChosenScaMethod(buildXs2aAuthenticationObjectForDecoupledApproach(chosenMethod.getAuthenticationMethodId()));
        return response;
    }

    private ChallengeData mapToChallengeData(SpiAuthorizationCodeResult authorizationCodeResult) {
        if (authorizationCodeResult != null && !authorizationCodeResult.isEmpty()) {
            return authorizationCodeResult.getChallengeData();
        }
        return null;
    }

    private boolean isSingleScaMethod(List<SpiAuthenticationObject> spiScaMethods) {
        return spiScaMethods.size() == 1;
    }

    private boolean isMultipleScaMethods(List<SpiAuthenticationObject> spiScaMethods) {
        return spiScaMethods.size() > 1;
    }

    private PsuIdData extractPsuIdData(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        PsuIdData psuDataInRequest = request.getPsuData();
        if (isPsuExist(psuDataInRequest)) {
            return psuDataInRequest;
        }

        return pisCommonPaymentServiceEncrypted.getPisCancellationAuthorisationById(request.getAuthorisationId())
                   .map(GetPisAuthorisationResponse::getPsuId)
                   .filter(StringUtils::isNotBlank)
                   .map(id -> new PsuIdData(id, null, null, null))
                   .orElse(psuDataInRequest);
    }

    private boolean isPsuExist(PsuIdData psuIdData) {
        return Optional.ofNullable(psuIdData)
                   .map(PsuIdData::isNotEmpty)
                   .orElse(false);
    }

    private boolean isPsuDataCorrect(String paymentId, PsuIdData psuData) {
        List<PsuIdData> psuIdDataList = pisPsuDataService.getPsuDataByPaymentId(paymentId);

        return psuIdDataList.stream()
                   .anyMatch(psu -> psu.contentEquals(psuData));
    }

    // Should ONLY be used for switching from Embedded to Decoupled approach during SCA method selection
    private Xs2aAuthenticationObject buildXs2aAuthenticationObjectForDecoupledApproach(String authenticationMethodId) {
        Xs2aAuthenticationObject xs2aAuthenticationObject = new Xs2aAuthenticationObject();
        xs2aAuthenticationObject.setAuthenticationMethodId(authenticationMethodId);
        return xs2aAuthenticationObject;
    }
}

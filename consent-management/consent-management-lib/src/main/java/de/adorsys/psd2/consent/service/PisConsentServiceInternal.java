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

import de.adorsys.psd2.consent.api.CmsAspspConsentDataBase64;
import de.adorsys.psd2.consent.api.CmsAuthorisationType;
import de.adorsys.psd2.consent.api.pis.authorisation.CreatePisConsentAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.authorisation.GetPisConsentAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.authorisation.UpdatePisConsentPsuDataRequest;
import de.adorsys.psd2.consent.api.pis.authorisation.UpdatePisConsentPsuDataResponse;
import de.adorsys.psd2.consent.api.pis.proto.CreatePisConsentResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisConsentRequest;
import de.adorsys.psd2.consent.api.pis.proto.PisConsentResponse;
import de.adorsys.psd2.consent.api.service.PisConsentService;
import de.adorsys.psd2.consent.domain.AspspConsentDataEntity;
import de.adorsys.psd2.consent.domain.payment.PisConsent;
import de.adorsys.psd2.consent.domain.payment.PisConsentAuthorization;
import de.adorsys.psd2.consent.domain.payment.PisPaymentData;
import de.adorsys.psd2.consent.repository.AspspConsentDataRepository;
import de.adorsys.psd2.consent.repository.PisConsentAuthorizationRepository;
import de.adorsys.psd2.consent.repository.PisConsentRepository;
import de.adorsys.psd2.consent.repository.PisPaymentDataRepository;
import de.adorsys.psd2.consent.service.mapper.PisConsentMapper;
import de.adorsys.psd2.consent.service.mapper.PsuDataMapper;
import de.adorsys.psd2.consent.service.security.DecryptedData;
import de.adorsys.psd2.consent.service.security.EncryptedData;
import de.adorsys.psd2.consent.service.security.SecurityDataService;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static de.adorsys.psd2.xs2a.core.consent.ConsentStatus.RECEIVED;
import static de.adorsys.psd2.xs2a.core.consent.ConsentStatus.VALID;
import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.SCAMETHODSELECTED;
import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.STARTED;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PisConsentServiceInternal implements PisConsentService {
    private final PisConsentRepository pisConsentRepository;
    private final PisConsentMapper pisConsentMapper;
    private final PsuDataMapper psuDataMapper;
    private final PisConsentAuthorizationRepository pisConsentAuthorizationRepository;
    private final PisPaymentDataRepository pisPaymentDataRepository;
    private final SecurityDataService securityDataService;
    private final AspspConsentDataRepository aspspConsentDataRepository;

    /**
     * Creates new pis consent with full information about payment
     *
     * @param request Consists information about payments.
     * @return Response containing identifier of consent
     */
    @Override
    @Transactional
    public Optional<CreatePisConsentResponse> createPaymentConsent(PisConsentRequest request) {
        PisConsent consent = pisConsentMapper.mapToPisConsent(request);
        String externalId = UUID.randomUUID().toString();
        consent.setExternalId(externalId);

        PisConsent saved = pisConsentRepository.save(consent);

        return Optional.ofNullable(saved.getId())
                   .flatMap(id -> securityDataService.encryptId(saved.getExternalId()))
                   .map(CreatePisConsentResponse::new);
    }

    /**
     * Retrieves consent status from pis consent by consent identifier
     *
     * @param encryptedConsentId String representation of pis consent identifier
     * @return Information about the status of a consent
     */
    @Override
    public Optional<ConsentStatus> getConsentStatusById(String encryptedConsentId) {
        return getPisConsentById(encryptedConsentId)
                   .map(PisConsent::getConsentStatus);
    }

    /**
     * Reads full information of pis consent by consent identifier
     *
     * @param encryptedConsentId String representation of pis encrypted consent identifier
     * @return Response containing full information about pis consent
     */
    @Override
    public Optional<PisConsentResponse> getConsentById(String encryptedConsentId) {
        return getPisConsentById(encryptedConsentId)
                   .flatMap(pisConsentMapper::mapToPisConsentResponse);
    }

    /**
     * Updates pis consent status by consent identifier
     *
     * @param encryptedConsentId String representation of pis encrypted consent identifier
     * @param status             new consent status
     * @return Response containing result of status changing
     */
    @Override
    @Transactional
    public Optional<Boolean> updateConsentStatusById(String encryptedConsentId, ConsentStatus status) {
        return getActualPisConsent(encryptedConsentId)
                   .map(con -> setStatusAndSaveConsent(con, status))
                   .map(con -> con.getConsentStatus() == status);
    }

    /**
     * Get Pis aspsp consent data by consent id
     *
     * @param encryptedConsentId id of the consent
     * @return Response containing aspsp consent data
     */
    @Override
    public Optional<CmsAspspConsentDataBase64> getAspspConsentDataByConsentId(String encryptedConsentId) {
        return getPisConsentById(encryptedConsentId)
                   .map(dta -> prepareAspspConsentData(dta, encryptedConsentId));
    }

    /**
     * Get Pis aspsp consent data by payment id
     *
     * @param encryptedPaymentId encrypted id of the payment
     * @return Response containing aspsp consent data
     */
    @Override
    public Optional<CmsAspspConsentDataBase64> getAspspConsentDataByPaymentId(String encryptedPaymentId) {
        Optional<String> paymentId = securityDataService.decryptId(encryptedPaymentId);
        if (!paymentId.isPresent()) {
            log.warn("Payment Id has not encrypted: {}", encryptedPaymentId);
            return Optional.empty();
        }
        return pisPaymentDataRepository.findByPaymentId(paymentId.get())
                   .map(dta -> dta.get(0))
                   .map(PisPaymentData::getConsent)
                   .map(dta -> prepareAspspConsentData(dta, encryptedPaymentId));
    }

    private CmsAspspConsentDataBase64 prepareAspspConsentData(PisConsent consent, String encryptedConsentId) {
        return aspspConsentDataRepository.findByConsentId(consent.getExternalId())
                   .map(AspspConsentDataEntity::getData)
                   .flatMap(dta -> securityDataService.decryptConsentData(encryptedConsentId, dta))
                   .map(DecryptedData::getData)
                   .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                   .map(str64 -> new CmsAspspConsentDataBase64(encryptedConsentId, str64))
                   .orElseGet(() -> new CmsAspspConsentDataBase64(encryptedConsentId, null));
    }

    /**
     * Get original decrypted Id from encrypted string
     *
     * @param encryptedId id to be decrypted
     * @return Response containing original decrypted Id
     */
    @Override
    public Optional<String> getDecryptedId(String encryptedId) {
        return securityDataService.decryptId(encryptedId);
    }

    /**
     * Update PIS consent aspsp consent data by id
     *
     * @param request            Aspsp provided pis consent data
     * @param encryptedConsentId id of the consent to be updated
     * @return String consent id
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<String> updateAspspConsentDataInPisConsent(String encryptedConsentId, CmsAspspConsentDataBase64 request) {
        Optional<PisConsent> consent = getActualPisConsent(encryptedConsentId);
        if(!consent.isPresent()) {
            return Optional.empty();
        }

        Optional<EncryptedData> encryptedConsentData = securityDataService.encryptConsentData(encryptedConsentId, request.getAspspConsentDataBase64());
        if (!encryptedConsentData.isPresent()) {
            return Optional.empty();
        }

        updateConsentData(consent.get().getExternalId(), encryptedConsentData.get().getData());
        return Optional.of(encryptedConsentId);
    }

    /**
     * Create consent authorization
     *
     * @param encryptedPaymentId encrypted id of the payment
     * @param authorizationType  type of authorization required to create. Can be  CREATED or CANCELLED
     * @return response contains authorization id
     */
    @Override
    @Transactional
    public Optional<CreatePisConsentAuthorisationResponse> createAuthorization(String encryptedPaymentId, CmsAuthorisationType authorizationType, PsuIdData psuData) {
        Optional<String> paymentId = securityDataService.decryptId(encryptedPaymentId);
        if (!paymentId.isPresent()) {
            log.warn("Payment Id has not encrypted: {}", encryptedPaymentId);
            return Optional.empty();
        }

        return pisPaymentDataRepository.findByPaymentIdAndConsent_ConsentStatus(paymentId.get(), RECEIVED)
                   .map(list -> saveNewAuthorization(list.get(0).getConsent(), authorizationType, psuData))
                   .map(c -> new CreatePisConsentAuthorisationResponse(c.getExternalId()));
    }

    @Override
    @Transactional
    public Optional<CreatePisConsentAuthorisationResponse> createAuthorizationCancellation(String paymentId, CmsAuthorisationType authorizationType, PsuIdData psuData) {
        return createAuthorization(paymentId, authorizationType, psuData);
    }

    /**
     * Update consent authorization
     *
     * @param authorizationId   id of the authorization to be updated
     * @param request           contains data for updating authorization
     * @param authorizationType type of authorization required to update. Can be  CREATED or CANCELLED
     * @return response contains updated data
     */
    @Override
    @Transactional
    public Optional<UpdatePisConsentPsuDataResponse> updateConsentAuthorization(String authorizationId, UpdatePisConsentPsuDataRequest request, CmsAuthorisationType authorizationType) {
        Optional<PisConsentAuthorization> pisConsentAuthorisationOptional = pisConsentAuthorizationRepository.findByExternalIdAndAuthorizationType(
            authorizationId, authorizationType);
        if (pisConsentAuthorisationOptional.isPresent()) {
            PisConsentAuthorization consentAuthorization = pisConsentAuthorisationOptional.get();

            if (SCAMETHODSELECTED == request.getScaStatus()) {
                String chosenMethod = request.getAuthenticationMethodId();
                if (StringUtils.isNotBlank(chosenMethod)) {
                    consentAuthorization.setChosenScaMethod(chosenMethod);
                }
            }
            consentAuthorization.setScaStatus(request.getScaStatus());
            pisConsentAuthorizationRepository.save(consentAuthorization);
        }
        return pisConsentAuthorisationOptional.map(p -> new UpdatePisConsentPsuDataResponse(p.getScaStatus()));
    }

    /**
     * Update PIS consent payment data and stores it into database
     *
     * @param request            PIS consent request for update payment data
     * @param encryptedConsentId encrypted Consent ID
     */
    // TODO return correct error code in case consent was not found https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/408
    @Override
    @Transactional
    public void updatePaymentConsent(PisConsentRequest request, String encryptedConsentId) {
        Optional<PisConsent> pisConsentById = getPisConsentById(encryptedConsentId);
        pisConsentById
            .ifPresent(pisConsent -> pisPaymentDataRepository.save(pisConsentMapper.mapToPisPaymentDataList(request.getPayments(), pisConsent)));
    }

    /**
     * Reads authorization data by authorization Id
     *
     * @param authorizationId   id of the authorization to be updated
     * @param authorizationType type of authorization. Can be  CREATED or CANCELLED
     * @return response contains authorization data
     */
    @Override
    public Optional<GetPisConsentAuthorisationResponse> getPisConsentAuthorizationById(String authorizationId, CmsAuthorisationType authorizationType) {
        return pisConsentAuthorizationRepository.findByExternalIdAndAuthorizationType(authorizationId, authorizationType)
                   .map(pisConsentMapper::mapToGetPisConsentAuthorizationResponse);
    }

    /**
     * Reads authorization id data by encrypted payment Id and type of authorization
     *
     * @param encryptedPaymentId encrypted id of the payment
     * @param authorizationType  type of authorization required to create. Can be  CREATED or CANCELLED
     * @return response contains authorization id
     */
    @Override
    public Optional<String> getAuthorisationByPaymentId(String encryptedPaymentId, CmsAuthorisationType authorizationType) {
        Optional<String> paymentId = securityDataService.decryptId(encryptedPaymentId);
        if (!paymentId.isPresent()) {
            log.warn("Payment Id has not encrypted: {}", encryptedPaymentId);
            return Optional.empty();
        }

        return pisPaymentDataRepository.findByPaymentIdAndConsent_ConsentStatus(paymentId.get(), RECEIVED)
                   .flatMap(list -> pisConsentAuthorizationRepository.findByConsentIdAndAuthorizationType(list.get(0).getConsent().getId(), authorizationType))
                   .filter(CollectionUtils::isNotEmpty)
                   .map(lst -> lst.get(0).getExternalId());
    }

    /**
     * Reads Psu data by encrypted payment Id
     *
     * @param encryptedPaymentId encrypted id of the payment
     * @return response contains data of Psu
     */
    @Override
    public Optional<PsuIdData> getPsuDataByPaymentId(String encryptedPaymentId) {
        Optional<String> paymentId = securityDataService.decryptId(encryptedPaymentId);
        if (!paymentId.isPresent()) {
            log.warn("Payment Id has not encrypted: {}", encryptedPaymentId);
            return Optional.empty();
        }

        return pisPaymentDataRepository.findByPaymentId(paymentId.get())
                   .map(l -> l.get(0))
                   .map(PisPaymentData::getConsent)
                   .map(pc -> psuDataMapper.mapToPsuIdData(pc.getPsuData()));
    }

    /**
     * Reads Psu data by encrypted consent Id
     *
     * @param encryptedConsentId encrypted Consent ID
     * @return response contains data of Psu
     */
    @Override
    public Optional<PsuIdData> getPsuDataByConsentId(String encryptedConsentId) {
        return getPisConsentById(encryptedConsentId)
                   .map(pc -> psuDataMapper.mapToPsuIdData(pc.getPsuData()));
    }

    private Optional<PisConsent> getActualPisConsent(String encryptedConsentId) {
        Optional<String> consentIdDecrypted = securityDataService.decryptId(encryptedConsentId);
        if (!consentIdDecrypted.isPresent()) {
            log.warn("Consent Id has not encrypted: {}", encryptedConsentId);
        }

        return consentIdDecrypted
                   .flatMap(id -> pisConsentRepository.findByExternalIdAndConsentStatusIn(id, EnumSet.of(RECEIVED, VALID)));
    }

    private Optional<PisConsent> getPisConsentById(String encryptedConsentId) {
        Optional<String> consentIdDecrypted = securityDataService.decryptId(encryptedConsentId);
        if (!consentIdDecrypted.isPresent()) {
            log.warn("Consent Id has not encrypted: {}", encryptedConsentId);
        }

        return consentIdDecrypted
                   .flatMap(pisConsentRepository::findByExternalId);
    }

    private PisConsent setStatusAndSaveConsent(PisConsent consent, ConsentStatus status) {
        consent.setConsentStatus(status);
        return pisConsentRepository.save(consent);
    }

    /**
     * Creates PIS consent authorization entity and stores it into database
     *
     * @param pisConsent PIS Consent, for which authorization is performed
     * @return PisConsentAuthorization
     */
    private PisConsentAuthorization saveNewAuthorization(PisConsent pisConsent, CmsAuthorisationType authorizationType, PsuIdData psuData) {
        PisConsentAuthorization consentAuthorization = new PisConsentAuthorization();
        consentAuthorization.setExternalId(UUID.randomUUID().toString());
        consentAuthorization.setConsent(pisConsent);
        consentAuthorization.setScaStatus(STARTED);
        consentAuthorization.setAuthorizationType(authorizationType);
        consentAuthorization.setPsuData(psuDataMapper.mapToPsuData(psuData));
        return pisConsentAuthorizationRepository.save(consentAuthorization);
    }

    private AspspConsentDataEntity updateConsentData(String externalId, byte[] consentData) {
        AspspConsentDataEntity aspspConsentDataEntity = aspspConsentDataRepository
                                                            .findByConsentId(externalId)
                                                            .orElseGet(() -> new AspspConsentDataEntity(externalId));
        aspspConsentDataEntity.setData(consentData);
        return aspspConsentDataRepository.save(aspspConsentDataEntity);
    }
}

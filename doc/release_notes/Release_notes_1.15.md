# Release notes v. 1.15

## Get SCA Status Request
Endpoints for getting the SCA status of the authorisation were implemented.
Available endpoints are listed below.

| Context                             | Method | Endpoint                                                                        | Description                                                         |
|-------------------------------------|--------|---------------------------------------------------------------------------------|---------------------------------------------------------------------|
| Payment Initiation Request          | GET    | /v1/{payment-service}/{paymentId}/authorisations/{authorisationId}              | Checks the SCA status of a authorisation sub-resource.              |
| Payment Cancellation Request        | GET    | /v1/{payment-service}/{paymentId}/cancellation- authorisations/{cancellationId} | Checks the SCA status of a cancellation authorisation sub-resource. |
| Account Information Consent Request | GET    | /v1/consents/{consentId}/authorisations/{authorisationId}                       | Checks the SCA status of a authorisation sub-resource.              |

## TPP-Nok-Redirect-URI returned when scaRedirect URI is expired (for AIS)
Now for AIS if scaRedirect URI is expired we deliver TPP-Nok-Redirect-URI in the response from CMS to Online-banking. This response is returned with code 408.
If TPP-Nok-Redirect-URI was not sent from TPP and in CMS is stored null, then CMS returns empty response with code 408. If payment is not found or psu data is incorrect, CMS returns 404. 

## One active authorisation per payment for one PSU
When PSU creates new authorisation for a payment, all previous authorisations, created by this PSU for the same payment, will be failed and expired.

## Bugfix: validate PSU credentials during update PSU data requests
From now on SPI response status UNAUTHORIZED_FAILURE corresponds to PSU_CREDENTIALS_INVALID error(response code HTTP 401).

Now SPI-Mock correctly handles invalid PSU credentials.

## Bugfix: method encryptConsentData in SecurityDataService takes byte array as an argument
Now to encrypt aspspConsentData in SecurityDataService we should provide byte array as an argument instead of Base64 encoded string

## Add instanceId to services in cms-aspsp-api and cms-psu-api
From now methods in cms-aspsp-api and cms-psu-api also require instanceId to be provided as a mandatory argument.
This id represents particular service instance and is used for filtering data from the database.

All corresponding CMS endpoints were also updated and from now on support instanceId as an optional header. 
If the header isn't provided, default value `UNDEFINED` will be used instead.

The following services were affected by this change:
  - In consent-aspsp-api:
    - de.adorsys.psd2.consent.aspsp.api.piis.CmsAspspPiisService
  - In consent-psu-api:
    - de.adorsys.psd2.consent.psu.api.CmsPsuAisService
    - de.adorsys.psd2.consent.psu.api.CmsPsuPiisService
    - de.adorsys.psd2.consent.psu.api.CmsPsuPisService

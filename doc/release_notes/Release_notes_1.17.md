# Release notes v. 1.17

## Bugfix: not able to retrieve the payment by redirect id when psu-id is not set in the initial request
When retrieving the payment by redirect id (endpoint GET /psu-api/v1/pis/consent/redirects/{redirect-id} in consent-management) now there is no need
to provide psu data and the payment can be retrieved without it, only by redirect id and instance id.

This also applies for getting payment for cancellation (endpoint GET /psu-api/v1/payment/cancellation/redirect/{redirect-id} in consent-management)

## Bugfix: Remove PSU Data from endpoint for getting consent by redirect id in CMS-PSU-API
PSU Data is no longer required for getting consent by redirect id.
As a result, headers `psu-id`, `psu-id-type`, `psu-corporate-id` and `psu-corporate-id-type` are no longer used in `psu-api/v1/ais/consent/redirect/{redirect-id}` endpoint.
PsuIdData was also removed as an argument from `de.adorsys.psd2.consent.psu.api.CmsPsuAisService#checkRedirectAndGetConsent` method.

## Delete old 'tpp demo' 
Deleted extra functional in 'tpp-demo' package

## Support of multiple SCA approaches
ASPSP can support several SCA approaches, so now XS2A Interface supports multiple SCA approaches also.
ASPSP profile was extended with list of approaches (in order of priority - first one with the highest priority) instead of one single approach.

| Option                                   | Meaning                                                                                                | Default value | 
|------------------------------------------|--------------------------------------------------------------------------------------------------------|---------------|
| scaApproaches                            | List of SCA Approach supported by ASPSP ordered by priority                                            |  - REDIRECT   |
|                                          |                                                                                                        |  - EMBEDDED   |
|                                          |                                                                                                        |  - DECOUPLED  |

Choice of SCA approaches depends on header parameter in payment,consent or signing basket initiation request - `TPP-Redirect-Preferred`. For Payment Cancellation parameter "TPP-Redirect-Preferred" is not used.
If `TPP-Redirect-Preferred` is true and ASPSP supports REDIRECT approach, then `REDIRECT` approach is used. Otherwise first approach in ASPSP-profile option `scaApproaches` is used.

## Bugfix: remove discrepancies between not null constraints in migration files and constraints in Java classes
There were some discrepancies between not null constraints specified in liquibase migration files and constraints specified in Java classes that were removed.
Now tables created via generated DDL should have the same constraints as the tables created via liquibase.

Attention: if you've bypassed the CMS and inserted some records into `pis_payment_data` table with `common_payment_id` 
column set to `null` directly, you'll have to assign some value to this column before updating the database.

## Provide endpoint to export AIS consents by aspspAccountId from CMS to ASPSP
By accessing endpoint GET `/aspsp-api/v1/ais/consents/account/{account-id}` (or corresponding method in `CmsAspspAisExportService.java`) 
one can export AIS Consents that contain certain account id. Method `exportConsentsByAccountId` now implemented and working.

## Implemented java interface and endpoint to save Account Access object in Consent by Online banking
Now by accessing `/psu-api/v1/ais/consent/{consent-id}/save-access` endpoint(or corresponding method in `CmsPsuAisService.java`)
online banking can update AccountAccess (along with `aspspAccountId` and `resourceId` if necessary), frequency per day and expiration date in consent.

## Bugfix: Additional payment products work only with "pain-" prefix

From now on SPI Developer may define any payment product they would like to support.
Only four payment products are considered as "standard" and processed in XS2A with the standard flow:
* sepa-credit-transfers
* instant-sepa-credit-transfers
* target-2-payments
* cross-border-credit-transfers

Any other payment product will be processed as byte-array to SPI level. SPI Developer needs to handle the payload
in SPI Connector.
Right now only content types `application/json` and `application/xml` are supported (support of `multipart/form-data`
is provided additionally to setup PAIN periodic payments).
Support of content type `text/plain` is planned.

## Bugfix: added missing ais_consent fields
Now while sending POST request to the `/v1/consents` endpoint the fields "availableAccounts" and "allPsd2" are persisted to the ais_consent table. Also they are available while calling the connector.

## Added support of Spring Data 2.x
In order to use Spring Data 2.x in CMS developer now shall use a dependency to help-module `spring-boot-2.x-support`
```xml
        <dependency>
            <groupId>de.adorsys.psd2</groupId>
            <artifactId>spring-boot-2.x-support</artifactId>
            <version>1.17-SNAPSHOT</version>
        </dependency>

```
By default `spring-boot-1.5.x-support` is used.

## Bugfix: removed duplicated links in xs2a responses
Previously in xs2a responses we had two blocks of links with the same content, but different namings (`links` and `_links`).
Now xs2a interface provides only one block of links.

## Implemented Decoupled SCA approach
From now on XS2A supports Decoupled SCA approach for authorising account consents, payments and payment cancellations.
It occurs in the following cases:
 * if `DECOUPLED` SCA approach was chosen by ASPSP
 * during the `EMBEDDED` SCA approach if decoupled SCA method was chosen by PSU during selection of SCA methods

New method `de.adorsys.psd2.xs2a.spi.service.AuthorisationSpi#startScaDecoupled` was added to the SPI interface to be implemented by SPI developers.
The response of this method should contain the message, shown to the PSU, containing recommendation to proceed the authorisation via the dedicated mobile app.

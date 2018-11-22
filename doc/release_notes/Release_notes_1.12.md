# Release notes v. 1.12

## Extract CMS web-endpoints to different maven modules
A SPI Developer now can decide what endpoints on CMS side he needs.
There are three modules with different endpoints to serve different possible purposes of CMS.
* consent-xs2a-web is used to provide endpoints for xs2a-service. 
This is normally needed if CMS is deployed as a separate service.
* consent-psu-web is used to provide endpoints for presentation and work with consents in some PSU application, 
i.e. online-banking system. This module can be used in any setup of CMS (embedded and standalone).
* consent-aspsp-web is used to provide other endpoints available for banking systems.
This module can be used in any setup of CMS (embedded and standalone).

## Payment cancellation is added
A PSU may want to cancel the payment. The flow of payment cancellation for embedded and redirect approaches was added.
SPI Developer needs to implement PaymentCancellationSpi in order to support it.

## Return full SCA Object from SPI during request to send TAN
By invoking `requestAuthorisationCode` method SPI developer now obliged
to return full SCA Authentification Object for choosen SCA Method.
This is done to avoid unnecessary additional call to list all available methods second time.
`SpiAuthorizationCodeResult` now adjusted accordingly.

## Improved nullability checks in SpiResponse
Due to importance of having not-nullable results in SPI API nullability checks by SpiResponse object construction were added.
Please note, that this also implies, if you need to return VoidResponse.
Please use SpiResponse.voidResponse() static method for that.

## Support Oracle DB
Migrations scripts were modified in order to fix oracle supporting issues. You may be required to regenerate local schemes.

## Log TPP requests and responses
Now all the requests and responses from TPP to XS2A are logged.
Logging flow is configured in logback-spring.xml file. By default, all such logs are written to console only. 
It is possible to change this behaviour by uncommenting the lines inside logback-spring.xml (see the hints inside this logback file).

If there is a need to rewrite logging configurations, the following should be done:
* logback file (logback.xml or logback.groovy (if Groovy is on the classpath)) should be created inside the project. 

Please, use only mentioned names for logback files to make rewriting configs work.

## New Java Interface for locking TPP Access provided in CMS-ASPSP-API
With the de.adorsys.psd2.consent.aspsp.api.tpp.CmsAspspTppService ASPSP can implement a functionality of locking TPP by a certain
time or forever or unlocking it. This may be required to provide temporary solutions to block requests from inappropriately
behaving TPP before its certificate will be revoked.
Functional implementation for this Java interface is planned to provided in the upcoming weeks. See [Roadmap](../roadmap.md)

## Create one endpoint in CMS for working with AspspConsentData
* Supported all types of services: AIS, PIS, PIIS
* Added ability to delete AspspConsentData

| Method | Path                                           | Description                                              |
|--------|------------------------------------------------|----------------------------------------------------------|
| GET    | api/v1/aspsp-consent-data/consents/{consent-id} | Get aspsp consent data identified by given consent id    |
| GET    | api/v1/aspsp-consent-data/payments/{payment-id} | Get aspsp consent data identified by given payment id    |
| PUT    | api/v1/aspsp-consent-data/consents/{consent-id} | Update aspsp consent data identified by given consent id |
| DELETE | api/v1/aspsp-consent-data/consents/{consent-id} | Delete aspsp consent data identified by given consent id |

Next old endpoints are not supported:
* api/v1/ais/consent/{consent-id}/aspsp-consent-data
* api/v1/pis/payment/{payment-id}/aspsp-consent-data
* api/v1/pis/consent/{consent-id}/aspsp-consent-data

Developer should change them according to the information in the table above.

## New Java Interfaces for exporting consents and payments provided in CMS-ASPSP-API
With the de.adorsys.psd2.consent.aspsp.api.ais.CmsAspspAisExportService and
de.adorsys.psd2.consent.aspsp.api.pis.CmsAspspPisExportService ASPSP can implement a functionality of exporting AIS Consents
and Payments by certain TPP or PSU and certain period of time.
Functional implementation for this Java interface is planned to provided in the upcoming weeks. See [Roadmap](../roadmap.md)

## New Java interface and CMS endpoints for creating, terminating and retrieving PIIS consents in CMS-ASPSP-API
With the de.adorsys.psd2.consent.aspsp.api.service.CmsAspspPiisService ASPSP can now create new PIIS consents,
terminate them by their Ids and retrieve them by PSU Id.
Appropriate endpoints were added to the CMS in consent-aspsp-web module.

## Record TPP requests
From now on all TPP request are recorded to the CMS database. Requests are recorder and stored as events,
with event type that corresponds to specific request.
Recorded event contains:
 * event timestamp, indicating when the event has occurred
 * consent id or payment id if it they were contained in the request
 * origin and type of the event
 * payload with: information about the TPP, TPP's ip address, unique identifier of the request, request URI,
 request headers and body
 
Added Java interface de.adorsys.psd2.consent.aspsp.api.CmsAspspEventService that allows ASPSP to retrieve list
of events by the requested. Appropriate endpoints were added to the CMS in consent-aspsp-web module.
 
Developers should apply new liquibase migration scripts in order to update the database.

# Set finalised statuses
For now there is no possibility to update statuses of Autorisation, Payment Transaction, Consent if they are marked as "finalised".

Finalised statuses are:

* for Authorisation (SCA) status - *Finalised, Failed*;
* for Payment Transaction status: *Cancelled, Rejected, AcceptedSettlementCompleted*;
* for Consent status: *Rejected, RevokedByPSU, Expired, TerminatedByTpp*.

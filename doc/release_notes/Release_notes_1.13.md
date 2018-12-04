# Release notes v. 1.13

## Add parameter "deltaReportSupported" to ASPSP-profile
Now we can set parameter for delta-report support in ASPSP-Profile.

| Option                                  | Meaning                                                                                             | Default value                                        | Possible values                                                                                      |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------|------------------------------------------------------------------------------------------------------|
|deltaReportSupported                     | This field indicates if an ASPSP supports Delta reports for transaction details                     | false                                                | true, false                                                                                          |

## Provide XS2A Swagger as an option
Now Swagger is not enabled for XS2A Interface by default.
To enable swagger in xs2a you have to add `@EnableXs2aSwagger` annotation on any of Spring configuration classes / Spring boot Application class in your application. To disable swagger just remove it.
You could also put PSD2 API yaml file to the resource folder of your connector to override default PSD2 API. To do that you need to fill in 
`xs2a.swagger.psd2.api.location` property in your application.properties file. I.e.
`xs2a.swagger.psd.api.location=path/in/my/classpath/my_swagger_api.yml`

## Provide CMS Swagger as an option
Now Swagger is not enabled for CMS Interface by default.
To enable swagger in cms you have to add `@EnableCmsSwagger` annotation on any of Spring configuration classes / Spring boot Application class in your CMS application. To disable swagger just remove it.

## PaymentProduct entity was replaced by raw String value
Now instead of using PaymentProduct enum class, string value is used. PaymentProduct enum class is removed.
In database, instead of saving enum values(SEPA, INSTANT_SEPA, etc), raw string values are saved:  sepa-credit-transfers, instant-sepa-credit-transfers, etc.

## Get authorisation sub-resources is implemented
| Context                             | Method | Endpoint                                        | Description                                                                                     |
|-------------------------------------|--------|-------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Payment Initiation Request          | GET    | v1/{payment-service}/{paymentId}/authorisations | Will deliver an array of resource identifications of all generated authorisation sub-resources. |
| Account Information Consent Request | GET    | v1/consents/{consentId}/authorisations          | Will deliver an array of resource identifications of all generated authorisation sub-resources. |

## Store TppInfo in AIS Consent
Now AIS Consent contains TppInfo object instead of TPP Id.
Developers should apply new liquibase migration scripts in order to update the database.

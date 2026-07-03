# stats-collector
# Overview

Will process a scheduled job to generate and collect information from other services.

There is a delay between generation and collection of stats because the service being asked to generate the data may take longer than a standard request timeout to generate the data.

The results will be published to Splunk as an event with the following:
  * auditSource set to "stats-generator" during the genration phase and "stats-collector" during the collection phase
  * transactionName set to the name of the configured job
  * auditType set to the audit-type from job config.
  * details set to the response from the called service


Obviously, as a user of this service you will either need to store the results of the generation phase or implement only a collection endpoint on it's own.

You need to add a service configuration for your service e.g:
```
      my-service {
        host = localhost
        port = 8500
      }
```

To configure the service to generate stats for your service, add a job to the list within the stats-generator config, e.g:

```javascript
stats-generator {
  schedule = "02:30",
  jobs = {
    splunk-transaction-name = {
      audit-type = "splunk-reference"
      service = my-service
      api = "/my/generate/endpoint"
    }
  }
}
```

To configure the service to collect stats for your service, add a job to the list within the stats-collector config, e.g:

```javascript
stats-collector {
  schedule = "03:00",
  jobs = {
     job-name = {
      audit-type = "splunk-reference"
      service = my-service
      api = "/my/collection/endpoint"
    }
  }
}
```

## Splunk event format

```javascript
{  
   "auditSource":"stats-collector",
   "eventId": [Unique generated id],
   "tags":{  
      "transactionName": [from stats-generator.jobs.{job-name} configuration property]
   },
   "detail":{  
      [json from stats collect call on service]
   },
   "generatedAt": "YYYY-MM-DDThh:mm:ss.zzz"
}
```

## Current statistic service calls


### Preferences

Collects statistics on number of paperless users and their current contactability

Details: https://github.com/HMRC/preferences/blob/master/stats.md

## Run the tests and sbt fmt before raising a PR

Ensure you have service-manager python environment setup:

`source ../servicemanager/bin/activate`

Format:

`sbt fmt`

Then run the tests and coverage report:

`sbt clean coverage test coverageReport`

If your build fails due to poor testing coverage, *DO NOT* lower the test coverage, instead inspect the generated report located here on your local repo: `/target/scala-2.12/scoverage-report/index.html`

Then run the integration tests:

`sm2 --start DC_STATS_COLLECTOR_IT`
`sbt it:test`
`sm2 --stop DC_STATS_COLLECTOR_IT`

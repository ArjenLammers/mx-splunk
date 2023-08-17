# Mendix Splunk Module

## Description
This is a Mendix Module that redirects the log of the Mendix application to Splunk.
It is intended for those who need more customization or control than the [Platform Splunk Support](https://docs.mendix.com/developerportal/operate/splunk-metrics/) which is part of the Mendix Cloud container.
It uses [this library](https://github.com/splunk/splunk-library-javalogging) as underlying implementation.

## Features
Adds the following metadata by default:
- `instance_index` (the index of the application's instance in the cluster)
- `environment_id` (like noted in the Cloud Portal)
- `application_name` (application name as respected from your the first part of the URL of the app)
- `runtime_version` (version of the Mendix Runtime used)
- `model_version` (version of the model running determined during build / part of the deployed archive)

Tags added to the application (see Cloud Portal / Environment / Tags) are also added.

## Installation
Connect the `Splunk.AfterStartup` microflow to your application's after startup logic.

## Configuration
Set the following constants:
- `Enabled` - Does the obvious. If true, the module will do its work.
- `Endpoint` - The endpoint which should be passed to the library. Note not to include a trailing / (so https://mycustomer/MENDIX is fine)
- `MinimalLogLevel` - The minimal level of messages to forward to splunk. One of NONE | CRITICAL | ERROR | WARNING | INFO | DEBUG | TRACE 
- `Token` - Token provided by Splunk.

## Common issues
The Splunk module is aware of logs created by itself, so a loop won't occur :)

### Incorrect URL
```
2023-08-08T07:51:22.590866 [APP/PROC/WEB/0]    ERROR - Splunk: Error while sending message to Splunk.
2023-08-08T07:51:22.590903 [APP/PROC/WEB/0]   com.splunk.logging.HttpEventCollectorErrorHandler$ServerErrorException: The requested URL was not found on this server.
```
The above can be caused by:
- Provided the wrong URL
- URL includes a trailing /

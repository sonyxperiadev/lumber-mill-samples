# Cloudwatch Logs to Elasticsearch

Simple example of "streaming" logs from Cloudwatch Logs to Elasticsearch, either AWS Elasticsearch of regular Elasticsearch.
It does not contain any deployment features (yet), this is done in AWS Console.

1. Configure your elasticsearch settings

Edit src/main/groovy/LumberMillCloudWatchLogsLambda.groovy and change elasticsearch properties

    elasticsearch.client (
        url:          'http://<es_url>',
        index_prefix: 'index-',
        type:         '{type}',
        region:       'eu-west-1'
    )
    
2. Build
    
    gradle clean build
    
3. Copy ZIP file to s3 using AWS Console or CLI

    aws cp build/distributions/lumbermill-samples-lambdas.zip s3://<bucket>/lumbermill-samples-lambdas.zip
    
4. Create Lambda Function in AWS Console

    1. Create function, skip 'Select blueprint step'
    2. FunctionName = 'CloudwatchLogsLambda' (or something....)
    3. Runtime = Java8
    4. Upload a file from Amazon S3, S3 link URL = https://<url> (use AWS Console, S3, zip-file, properties, copy S3 url)
    5. Handler = lumbermill.LumberMillCloudWatchLogsLambda
    6. Role = Create a role with the permissions below
        
               {
               "Version": "2012-10-17",
               "Statement": [
                {
                   "Action": [
                       "es:*"
                   ],
                   "Resource": [
                       "*"
                   ],
                   "Effect": "Allow"
                },
                {
                   "Effect": "Allow",
                   "Action": [
                       "logs:CreateLogGroup",
                       "logs:CreateLogStream",
                       "logs:PutLogEvents"
                    ],
                    "Resource": "*"
                }
                ]
               }
    7. Memory = 512 MB
    8. Timeout = 1min (will go much faster)
    9. VPC = NoVpc (We have not yet tested with VPC, you probably need to setup a NAT)


5. Test

    1. Test -> Sample Event Template = Cloudwatch Logs.
    2. No errors should be visible in console and "Something" should be visible in Elasticsearch/Kibana

6. Add Event Source

    1. Event Source Type = CloudWatch Logs
    2. Choose the LogGroup you want to subscribe to
    3. Pick a name
    4. Pick a filter or leave empty
    5. Enable = true
    
    
Your logs should now be visible in kibana/ES
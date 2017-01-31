# Lumber Mill Samples Project

Samples for <https://github.com/sonyxperiadev/lumber-mill>

**More samples will be added shortly**


## Lambda for reading ELB Logs from S3, parse them and write to kinesis

Code src/main/groovy/Lumbermill/LumbermillS3Lambda.groovy

This sample does the following.


1. Downloads the ELB file
2. Compresses it
3. Puts the compressed version to another location
4. Reads each line 
5. Groks the ELB into a json structure for each field
6. Buffers each json before sending to kinesis
7. Writes to kinesis
8. Removes original file from s3

If you would like to change something, simply edit the groovy file and build.


#### Build

This will create build/distributions/lumbermill-lambda-samples-{version}.zip and contains
all required code.

```
gradle clean build
```

#### Installation / Deployment

It is expected that you can deploy a Lambda in AWS Console so we will not go in to details
how this is done.


1. Create Lambda function in Amazon console, start by clicking "Create a lambda function" button.

2. Choose a blank function

3. Choose trigger function S3. You can choose to enable trigger now or later.
      
    - Bucket = your access logs bucket
    - Event type = Complete Multipart Upload
    - Prefix = empty
    - Suffix = .log
      
      
4. Configure function and upload code

      - Name = LumbermillS3ToKinesisLambda (or a name of your choice)
      - Runtime = Java 8
      - Upload function code: build/distributions/lumbermill-lambda-samples-{version}.zip
      - Set Environment variables (see point 5)
      - Handler = lumbermill.LumbermillS3ToKinesisLambda
      - Role = Use an existing or create a new role with json below
      - Memory = Set to max (1535)
      - Timeout = 5 min      
      
      
5. Environment variables
       
       - *stream = your_stream_name (required)*
       - region = your_region  (default is eu-west-1) 
       - endpoint = custom kinesis endpoint (do not set unless you have special demands)
       - put_records_size = batch size (default is 150, max is 500)
       - max_connections = Nr of concurrent connections (default is 1)
       - ms_between_posts = Milliseconds between each kinesis post (per thread), default is 350ms        


5. Thats it
      
Under monitoring in AWS Console you can see invocations and errors and details
and trace logs are found under Cloudwatch Logs.
      
      
#### Role for S3 to Kinesis with access to cloudwatch logs   
   
Use this json when creating the role, limit to your resources if you desire.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
           "Effect": "Allow",
           "Action": [
               "logs:CreateLogGroup",
               "logs:CreateLogStream",
               "logs:PutLogEvents"
            ],
            "Resource": "*"
        },
        {
            "Action": [
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:GetObject",
                "s3:PutObject"
            ],
            "Resource": [
                "*"
            ],
            "Effect": "Allow"
        },
        {
            "Action": [
                "kinesis:PutRecords",
                "kinesis:GetRecords",
                "kinesis:GetShardIterator",
                "kinesis:DescribeStream",
                "kinesis:ListStreams"
            ],
            "Resource": [
                "*"
            ],
            "Effect": "Allow"
        }
    ]
}
```



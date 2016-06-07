/*
 * Copyright 2016 Sony Mobile Communications, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package lumbermill

import com.amazonaws.services.lambda.runtime.Context
import lumbermill.api.Event
import lumbermill.aws.lambda.CloudWatchLogsEventPreProcessor
import lumbermill.aws.lambda.CloudWatchLogsLambda
import lumbermill.aws.lambda.LambdaContextAwareEventProcessor
import rx.Observable

import static lumbermill.Core.addField
import static lumbermill.AWS.elasticsearch;

/*
 *  Not using AWS Elasticsearch?
 *  Use below import instead of lumbermil.AWS.elasticsearch
 */
//import static lumbermill.ElasticSearch.elasticsearch;

/**
 * This is the handler to invoke when setting up the Lambda
 * in Cloudformation, CLI, console or any other way.
 */
@SuppressWarnings("unused")
public class LumberMillCloudWatchLogsLambda extends CloudWatchLogsLambda {

    // Invoked by Lambda Runtime at deployment
    public LumberMillCloudWatchLogsLambda() {
        super(new CloudWatchLogsToKinesisEventProcessor());
    }
    
    /**
    * Received events from CloudWatch Logs. CloudWatchLogsEventPreProcessor decodes
    * the message into
    * Invoked with event from Lumber MIll AWS Lambda Framework
    */
    private static class CloudWatchLogsToKinesisEventProcessor implements LambdaContextAwareEventProcessor {

        private Context context;

        public void initialize(Context context) {
            this.context = context;
            context.getLogger().log("AWS Lambda Context Object successfully set");
        }    

        Observable<Event> call(Observable observable) {
            observable
            
            // Parse and de-normalize events
            .compose (
                new CloudWatchLogsEventPreProcessor()
            )

            .map ( addField('type','cloudwatchlogs'))
            .buffer (1000)
            .flatMap (
                elasticsearch.client (
                    url:          'http://<es_url>',
                    index_prefix: 'index-',
                    type:         '{type}',
                    region:       'eu-west-1'
                )
            )
        }
    }

}

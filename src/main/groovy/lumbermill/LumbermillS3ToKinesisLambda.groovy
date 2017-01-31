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

import lumbermill.api.EventProcessor
import lumbermill.aws.lambda.S3Lambda

import rx.Observable

import java.util.concurrent.TimeUnit

import static lumbermill.AWS.kinesis
import static lumbermill.AWS.s3
import static lumbermill.Core.*
import static lumbermill.api.Sys.env


@SuppressWarnings("unused")
public class LumbermillS3ToKinesisLambda extends S3Lambda {

    public LumbermillS3ToKinesisLambda() {
        super(new LumbermillS3EventProcessor());
    }

    public static class LumbermillS3EventProcessor implements EventProcessor {

        /**
         * Invoked for each S3 file with a JsonEvent describing the event
         */
        @Override
        Observable call(Observable observable) {

            observable

            .flatMap(
                s3.download(
                        bucket: '{bucket_name}',
                        key: '{key}',
                        remove: false
                )
            )

            // Compress file since we want to move it compressed
            .flatMap (
                gzip.compress (
                    file: '{s3_download_path}'
                )
            )

            // Put compressed file S3
            .flatMap (
                s3.put (
                    bucket: env('backupBucket', '{bucket_name}').string(),
                    key   : env('gzippedBackupKey', 'processed/{key}.gz').string(),
                    file  : '{gzip_path_compressed}'
                )
            )

            .flatMap (
                // For each line, add service metadata
                file.lines(file: '{s3_download_path}')
            )

            // Parse message with grok, tag with _grokparsefailure on miss
            .flatMap (
                grok.parse(
                        field: 'message',
                        pattern: '%{AWS_ELB_LOG}',
                        tagOnFailure: true
                )
            )

            // Use correct timestamp
            .flatMap (
                rename(
                    from: 'timestamp',
                    to: '@timestamp'
                )
            )
            .flatMap (
                addField('type', 'elb')
            )
            .flatMap (
                computeIfExists('message') {
                    fingerprint.md5('{message}')
                }
            )

            // No need to maximize to 300, sbdp mixmux likes smaller better
            .buffer(env('put_records_size','150').number())

            // This will smooth out the posting instead of hammering
            // (This is to support our internal system when clients want to test)
            .zipWith(Observable.interval( env('ms_between_posts','350').number(), TimeUnit.MILLISECONDS), { obs, timer -> obs})

            .flatMap (
                kinesis.bufferedProducer (
                    endpoint        : env('endpoint','').string(),
                    region          : env('region','eu-west-1').string(),
                    stream          : '{streamName}',
                    partition_key   : '{fingerprint}',
                    max_connections : env('max_connections','1').number(),
                    retry: [
                        policy    : 'exponential',
                        attempts  : 30,
                        delayMs   : 1300
                    ]
                )
            )
        }
    }
}

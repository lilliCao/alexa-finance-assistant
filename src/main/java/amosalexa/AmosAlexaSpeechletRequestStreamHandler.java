/**
 Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

 http://aws.amazon.com/apache2.0/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package amosalexa;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * This class could be the handler for an AWS Lambda function powering an Alexa Skills Kit
 * experience. To do this, simply set the handler field in the AWS Lambda console to
 * "amosalexa.AmosAlexaSpeechletRequestStreamHandler" For this to work, you'll also need to build
 * this project using the {@code lambda-compile} Ant task and upload the resulting zip file to power
 * your function.
 */
/*
public final class AmosAlexaSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds = new HashSet<String>();
    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.

        supportedApplicationIds.add("amzn1.ask.skill.464a154a-6a6a-48de-9036-841baaef8dd7"); // Gabriel
        supportedApplicationIds.add("amzn1.ask.skill.38e33c69-1510-43cd-be1d-929f08a966b4"); // Julian
        supportedApplicationIds.add("amzn1.ask.skill.3eaf7014-b9fb-46b3-84dc-59c5b639451c"); // Paul
        supportedApplicationIds.add("amzn1.ask.skill.ea3217b7-23d5-4783-b686-c04d68abe1ca"); // Vladimir
        supportedApplicationIds.add("amzn1.ask.skill.c0108bd4-5bd8-4d8a-9d44-11ce3db9dbdf"); // Lucas
        supportedApplicationIds.add("amzn1.ask.skill.3bfee2e7-b5b7-4855-b3e1-9b9cd3189d5e"); //Lilli

        supportedApplicationIds.add("amzn1.ask.skill.d9eeac14-3884-47a5-b688-728639af92ee"); // developer account

    }

    public AmosAlexaSpeechletRequestStreamHandler() {

        super(AmosAlexaSpeechlet.getInstance(), supportedApplicationIds);
    }
}
*/
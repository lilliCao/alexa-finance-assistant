/*
 * Copyright 2016-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 * this file except in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package model.location;

/**
 * This is a wrapper class that mimics the JSON structure returned from the Alexa Device Address API.
 * Refer to the Alexa Device Address API documentation on https://developer.amazon.com/ for more info.
 * Note that depending on the API path that you hit, not all properties will be populated.
 */
public class Address {

    private String stateOrRegion = "Germany";
    private String city = "Nürnberg";
    private String countryCode = null;
    private String postalCode = "90459";
    private String addressLine1 = "Wölkernstraße 11";

    public Address() {
    }

    public String getStateOrRegion() {
        return stateOrRegion;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

}

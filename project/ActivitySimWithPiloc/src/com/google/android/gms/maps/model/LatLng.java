/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

// NOTE: I was not able to get the source code of the original Google class, so I am modifying
//  the LatLng java code in another package 
//  See: http://grepcode.com/file/repo1.maven.org/maven2/com.google.maps/google-maps-services/0.1.3/com/google/maps/model/LatLng.java

package com.google.android.gms.maps.model;

/**
 * A place on Earth, represented by a Latitude/Longitude pair.
 */
public class LatLng {

    /**
     * The latitude of this location (in degrees).
     */
    public final double latitude;

    /**
     * The longitude of this location (in degrees).
     */
    public final double longitude;

    /**
     * Construct a location with a latitude longitude pair.
     */
    public LatLng(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return( new Double( latitude ).hashCode() | 
                new Double( longitude ).hashCode() );
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals( Object otherObject ) {
        // Cast the object
        LatLng other = (LatLng) otherObject;
        // Test if they are the same
        // Note: Comparing doubles is not a good idea, but for the CS4222
        //  project this should be fine
        return ( new Double( latitude ).equals( other.latitude ) && 
                 new Double( longitude ).equals( other.longitude ) );
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ("Lat:" + latitude + "Lng:" + longitude );
    }
}

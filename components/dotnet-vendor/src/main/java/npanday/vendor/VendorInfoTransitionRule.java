package npanday.vendor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Provides a service for filling in (or processing) vendor information requirements and transitioning its state. The
 * <code>StateMachineProcessor</code> is responsible for processing each transition rule within the framework.
 *
 * @author Shane Isbell
 * @see StateMachineProcessor
 */
public interface VendorInfoTransitionRule
{

    /**
     * Fills in some or all of the specified vendor info object and returns the new state.
     *
     * @param vendorRequirement the vendor info to fill in
     * @return the new state of the vendor info parameter
     */
    VendorRequirementState process( VendorRequirement vendorRequirement );
}

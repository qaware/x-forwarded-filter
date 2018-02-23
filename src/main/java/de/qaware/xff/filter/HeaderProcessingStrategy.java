/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.qaware.xff.filter;

/**
 * ForwardedHeader Processing Strategy offers choices:
 * <ul>
 * <li>EVAL_AND_KEEP</li>
 * <li>EVAL_AND_REMOVE</li>
 * <li>DONT_EVAL_AND_REMOVE</li>
 * </ul>
 */
public enum HeaderProcessingStrategy {

    //Evaluate headers remove afterwards so they will be visible to downstream filters and the application
    EVAL_AND_KEEP(true, false),

    //Evaluate headers remove afterwards so they wont be visible to downstream filters and the application.
    EVAL_AND_REMOVE(true, true),

    //Enables mode in which any "Forwarded" or "X-Forwarded-*" headers are removed only and the information in them ignored.
    DONT_EVAL_AND_REMOVE(false, true);

    //Mode: DONT_USE_AND_DONT_REMOVE is not supported as it is equal to not using/activating this filter at all

    private boolean evaluate;
    private boolean remove;

    HeaderProcessingStrategy(boolean evaluate, boolean remove) {
        this.evaluate = evaluate;
        this.remove = remove;
    }

    public boolean isEvaluateHeaders() {
        return evaluate;
    }

    public boolean isRemoveHeaders() {
        return remove;
    }
}

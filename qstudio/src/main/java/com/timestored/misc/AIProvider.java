/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.misc;

import java.io.IOException;
import java.util.List;

public interface AIProvider {
    /** Human-readable name for UI display (e.g. "OpenAI", "Claude"). */
    String getName();

    /** Send a list of messages and return the assistant's response text. */
    String chat(List<ChatMessage> messages) throws IOException;

    /** Return true if this provider has enough configuration to make API calls. */
    boolean isConfigured();

    /** Return a user-facing message describing what configuration is missing. */
    String getConfigurationHelp();

    /** Set the API key for this provider. */
    void setApiKey(String apiKey);

    /** Set the model to use. Pass null or empty to use the provider's default. */
    void setModel(String model);

    /** Set the base URL. Pass null or empty to use the provider's default. Not all providers support this. */
    void setBaseUrl(String baseUrl);

    /** Return prepopulated model options for this provider. First entry is the default. */
    String[] getModelOptions();
}

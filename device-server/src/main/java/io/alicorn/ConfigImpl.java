/*
 * Project: gh6
 * Since: Oct 21, 2016
 *
 * Copyright (c) Brandon Sanders [brandon@alicorn.io]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.alicorn;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Singleton
public class ConfigImpl implements Config {

    private static final Logger logger = LoggerFactory.getLogger(ConfigImpl.class);

    private String twilioAccountSID = "";
    private String twilioAuthToken = "";
    private int mongoDatabasePort = 27017;
    private String mongoDatabaseUrl = "localhost";
    private String mongoDatabaseName = "GH6";

    @Inject
    public ConfigImpl() {
        try {
            JsonObject json = Json.parse(new FileReader(new File("config.json"))).asObject();
            twilioAccountSID = json.get("twilioAccountSID").asString();
            twilioAuthToken = json.get("twilioAuthToken").asString();
        } catch (IOException e) {
            twilioAccountSID = System.getenv("gh6.twilio.acc.sid");
            twilioAuthToken = System.getenv("gh6.twilio.auth.sid");
            mongoDatabaseUrl = System.getenv("gh6.mongo.url");
            mongoDatabasePort = Integer.parseInt(System.getenv("gh6.mongo.port"));
            mongoDatabaseName = System.getenv("gh6.mongo.database");
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getTwilioAccountSID() {
        return twilioAccountSID;
    }

    @Override
    public String getTwilioAuthToken() {
        return twilioAuthToken;
    }

    @Override
    public String getMongoDatabaseName() {
        return mongoDatabaseName;
    }
}

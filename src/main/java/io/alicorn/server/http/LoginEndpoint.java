/*
 * Project: gh6
 * Since: Oct 22, 2016
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
package io.alicorn.server.http;

import com.eclipsesource.json.JsonObject;
import io.alicorn.data.jongothings.JongoDriver;
import io.alicorn.data.models.Agent;
import io.alicorn.data.models.Client;
import io.alicorn.data.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO:
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
@Singleton
public class LoginEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoginEndpoint.class);

    private Map<String, String> emailToTokenMap = new ConcurrentHashMap<>();
    private Map<String, User> tokenToUserMap = new ConcurrentHashMap<>();
    private Map<Thread, User> threadToUserMap = new ConcurrentHashMap<>();

    public static final Charset charset = Charset.forName("ISO-8859-1");

    public static String hash(byte[] bytes) {
        try {
            //Create the message digest.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);

            //Hash the bytes.
            byte[] hashed = digest.digest();

            //Translate the bytes into a hexadecimal string.
            StringBuilder hexString = new StringBuilder();
            for (byte aHashed : hashed) {
                String hex = Integer.toHexString(0xff & aHashed);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String hash(char[] chars) {
        //Parse chars into bytes for hashing.
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = charset.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                                          byteBuffer.position(),
                                          byteBuffer.limit());

        //Clear temporary arrays of any data.
        Arrays.fill(charBuffer.array(), '\u0000');
        Arrays.fill(byteBuffer.array(), (byte) 0);

        //Generate the SHA-256 hash.
        String hash = hash(bytes);

        //Clear remaining arrays of any data.
        Arrays.fill(bytes, (byte) 0);

        return hash;
    }

    public static String hash(String string) {
        return hash(string.getBytes(charset));
    }

    private String getTokenForUser(String email, String key, boolean asAgent) {
        User user;
        if (asAgent) {
            user = JongoDriver.getCollection("Agents").findOne("{email:#}", email).as(Agent.class);
        }  else {
            user = JongoDriver.getCollection("Clients").findOne("{email:#}", email).as(Client.class);
        }

        if (user.getKey().equals(hash(key))) {
            if (emailToTokenMap.containsKey(email)) {
                return emailToTokenMap.get(email);
            } else {
                String uuid = UUID.randomUUID().toString();
                emailToTokenMap.put(email, uuid);
                tokenToUserMap.put(uuid, user);
                return uuid;
            }
        }

        return "U sux";
    }

    @Inject
    public LoginEndpoint(SparkWrapper sparkWrapper) {

        sparkWrapper.before((req, res) -> {
            try {
                JsonObject json = JsonObject.readFrom(req.body()).asObject();
                if (json.get("token") != null) {
                    if (tokenToUserMap.containsKey(json.get("token").asString())) {
                        threadToUserMap.put(Thread.currentThread(), tokenToUserMap.get(json.get("token").asString()));
                    }
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        });

        sparkWrapper.after((req, res) -> {
            if (threadToUserMap.containsKey(Thread.currentThread())) {
                threadToUserMap.remove(Thread.currentThread());
            }
        });

        // Clients ////////////////////////////////////////////////////////////
        sparkWrapper.post("/api/user/client/token", (req, res) -> {
            JsonObject json = JsonObject.readFrom(req.body());
            return getTokenForUser(json.get("email").asString(), json.get("key").asString(), false);
        });

        sparkWrapper.post("/api/user/client/create", (req, res) -> {
            JsonObject json = JsonObject.readFrom(req.body());
            if (hasCurrentUser() && getCurrentUser().getKind().equals(User.Kind.AGENT)) {
                JsonObject user = json.get("user").asObject();
                if (user.get("email") != null && user.get("password") != null) {
                    user.set("password", hash(user.get("password").asString()));
                    JongoDriver.getCollection("Clients").update("{email:#}",
                                                                user.get("email").asString()).upsert().with(user.toString());
                    return "Created user!";
                }
            }

            return "U SUCK";
        });

        // Agents /////////////////////////////////////////////////////////////
        sparkWrapper.post("/api/user/agent/token", (req, res) -> {
            JsonObject json = JsonObject.readFrom(req.body());
            return getTokenForUser(json.get("email").asString(), json.get("key").asString(), true);
        });
    }

    public boolean hasCurrentUser() {
        return threadToUserMap.containsKey(Thread.currentThread());
    }

    public User getCurrentUser() {
        return threadToUserMap.get(Thread.currentThread());
    }
}
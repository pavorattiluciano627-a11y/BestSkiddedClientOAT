package dev.echo.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Base64;

/** Reads the configuration response protocol, including its original fallback rules. */
public final class ProfileResponseCodec {
    private ProfileResponseCodec() { }

    /**
     * Parses an object response; malformed JSON and nonobjects become empty objects.
     *
     * @param text the response text
     * @return the parsed object, or a fresh empty object
     */
    public static JsonObject parseObject(String text) {
        try {
            JsonElement element = JsonParser.parseString(text);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception exception) {
            return new JsonObject();
        }
    }

    /**
     * Tests the response's optional success flag.
     *
     * <p>An absent flag means success, whereas a null response means failure.
     * Invalid flag values retain Gson's conversion exceptions.
     *
     * @param response the response object
     * @return whether the response reports success or omits the flag
     */
    public static boolean isSuccessful(JsonObject response) {
        return response != null && (!response.has("success") || response.get("success").getAsBoolean());
    }

    /**
     * Returns an object-valued member, without coercing arrays or primitive values.
     *
     * @param object the containing object
     * @param name the member name
     * @return the member object, or null
     */
    public static JsonObject objectField(JsonObject object, String name) {
        if (object == null || !object.has(name) || !object.get(name).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(name);
    }

    /**
     * Returns text, using an empty string for missing and JSON-null values.
     *
     * @param object the containing object
     * @param name the member name
     * @return the member text
     */
    public static String textField(JsonObject object, String name) {
        return textField(object, name, "");
    }

    /**
     * Returns text, preserving conversion failures for present nontextual values.
     *
     * @param object the containing object
     * @param name the member name
     * @param fallback the value for absent and JSON-null members
     * @return the member text or fallback
     */
    public static String textField(JsonObject object, String name, String fallback) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return fallback;
        }
        return object.get(name).getAsString();
    }

    /**
     * Reads a timestamp, returning zero for absent, null or invalid values.
     *
     * @param object the containing object
     * @param name the member name
     * @return the timestamp or zero
     */
    public static long timestampField(JsonObject object, String name) {
        try {
            if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
                return 0L;
            }
            return object.get(name).getAsLong();
        } catch (Exception exception) {
            return 0L;
        }
    }

    /**
     * Reads a flag using false for absent, null or invalid values.
     *
     * @param object the containing object
     * @param name the member name
     * @return the flag or false
     */
    public static boolean booleanField(JsonObject object, String name) {
        return booleanField(object, name, false);
    }

    /**
     * Reads a flag with the supplied missing/invalid fallback.
     *
     * @param object the containing object
     * @param name the member name
     * @param fallback the default flag
     * @return the flag or fallback
     */
    public static boolean booleanField(JsonObject object, String name, boolean fallback) {
        try {
            if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
                return fallback;
            }
            return object.get(name).getAsBoolean();
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * Decodes standard Base64 icon data, retaining null for blank or invalid input.
     *
     * @param text the encoded icon
     * @return decoded bytes or null
     */
    public static byte[] decodeIcon(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Reads all seventeen metadata components in their original evaluation order.
     *
     * @param object the metadata object
     * @return the decoded metadata
     */
    public static ConfigMetadata metadata(JsonObject object) {
        return new ConfigMetadata(
                textField(object, "id"), textField(object, "name"),
                textField(object, "description"), textField(object, "author"),
                textField(object, "serverName"), textField(object, "serverIp"),
                decodeIcon(textField(object, "serverIconBase64")),
                textField(object, "type", "GHOST"),
                "PUBLIC".equalsIgnoreCase(textField(object, "visibility")),
                timestampField(object, "createdAt"), timestampField(object, "updatedAt"),
                booleanField(object, "active"), booleanField(object, "owned", true),
                textField(object, "sourceId"), textField(object, "publishedId"),
                booleanField(object, "publishedChanges"), booleanField(object, "updateAvailable"));
    }
}

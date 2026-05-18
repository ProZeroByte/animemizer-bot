package pro.zerobyte.animemizerbot.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.zerobyte.animemizerbot.exception.JsonDeserializationException;
import pro.zerobyte.animemizerbot.exception.JsonSerializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
//TODO: Think about remove unused methods
public class JsonService {
    private static final Logger log = LoggerFactory.getLogger(JsonService.class);

    private final JsonMapper jsonMapper;

    public JsonService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String toJson(Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            log.error("Failed to serialize object of class {} to JSON String", object.getClass().getSimpleName(), e);
            throw new JsonSerializationException("JSON string serialization error", e);
        }
    }

    public void writeValue(OutputStream stream, Object object) {
        try {
            byte[] bytes = jsonMapper.writeValueAsBytes(object);
            stream.write(bytes);
            stream.flush();
        } catch (IOException e) {
            log.error("Failed to write object of class {} as JSON to OutputStream", object.getClass().getSimpleName(), e);
            throw new JsonSerializationException("JSON stream write error", e);
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return jsonMapper.readValue(json, clazz);
        } catch (JacksonException e) {
            log.error("Failed to deserialize JSON String to object of class {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("JSON string deserialization error", e);
        }
    }

    public <T> T fromJson(byte[] json, Class<T> clazz) {
        try {
            return jsonMapper.readValue(json, clazz);
        } catch (JacksonException e) {
            log.error("Failed to deserialize JSON byte[] to object of class {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("JSON byte array deserialization error", e);
        }
    }

    public <T> T fromJson(InputStream stream, Class<T> clazz) {
        try {
            return jsonMapper.readValue(stream, clazz);
        } catch (JacksonException e) {
            log.error("Failed to deserialize JSON InputStream to object of class {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("JSON stream deserialization error", e);
        }
    }

    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return jsonMapper.readValue(json, typeReference);
        } catch (JacksonException e) {
            log.error("Failed to deserialize JSON String to complex type {}", typeReference.getType(), e);
            throw new JsonDeserializationException("JSON complex type deserialization error", e);
        }
    }
}


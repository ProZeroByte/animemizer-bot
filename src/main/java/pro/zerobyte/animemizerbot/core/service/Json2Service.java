package pro.zerobyte.animemizerbot.core.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.zerobyte.animemizerbot.exception.JsonDeserializationException;
import pro.zerobyte.animemizerbot.exception.JsonSerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
public class Json2Service {
    private static final Logger log = LoggerFactory.getLogger(Json2Service.class);

    private final JsonMapper jsonMapper;

    public Json2Service(JsonMapper json2Mapper) {
        this.jsonMapper = json2Mapper;
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

    public <T> T fromJson(InputStream stream, Class<T> clazz) {
        try {
            return jsonMapper.readValue(stream, clazz);
        } catch (IOException e) {
            log.error("Failed to deserialize JSON InputStream to object of class {}", clazz.getSimpleName(), e);
            throw new JsonDeserializationException("JSON stream deserialization error", e);
        }
    }

}

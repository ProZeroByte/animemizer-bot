package pro.zerobyte.animemizerbot.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import pro.zerobyte.animemizerbot.core.service.Json2Service;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Json2Service json2Service;

    public WebConfig(Json2Service json2Service) {this.json2Service = json2Service;}

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        AbstractHttpMessageConverter<Object> telegramConverter = new AbstractHttpMessageConverter<>(MediaType.APPLICATION_JSON) {

            @Override
            protected boolean supports(@NonNull Class<?> clazz) {
                return BotApiMethod.class.isAssignableFrom(clazz) || Update.class.isAssignableFrom(clazz);
            }

            @Override
            protected @NonNull Object readInternal(@NonNull Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
                return json2Service.fromJson(inputMessage.getBody(), clazz);
            }

            @Override
            protected void writeInternal(@NonNull Object object, HttpOutputMessage outputMessage) throws IOException {
                json2Service.writeValue(outputMessage.getBody(), object);
            }
        };

        builder.addCustomConverter(telegramConverter);
    }
}

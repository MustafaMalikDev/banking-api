package com.github.mustafamalikdev.banking.util;

import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.Resource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class JsonFileHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> List<T> get(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readValue(inputStream, new TypeReference<>() {});
        }
    }


    public static <T> List<T> getList(Resource resource, Class<T> elementClass) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readValue(
                    inputStream,
                    mapper.getTypeFactory().constructCollectionType(List.class, elementClass)
            );
        }
    }
}

package apps.sarafrika.elimika.shared.config;

import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;

/**
 * Hibernate JSON column mapper on Jackson 3; Hibernate 7.2 ships only a Jackson 2 one,
 * which cannot read the {@code tools.jackson} JsonNode fields our entities use.
 */
public class Jackson3JsonFormatMapper extends AbstractJsonFormatMapper {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public <T> T fromString(CharSequence charSequence, Type type) {
        return jsonMapper.readValue(charSequence.toString(), jsonMapper.constructType(type));
    }

    @Override
    public <T> String toString(T value, Type type) {
        return jsonMapper.writerFor(jsonMapper.constructType(type)).writeValueAsString(value);
    }
}

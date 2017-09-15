package info.rmapproject.spring.env.support;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface EnumerablePropertySources extends PropertySources {

    @Nullable
    @Override
    EnumerablePropertySource<?> get(String name);

}

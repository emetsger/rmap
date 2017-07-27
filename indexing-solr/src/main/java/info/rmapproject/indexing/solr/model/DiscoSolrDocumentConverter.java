package info.rmapproject.indexing.solr.model;

import org.apache.commons.collections.map.HashedMap;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DiscoSolrDocumentConverter implements Converter<DiscoSolrDocument, SolrInputDocument> {

    @Override
    public SolrInputDocument convert(DiscoSolrDocument discoSolrDocument) {
        final String defaultValue = (String)AnnotationUtils.getDefaultValue(
                org.apache.solr.client.solrj.beans.Field.class);

        Set<String> fields = Stream.of(discoSolrDocument.getClass().getFields())
                .filter(field -> AnnotatedElementUtils.isAnnotated(field, "Field"))
                .map(field -> {
                    String namedField;
                    if ((namedField = AnnotatedElementUtils.getMergedAnnotationAttributes(field, "Field")
                            .getString("value")).equals(defaultValue)) {
                        return field.getName();
                    } else {
                        return namedField;
                    }
                })
                .collect(Collectors.toSet());

        SolrInputDocument solrInputDocument = new SolrInputDocument(fields.toArray(new String[]{}));

//        solrInputDocument.
        return solrInputDocument;
    }

}

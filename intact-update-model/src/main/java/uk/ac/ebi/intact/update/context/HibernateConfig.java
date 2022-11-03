package uk.ac.ebi.intact.update.context;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class HibernateConfig extends Configuration {

    private static final TypeFilter[] DEFAULT_ENTITY_TYPE_FILTERS = new TypeFilter[] {
            new AnnotationTypeFilter(Entity.class, false),
            new AnnotationTypeFilter(Embeddable.class, false),
            new AnnotationTypeFilter(MappedSuperclass.class, false)};

    private final Set<String> entityClassNames = new TreeSet<>();
    private final Set<String> converterClassNames = new TreeSet<>();
    private final Set<String> packageNames = new TreeSet<>();

    private static final String RESOURCE_PATTERN = "/**/*.class";

    private static final String PACKAGE_INFO_SUFFIX = ".package-info";


    private final ResourcePatternResolver resourcePatternResolver;

    private static TypeFilter converterTypeFilter;

    static {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> converterAnnotation = (Class<? extends Annotation>)
                    ClassUtils.forName("javax.persistence.Converter", Configuration.class.getClassLoader());
            converterTypeFilter = new AnnotationTypeFilter(converterAnnotation, false);
        }
        catch (ClassNotFoundException ex) {
            // JPA 2.1 API not available - Hibernate <4.3
        }
    }

    public HibernateConfig() {
        this(new PathMatchingResourcePatternResolver());
    }

    public HibernateConfig(ClassLoader classLoader) {
        this(new PathMatchingResourcePatternResolver(classLoader));
    }

    public HibernateConfig(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    }

    public HibernateConfig scanPackages(String... packagesToScan) throws HibernateException {
        try {
            for (String pkg : packagesToScan) {
                String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                        ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;

                Resource[] resources = this.resourcePatternResolver.getResources(pattern);
                MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        MetadataReader reader = readerFactory.getMetadataReader(resource);
                        String className = reader.getClassMetadata().getClassName();
                        if (matchesEntityTypeFilter(reader, readerFactory)) {
                            entityClassNames.add(className);
                        }
                        else if (converterTypeFilter != null && converterTypeFilter.match(reader, readerFactory)) {
                            converterClassNames.add(className);
                        }
                        else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
                            packageNames.add(className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
                        }
                    }
                }
            }
        }
        catch (IOException ex) {
            throw new MappingException("Failed to scan classpath for unlisted classes", ex);
        }
        try {
            ClassLoader cl = this.resourcePatternResolver.getClassLoader();
            for (String className : entityClassNames) {
                addAnnotatedClass(cl.loadClass(className));
            }
            for (String className : converterClassNames) {
                ConverterRegistrationDelegate.registerConverter(this, cl.loadClass(className));
            }
            for (String packageName : packageNames) {
                addPackage(packageName);
            }
        }
        catch (ClassNotFoundException ex) {
            throw new MappingException("Failed to load annotated classes from classpath", ex);
        }
        return this;
    }

    private boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
        for (TypeFilter filter : DEFAULT_ENTITY_TYPE_FILTERS) {
            if (filter.match(reader, readerFactory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inner class to avoid hard dependency on JPA 2.1 / Hibernate 4.3.
     */
    private static class ConverterRegistrationDelegate {

        @SuppressWarnings("unchecked")
        public static void registerConverter(Configuration config, Class<?> converterClass) {
            config.addAttributeConverter((Class<? extends AttributeConverter<?, ?>>) converterClass);
        }
    }

    public Set<Class<?>> getEntityClasses() {
        ClassLoader cl = this.resourcePatternResolver.getClassLoader();
        return entityClassNames.stream().map(classString -> {
            try {
                return cl.loadClass(classString);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toUnmodifiableSet());

    }

    public Set<Class<?>> getConverterClasses() {
        ClassLoader cl = this.resourcePatternResolver.getClassLoader();
        return converterClassNames.stream().map(classString -> {
            try {
                return cl.loadClass(classString);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> getPackageNames() {
        return packageNames;
    }
}

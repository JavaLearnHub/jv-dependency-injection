package mate.academy.lib;

import mate.academy.exceptions.ComponentMissingException;
import mate.academy.exceptions.FieldInitializationException;
import mate.academy.exceptions.InstanceCreationException;
import mate.academy.service.FileReaderService;
import mate.academy.service.ProductParser;
import mate.academy.service.ProductService;
import mate.academy.service.impl.FileReaderServiceImpl;
import mate.academy.service.impl.ProductParserImpl;
import mate.academy.service.impl.ProductServiceImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Injector {

    private static final Injector INJECTOR = new Injector();
    private Map<Class<?>, Object> instances = new HashMap<>();

    public static Injector getInjector() {
        return INJECTOR;
    }

    public Object getInstance(Class<?> interfaceClazz) throws ComponentMissingException, InstanceCreationException, FieldInitializationException {

        Object clazzImplementationInstance = null;
        Class<?> clazz = findImplementation(interfaceClazz);

        if (!clazz.isAnnotationPresent(Component.class)) {
            throw new ComponentMissingException("Injection failed, missing @Component annotation on the class " + clazz.getName());
        }

        for (Field field : interfaceClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {

                Object fieldInstance = getInstance(field.getType());
                clazzImplementationInstance = createNewInstance(clazz);

                field.setAccessible(true);

                try {
                    field.set(clazzImplementationInstance, fieldInstance);
                } catch (IllegalAccessException e) {

                    throw new FieldInitializationException("Can't initialize field value. " +
                            "Class: " + clazz.getName() + ". Field: " + field.getName());
                }
            }
        }


        if (clazzImplementationInstance == null) {
            clazzImplementationInstance = createNewInstance(clazz);
        }
        return clazzImplementationInstance;
    }

    private Object createNewInstance(Class<?> clazz) throws InstanceCreationException {
        if (instances.containsKey(clazz)) {
            return instances.get(clazz);
        }

        try {
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();
            instances.put(clazz, instance);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new InstanceCreationException("Can't create a new instance of " + clazz.getName());
        }
    }

    private Class<?> findImplementation(Class<?> interfaceClazz) {

        Map<Class<?>, Class<?>> interfaceImplementations = Map.of(
                FileReaderService.class, FileReaderServiceImpl.class,
                ProductParser.class, ProductParserImpl.class,
                ProductService.class, ProductServiceImpl.class
        );

        if (interfaceClazz.isInterface()) {
            return interfaceImplementations.get(interfaceClazz);
        }

        return interfaceClazz;
    }
}

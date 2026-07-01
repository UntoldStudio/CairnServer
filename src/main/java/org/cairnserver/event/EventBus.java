package org.cairnserver.event;

import org.cairnserver.util.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {
    private static final Map<Class<? extends Event>, List<RegisteredHandler>> handlerMap = new ConcurrentHashMap<>();

    private EventBus(){}

    public static void registerListener(EventListener listener) {
        for (Method method : listener.getClass().getMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1 || !Event.class.isAssignableFrom(paramTypes[0])) continue;
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramTypes[0];
            boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
            handlerMap.computeIfAbsent(eventClass, _ -> new ArrayList<>()).add(new RegisteredHandler(listener, method, isStatic));
        }
    }

    public static void registerStaticListener(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1 || !Event.class.isAssignableFrom(paramTypes[0])) continue;
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramTypes[0];
            handlerMap.computeIfAbsent(eventClass, _ -> new ArrayList<>()).add(new RegisteredHandler(null, method, true));
        }
    }

    public static Event post(Event event) {
        List<RegisteredHandler> handlers = handlerMap.getOrDefault(event.getClass(), Collections.emptyList());
        for (RegisteredHandler handler : handlers) {
            try {
                if (handler.isStatic) {
                    handler.method.invoke(null, event);
                } else {
                    handler.method.invoke(handler.listener, event);
                }
            } catch (Exception e) {
                Logger.warn("事件处理失败:" + event.getClass().getSimpleName(), e);
            }
        }
        return event;
    }

    private record RegisteredHandler(EventListener listener, Method method, boolean isStatic) {}
}

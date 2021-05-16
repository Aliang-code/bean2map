package com.netease.bean2map.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapCodecRegister {
    private static final Logger log = LoggerFactory.getLogger(MapCodecRegister.class);
    private static final Map<Class, IMapCodec> map = new ConcurrentHashMap<>();

    static {
        try {
            Enumeration<URL> enumUrl = MapCodecRegister.class.getClassLoader().getResources("META-INF/" + MapCodecRegister.class.getName());
            while (enumUrl.hasMoreElements()) {
                URL url = enumUrl.nextElement();
                try (InputStream in = url.openStream()) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        Class<?> codecClass = Class.forName(line);
                        if (IMapCodec.class.isAssignableFrom(codecClass)) {
                            Class<?> entityClass = register((IMapCodec<?>) codecClass.newInstance());
                            log.debug("[PRELOAD] codec[{}] for [{}]", codecClass.getName(), entityClass.getName());
                        } else {
                            log.warn("[PRELOAD] codec[{}] illegal", codecClass.getName());
                        }
                    }

                } catch (IOException e) {
                    log.error("[PRELOAD] codec mainfest[" + url + "] failed:", e);
                }
            }
        } catch (Exception var19) {
            log.error("[PRELOAD] codec mainfest error:", var19);
        }
    }

    public static <T> Class<T> register(IMapCodec<T> codec) {
        Type[] typeArguments = ((ParameterizedType) codec.getClass().getGenericInterfaces()[0]).getActualTypeArguments();
        if (typeArguments.length > 0) {
            Class<T> clazz = (Class<T>) typeArguments[0];
            map.put(clazz, codec);
            return clazz;
        } else {
            throw new RuntimeException("can not get entity class");
        }
    }

    public static <T> IMapCodec<T> getCodec(Class<T> clazz) {
        return map.get(clazz);
    }
}

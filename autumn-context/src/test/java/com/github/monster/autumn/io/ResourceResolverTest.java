package com.github.monster.autumn.io;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.sql.DataSourceDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Monster
 */
class ResourceResolverTest {

    @Test
    void scanClass() {
        String pkg = "com.github.monster.autumn.io.scan";
        ResourceResolver resolver = new ResourceResolver(pkg);
        List<String> scan = resolver.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });

        System.out.println(scan);

        String[] expect = new String[] {
                "com.github.monster.autumn.io.scan.scan",
                "com.github.monster.autumn.io.scan.sub1.sub1",
                "com.github.monster.autumn.io.scan.sub1.sub2.sub2",
        };

        for (String clazz : expect) {
            assertTrue(scan.contains(clazz));
        }

    }

    @Test
    void scanJar(){
        var pkg = PostConstruct.class.getPackageName();
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        // classes in jar:
        assertTrue(classes.contains(PostConstruct.class.getName()));
        assertTrue(classes.contains(PreDestroy.class.getName()));
        assertTrue(classes.contains(PermitAll.class.getName()));
        assertTrue(classes.contains(DataSourceDefinition.class.getName()));
    }
}

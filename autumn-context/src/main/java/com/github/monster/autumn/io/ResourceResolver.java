package com.github.monster.autumn.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 文件路径解析
 *
 * @author Monster
 */
public class ResourceResolver {

    Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 例如：com.github.monster
     */
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource, R> mapper) {
        String basePackagePath = this.basePackage.replace(".", "/");
        Enumeration<URL> resources = null;
        List<R> collector = new ArrayList<>();
        try {
            resources = getContextClassLoader().getResources(basePackagePath);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                URI uri = url.toURI();

                // url可能存在尾部斜线
                String uriStr = removeTrailingSlash(URLDecoder.decode(url.toString(), StandardCharsets.UTF_8));

                String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
                if (uriBaseStr.startsWith("file:")) {
                    uriBaseStr = uriBaseStr.substring(5);
                }
                if (uriStr.startsWith("jar:")) {
                    scanFile(true, uriBaseStr, FileSystems.newFileSystem(uri, Map.of()).getPath(basePackagePath), collector, mapper);
                } else {
                    scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
                }

            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return collector;
    }

    /**
     * ClassLoader首先从Thread.getContextClassLoader()获取，如果获取不到，再从当前Class获取，
     * 因为Web应用的ClassLoader不是JVM提供的基于Classpath的ClassLoader，而是Servlet容器提供的ClassLoader，它不在默认的Classpath搜索，
     * 而是在/WEB-INF/classes目录和/WEB-INF/lib的所有jar包搜索，从Thread.getContextClassLoader()可以获取到Servlet容器专属的ClassLoader；
     */
    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }

    /**
     * 扫描目录时，返回的路径可能是abc/xyz，也可能是abc/xyz/，需要注意处理末尾的/
     */
    String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", res);
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    /**
     * 去除路径前的斜杠
     */
    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

}

package notnullcheckweaver;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Generates run-time checks that enforce the @NotNull annotations in a specified package and its subpackages.    
 */
public final class NotNullCheckWeaver {
    private NotNullCheckWeaver() {}
    
    private static File getJarBaseDir() {
        String myUrl = NotNullCheckWeaver.class.getResource("NotNullCheckWeaver.class").toString();
        if (!(myUrl.startsWith("jar:file:") && myUrl.contains("!/")))
            throw new AssertionError();
        int i = myUrl.lastIndexOf("!/");
        File jarPath = new File(myUrl.substring(9, i));
        return jarPath.getParentFile();
    }
    
    /**
     * Called by the JVM when the weaver is used as a Java agent.
     * 
     * To use the weaver, start the JVM as follows:
     * <pre>java -javaagent:notnullcheckweaver.jar=my.root.package my.root.package.MyMainClass</pre>
     * If started this way, the weaver instruments all classes in the specified package
     * and its subpackages.
     * 
     * Note: the weaver ignores package annotations for packages not below the specified package. 
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        File baseDir = getJarBaseDir();
        File weaverJarFile = new File(baseDir, "notnullcheckweaver-weaver.jar");
        if (!weaverJarFile.exists())
            throw new RuntimeException(
                "File '"+weaverJarFile+"' does not exist. "+
                "Please put notnullcheckweaver-weaver.jar in the same directory as notnullcheckweaver.jar.");
        URL weaverJar;
        try {
            weaverJar = weaverJarFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URLClassLoader loader = new URLClassLoader(new URL[] {weaverJar}) {
            public Class loadClass(String className) throws ClassNotFoundException {
                // Look here first, then in parent class loader.
                try {
                    return findClass(className);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(className);
                }
            }
        };
        try {
            Class<?> weaver = loader.loadClass("notnullcheckweaver.weaver.NotNullCheckWeaver");
            weaver.getMethod("premain", new Class[] {String.class, Instrumentation.class}).invoke(null, new Object[] {agentArgs, inst});
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

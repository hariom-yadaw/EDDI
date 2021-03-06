package ai.labs.rest.restinterfaces;

/**
 * @author ginccc
 */
public interface IRestInterfaceFactory {
    <T> T get(Class<T> clazz, String targetServerUri) throws RestInterfaceFactory.RestInterfaceFactoryException;

    <T> T get(Class<T> clazz, String targetServerUri, String securityToken) throws RestInterfaceFactory.RestInterfaceFactoryException;
}

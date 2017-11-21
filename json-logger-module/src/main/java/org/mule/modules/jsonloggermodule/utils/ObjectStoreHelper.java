package org.mule.modules.jsonloggermodule.utils;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;

import org.apache.commons.lang.Validate;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.Registry;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;

public class ObjectStoreHelper {

    /**
     * Manages object stores in mule runtime.
     */
    @Inject
    private ObjectStoreManager objectStoreManager;
    
    /**
     * Lock id to handle shared objects in store.
     */
    private final String sharedObjectStoreLockId = new Random().nextInt(1000) + "-" + System.currentTimeMillis() + "-lock";

    /**
     * A reference for cleaner access assigned in init
     */
    private ObjectStore<Serializable> objectStore;
    
    /**
     * Required parameters to interact with Mule OS
     */
    private Registry registry;
    private MuleContext muleContext;

	public ObjectStoreHelper(Registry registry, MuleContext muleContext) {
		this.setRegistry(registry);
		this.setMuleContext(muleContext);
		this.setObjectStore(createObjectStore());
	}

	public void store(final String key, final Serializable value) throws ObjectStoreException {
        executeInsideLock(key, new ObjectStoreTask<Void>() {

            @Override
            public Void execute() throws ObjectStoreException {
                Validate.notNull(value);
                try {
                    objectStore.store(key, value);
                } catch (ObjectAlreadyExistsException e) {
                    objectStore.remove(key);
                    objectStore.store(key, value);
                }
                return null;
            }
        });
    }
	
	public Object retrieve(final String key) throws ObjectStoreException {
        return executeInsideLock(key, new ObjectStoreTask<Object>() {

            @Override
            public Object execute() throws ObjectStoreException {
                Object availableObject;
                availableObject = objectStore.retrieve(key);
                return availableObject;
            }
        });
    }
    
	private interface ObjectStoreTask<T> {
        T execute() throws ObjectStoreException;
    }
	
	private <T> T executeInsideLock(final String key, final ObjectStoreTask<T> task) throws ObjectStoreException {
        Validate.notNull(key);
        Lock lock = muleContext.getLockFactory().createLock(sharedObjectStoreLockId + "-" + key);
        lock.lock();
        try {
            return task.execute();
        } finally {
            lock.unlock();
        }
    }
	
	private ObjectStore<Serializable> createObjectStore() {
		return registry.lookupObject(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME);
    }
	
    // Boilerplate 
    
	public ObjectStore<Serializable> getObjectStore() {
		return objectStore;
	}

	public void setObjectStore(ObjectStore<Serializable> objectStore) {
		this.objectStore = objectStore;
	}

	public String getSharedObjectStoreLockId() {
		return sharedObjectStoreLockId;
	}    

    public void setObjectStoreManager(final ObjectStoreManager objectStoreManager) {
        this.objectStoreManager = objectStoreManager;
    }

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public MuleContext getMuleContext() {
		return muleContext;
	}

	public void setMuleContext(MuleContext muleContext) {
		this.muleContext = muleContext;
	}
    
}

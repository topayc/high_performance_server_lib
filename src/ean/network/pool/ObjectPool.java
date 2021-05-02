package ean.network.pool;

import java.util.ArrayList;
import java.util.List;

public class ObjectPool<T>{
	private  List<T> freeObjects;
	private  int maxSize;
	private PoolObjectFactory<T> factory;


	public ObjectPool(PoolObjectFactory<T> factory, int maxSize) {
		this.maxSize = maxSize;
		this.factory = factory;
		this.freeObjects = new ArrayList<T>(maxSize);
	}

	public  synchronized T getObject() {
		T object = null;
		if (freeObjects.size() == 0)
			object = factory.createObject();
		else 
			object = freeObjects.remove(freeObjects.size() -1);
		return object;
	}

	public   synchronized void freeObject(T object) {
		if (freeObjects.size() < maxSize)
			freeObjects.add(object);
	}
}

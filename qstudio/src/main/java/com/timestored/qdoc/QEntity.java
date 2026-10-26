package com.timestored.qdoc;

import com.timestored.qdoc.DocumentedEntity;

/**
 * Represents a single object within Q, uniquely identifiable by it's full name.
 */
public interface QEntity extends DocumentedEntity {

	public abstract String getNamespace();
}
/*
 * #%L
 * SciJava Common shared library for SciJava software.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, Max Planck
 * Institute of Molecular Cell Biology and Genetics, University of
 * Konstanz, and KNIME GmbH.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.object;

import java.util.List;

import org.scijava.Named;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.object.event.ObjectCreatedEvent;
import org.scijava.object.event.ObjectDeletedEvent;
import org.scijava.object.event.ObjectsAddedEvent;
import org.scijava.object.event.ObjectsRemovedEvent;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default service for keeping track of registered objects. Automatically
 * registers new objects from {@link ObjectCreatedEvent}s, and removes objects
 * from {@link ObjectDeletedEvent}s.
 * <p>
 * This is useful to retrieve available objects of a particular type. For
 * example, the {@link org.scijava.widget.InputHarvester} infrastructure uses it
 * to provide a pool of available objects for widgets such as the
 * {@link org.scijava.widget.ObjectWidget}, which provide the user with a
 * multiple-choice selection.
 * </p>
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public final class DefaultObjectService extends AbstractService implements
	ObjectService
{

	@Parameter
	private EventService eventService;

	/** Index of registered objects. */
	private NamedObjectIndex<Object> objectIndex;

	// -- ObjectService methods --

	@Override
	public EventService eventService() {
		return eventService;
	}

	@Override
	public ObjectIndex<Object> getIndex() {
		return objectIndex;
	}

	@Override
	public <T> List<T> getObjects(final Class<T> type) {
		final List<Object> list = objectIndex.get(type);
		@SuppressWarnings("unchecked")
		final List<T> result = (List<T>) list;
		return result;
	}

	@Override
	public void addObject(final Object obj) {
		addObject(obj, null);
	}

	@Override
	public void addObject(Object obj, String name) {
		objectIndex.add(obj, name);		
		eventService.publish(new ObjectsAddedEvent(obj));
	}

	@Override
	public void removeObject(final Object obj) {
		objectIndex.remove(obj);
		eventService.publish(new ObjectsRemovedEvent(obj));
	}

	@Override
	public String getName(Object obj) {
		if (obj == null) throw new NullPointerException();
		final String name = objectIndex.getName(obj);
		if (name != null) return name;
		if (obj instanceof Named) {
			final String n = ((Named) obj).getName();
			if (n != null) return n;
		}
		final String s = obj.toString();
		if (s != null) return s;
		return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
	}

	// -- Service methods --

	@Override
	public void initialize() {
		objectIndex = new NamedObjectIndex<>(Object.class);
	}

	// -- Event handlers --

	@EventHandler
	protected void onEvent(final ObjectCreatedEvent event) {
		addObject(event.getObject());
	}

	@EventHandler
	protected void onEvent(final ObjectDeletedEvent event) {
		removeObject(event.getObject());
	}
}

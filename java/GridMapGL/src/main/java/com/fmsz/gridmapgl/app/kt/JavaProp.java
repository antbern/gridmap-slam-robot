/*******************************************************************************
 *  Copyright 2018 Anton Berneving
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package com.fmsz.gridmapgl.app.kt;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.reflect.KMutableProperty0;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty0;
import kotlin.reflect.KType;
import kotlin.reflect.KTypeParameter;
import kotlin.reflect.KVisibility;

public class JavaProp<T> implements KMutableProperty0<T> {

	private com.fmsz.gridmapgl.app.kt.Getter<T> getter;
	private com.fmsz.gridmapgl.app.kt.Setter<T> setter;

	public JavaProp(com.fmsz.gridmapgl.app.kt.Getter<T> getter, com.fmsz.gridmapgl.app.kt.Setter<T> setter) {
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void set(T t) {
		setter.set(t);
	}

	@NotNull
	@Override
	public Setter<T> getSetter() {
		return null;
	}

	@Override
	public T get() {
		return getter.get();
	}

	@Nullable
	@Override
	public Object getDelegate() {
		return null;
	}

	@NotNull
	@Override
	public KProperty0.Getter<T> getGetter() {
		return null;
	}

	@Override
	public T invoke() {
		return get();
	}

	@Override
	public boolean isLateinit() {
		return false;
	}

	@Override
	public boolean isConst() {
		return false;
	}

	@NotNull
	@Override
	public String getName() {
		return null;
	}

	@NotNull
	@Override
	public List<KParameter> getParameters() {
		return null;
	}

	@NotNull
	@Override
	public KType getReturnType() {
		return null;
	}

	@NotNull
	@Override
	public List<KTypeParameter> getTypeParameters() {
		return null;
	}

	@Override
	public T call(Object... objects) {
		return null;
	}

	@Override
	public T callBy(Map<KParameter, ?> map) {
		return null;
	}

	@Nullable
	@Override
	public KVisibility getVisibility() {
		return null;
	}

	@Override
	public boolean isFinal() {
		return false;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@NotNull
	@Override
	public List<Annotation> getAnnotations() {
		return null;
	}
}
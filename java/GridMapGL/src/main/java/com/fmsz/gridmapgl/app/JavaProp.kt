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

@file:JvmName("JavaProp")
package com.fmsz.gridmapgl.app
//https://stackoverflow.com/questions/47222807/implementing-a-kotlin-interface-in-java?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty0
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility


public class JavaProp<T>(val g: () -> T, val s: (T) -> T) : KMutableProperty0<T> {

	override val isConst: Boolean = false
	override val isOpen: Boolean = false
	override val annotations: List<Annotation> = listOf()
	override val isLateinit: Boolean = false
	override val isAbstract: Boolean = false
	override val isFinal: Boolean = false
	override val name: String = ""
	override val parameters: List<KParameter> = listOf()
	override val returnType: KType = TODO()
	override val typeParameters: List<KTypeParameter> = listOf()
	override val getter: KProperty0.Getter<T> = TODO()
	override val setter: KMutableProperty0.Setter<T> = TODO()
	override val visibility: KVisibility? = null

	override fun invoke(): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun set(value: T) {
		s(value)
	}

	override fun get(): T {
		return g()
	}

	override fun call(vararg args: Any?): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun callBy(args: Map<KParameter, Any?>): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getDelegate(): Any? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}
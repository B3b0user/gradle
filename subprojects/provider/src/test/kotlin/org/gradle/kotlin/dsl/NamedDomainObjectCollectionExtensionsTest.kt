package org.gradle.kotlin.dsl

import com.nhaarman.mockito_kotlin.KStubbing
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.PolymorphicDomainObjectContainer

import org.gradle.kotlin.dsl.fixtures.assertFailsWith
import org.gradle.kotlin.dsl.fixtures.matches

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Ignore
import org.junit.Test

import java.util.regex.Pattern

import kotlin.reflect.KClass


@Ignore("wip")
class NamedDomainObjectCollectionExtensionsTest {

    data class DomainObject(var foo: String? = null, var bar: String? = null)

    @Test
    fun `reified withType extension returns named collection`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor(element)
        val typedCollection = mock<NamedDomainObjectCollection<DomainObject>> {
            on { named("domainObject") } doReturn elementProvider
        }
        val container = mock<NamedDomainObjectCollection<Any>> {
            on { withType(DomainObject::class.java) } doReturn typedCollection
        }
        assertThat(
            container.withType<DomainObject>().named("domainObject").get(),
            sameInstance(element))
    }

    @Test
    fun `val domainObject by registering`() {

        val domainObjectProvider = mockDomainObjectProviderFor(DomainObject())
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { register("domainObject") } doReturn domainObjectProvider
        }

        container {

            val domainObject by registering

            inOrder(container, domainObjectProvider) {
                verify(container).register("domainObject")
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `val domainObject by registering { }`() {

        val domainObjectProvider = mockDomainObjectProviderFor(DomainObject())
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { register(eq("domainObject"), any<Action<DomainObject>>()) } doReturn domainObjectProvider
        }

        container {

            val domainObject by registering {
                assertInferredTypeOf(
                    this,
                    typeOf<DomainObject>()
                )
            }

            inOrder(container, domainObjectProvider) {
                verify(container).register(eq("domainObject"), any<Action<DomainObject>>())
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `val domainObject by registering(type)`() {

        val domainObjectProvider = mockDomainObjectProviderFor(DomainObject())
        val container = mock<PolymorphicDomainObjectContainer<Any>> {
            on { register("domainObject", DomainObject::class.java) } doReturn domainObjectProvider
        }

        container {

            val domainObject by registering(DomainObject::class)

            inOrder(container, domainObjectProvider) {
                verify(container).register("domainObject", DomainObject::class.java)
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `val domainObject by registering(type) { }`() {

        val domainObjectProvider = mockDomainObjectProviderFor(DomainObject())
        val container = mock<PolymorphicDomainObjectContainer<Any>> {
            onRegisterWithAction("domainObject", DomainObject::class, domainObjectProvider)
        }

        container {

            val domainObject by registering(DomainObject::class) {
                assertInferredTypeOf(
                    this,
                    typeOf<DomainObject>()
                )
            }

            inOrder(container) {
                verify(container).register(eq("domainObject"), eq(DomainObject::class.java), any<Action<DomainObject>>())
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `can getByName via indexer`() {

        val element = DomainObject()
        val container = mock<NamedDomainObjectCollection<DomainObject>> {
            on { getByName("domainObject") } doReturn element
        }
        assertThat(
            container["domainObject"],
            sameInstance(element))
    }

    @Test
    fun `can lazily access and configure existing element by name and type`() {

        val fooObject = DomainObject()
        val fooProvider = mockDomainObjectProviderFor(fooObject)

        val barObject = DomainObject()
        val barProvider = mockDomainObjectProviderFor(barObject)

        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { named("foo") } doReturn fooProvider
            on { named("bar") } doReturn barProvider
            on { getByName("foo") } doReturn fooObject
            on { getByName("bar") } doReturn barObject
        }

        container.named<DomainObject>("foo").configure {
            it.foo = "reified access"
        }
        container.named<DomainObject>("foo") {
            bar = "reified configuration"
        }

        container.named("bar", DomainObject::class).configure {
            it.foo = "typed access"
        }
        container.named("bar", DomainObject::class) {
            bar = "typed configuration"
        }

        assertThat(container["foo"].foo, equalTo("reified access"))
        assertThat(container["foo"].bar, equalTo("reified configuration"))

        assertThat(container["bar"].foo, equalTo("typed access"))
        assertThat(container["bar"].bar, equalTo("typed configuration"))
    }

    @Test
    fun `can access named element via delegated property`() {

        val element = DomainObject()
        val provider = mockDomainObjectProviderFor(element)
        val container = mock<NamedDomainObjectCollection<DomainObject>> {
            on { named("domainObject") } doReturn provider
        }

        val domainObject by container

        inOrder(container) {
            verify(container).named("domainObject")
            verifyNoMoreInteractions()
        }

        assertThat(
            domainObject.foo, // just to prove domainObject's type is inferred correctly
            nullValue())

        assertThat(
            domainObject,
            sameInstance(element))

        inOrder(container, provider) {
            verify(provider, times(2)).get()
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `val domainObject by existing`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor(element)
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            val domainObject by existing
            inOrder(container, elementProvider) {
                verify(container).named("domainObject")
                verifyNoMoreInteractions()
            }
            assertThat(domainObject, sameInstance(elementProvider))
            inOrder(elementProvider) {
                verifyNoMoreInteractions()
            }
        }

        container.apply {
            // regular syntax
            val domainObject by existing
            assertThat(domainObject, sameInstance(elementProvider))
        }
    }

    @Test
    fun `val domainObject by existing { }`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor(element)
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            val domainObject by existing {
                assertInferredTypeOf(
                    this,
                    typeOf<DomainObject>()
                )
            }

            inOrder(container, elementProvider) {
                verify(container).named("domainObject")
                verify(elementProvider).configure(any<Action<DomainObject>>())
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }

        container.apply {
            // regular syntax
            val domainObject by existing {
            }
            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `val domainObject by existing(type)`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor<Any>(element)
        val container = mock<NamedDomainObjectContainer<Any>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            val domainObject by existing(DomainObject::class)

            inOrder(container, elementProvider) {
                verify(container).named("domainObject")
                verify(elementProvider).configure(any<Action<Any>>())
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }

        container.apply {
            // regular syntax
            val domainObject by existing(DomainObject::class)
            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `val domainObject by existing(type) { }`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor(element)
        val container = mock<NamedDomainObjectContainer<Any>> {
            on { named<DomainObject>(eq("domainObject"), any<Class<DomainObject>>(), any<Action<DomainObject>>()) } doReturn elementProvider
        }

        container {
            // invoke syntax
            val domainObject by existing(DomainObject::class) {
                assertInferredTypeOf(
                    this,
                    typeOf<DomainObject>()
                )
            }

            inOrder(container, elementProvider) {
                verify(container).named(eq("domainObject"), eq(DomainObject::class.java), any<Action<Any>>())
                verifyNoMoreInteractions()
            }

            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }

        container.apply {
            // regular syntax
            val domainObject by existing(DomainObject::class) {
            }
            assertInferredTypeOf(
                domainObject,
                typeOf<NamedDomainObjectProvider<DomainObject>>()
            )
        }
    }

    @Test
    fun `can access named element by getting`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor(element)
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            val domainObject by getting
            inOrder(container, elementProvider) {
                verify(container).named("domainObject")
                verifyNoMoreInteractions()
            }
            assertThat(domainObject, sameInstance(element))
            inOrder(container, elementProvider) {
                verify(elementProvider).get()
                verifyNoMoreInteractions()
            }
        }

        container.apply {
            // regular syntax
            val domainObject by getting
            assertThat(domainObject, sameInstance(element))
        }
    }

    @Test
    fun `can configure named element by getting`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor(element)
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            @Suppress("unused_variable")
            val domainObject by getting { foo = "foo" }
            assertThat(element.foo, equalTo("foo"))
        }

        container.apply {
            // regular syntax
            @Suppress("unused_variable")
            val domainObject by getting { foo = "bar" }
            assertThat(element.foo, equalTo("bar"))
        }
    }

    @Test
    fun `can access named element by getting with type`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor<Any>(element)
        val container = mock<NamedDomainObjectContainer<Any>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            val domainObject by getting(DomainObject::class)
            assertThat(domainObject, sameInstance(element))
        }

        container.apply {
            // regular syntax
            val domainObject by getting(DomainObject::class)
            assertThat(domainObject, sameInstance(element))
        }
    }

    @Test
    fun `can configure named element by getting with type`() {

        val element = DomainObject()
        val elementProvider = mockDomainObjectProviderFor<Any>(element)
        val container = mock<NamedDomainObjectContainer<Any>> {
            on { named("domainObject") } doReturn elementProvider
        }

        container {
            // invoke syntax
            @Suppress("unused_variable")
            val domainObject by getting(DomainObject::class) { foo = "foo" }
            assertThat(element.foo, equalTo("foo"))
        }

        container.apply {
            // regular syntax
            @Suppress("unused_variable")
            val domainObject by getting(DomainObject::class) { foo = "bar" }
            assertThat(element.foo, equalTo("bar"))
        }
    }

    @Test
    fun `can register element by creating`() {

        val fooObject = DomainObject()
        val fooObjectProvider = mockDomainObjectProviderFor(fooObject)
        val barObject = DomainObject()
        val barObjectProvider = mockDomainObjectProviderFor(barObject)
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            on { register("foo") } doReturn fooObjectProvider
            on { register("bar") } doReturn barObjectProvider
        }

        container {
            // invoke syntax
            val foo by creating
            assertThat(foo.foo, nullValue())
        }

        container.apply {
            // regular syntax
            val bar by creating
            assertThat(bar.foo, nullValue())
        }
    }

    @Test
    fun `can register and configure element by creating`() {

        val fooObject = DomainObject()
        val fooObjectProvider = mockDomainObjectProviderFor(fooObject)
        val barObject = DomainObject()
        val barObjectProvider = mockDomainObjectProviderFor(barObject)
        val container = mock<NamedDomainObjectContainer<DomainObject>> {
            onRegisterWithAction("foo", fooObjectProvider)
            onRegisterWithAction("bar", barObjectProvider)
        }

        container {
            // invoke syntax
            val foo by creating { foo = "foo" }
            assertThat(foo.foo, equalTo("foo"))
        }

        container.apply {
            // regular syntax
            val bar by creating { foo = "bar" }
            assertThat(bar.foo, equalTo("bar"))
        }
    }

    @Test
    fun `can register element by creating with type`() {

        val fooObject = DomainObject()
        val fooObjectProvider = mockDomainObjectProviderFor(fooObject)
        val barObject = DomainObject()
        val barObjectProvider = mockDomainObjectProviderFor(barObject)
        val container = mock<PolymorphicDomainObjectContainer<Any>> {
            on { register("foo", DomainObject::class.java) } doReturn fooObjectProvider
            on { register("bar", DomainObject::class.java) } doReturn barObjectProvider
        }

        container {
            // invoke syntax
            val foo by creating(DomainObject::class)
            assertThat(foo.foo, nullValue())
        }

        container.apply {
            // regular syntax
            val bar by creating(DomainObject::class)
            assertThat(bar.foo, nullValue())
        }
    }

    @Test
    fun `can register and configure element by creating with type`() {

        val fooObject = DomainObject()
        val fooObjectProvider = mockDomainObjectProviderFor(fooObject)
        val barObject = DomainObject()
        val barObjectProvider = mockDomainObjectProviderFor(barObject)
        val container = mock<PolymorphicDomainObjectContainer<Any>> {
            onRegisterWithAction("foo", DomainObject::class, fooObjectProvider)
            onRegisterWithAction("bar", DomainObject::class, barObjectProvider)
        }

        container {
            // invoke syntax
            val foo by creating(DomainObject::class) { foo = "foo" }
            assertThat(foo.foo, equalTo("foo"))
        }

        container.apply {
            // regular syntax
            val bar by creating(DomainObject::class) { foo = "bar" }
            assertThat(bar.foo, equalTo("bar"))
        }
    }

    @Test
    fun `accessing existing element with wrong type gives proper error message`() {

        val obj = Object()
        val provider = mockDomainObjectProviderFor<Any>(obj)
        val container = mock<NamedDomainObjectCollection<Any>> {
            on { named("domainObject") } doReturn provider
            on { getByName("domainObject") } doReturn obj
        }

        fun assertFailsWithIllegalElementType(type: KClass<*>, container: Any, block: () -> Unit) {
            val error = assertFailsWith(IllegalArgumentException::class, block)
            assertThat(
                error.message,
                matches("Element 'domainObject' of type 'java\\.lang\\.Object' from container '${Pattern.quote(container.toString())}' cannot be cast to '${Pattern.quote(type.qualifiedName)}'\\."))
        }

        assertFailsWithIllegalElementType(DomainObject::class, container) {
            container.named<DomainObject>("domainObject").get()
        }

        val domainObject: DomainObject by container
        assertFailsWithIllegalElementType(DomainObject::class, provider) {
            println(domainObject)
        }
    }
}


internal
fun <T> KStubbing<NamedDomainObjectContainer<T>>.onRegisterWithAction(name: String, provider: NamedDomainObjectProvider<T>) {
    on { register(eq(name), any<Action<T>>()) } doAnswer {
        it.getArgument<Action<T>>(1).execute(provider.get())
        provider
    }
}


internal
fun <T : Any, U : T> KStubbing<PolymorphicDomainObjectContainer<T>>.onRegisterWithAction(name: String, type: KClass<U>, domainObjectProvider: NamedDomainObjectProvider<U>) {
    on { register(eq(name), eq(type.java), any<Action<U>>()) } doAnswer {
        it.getArgument<Action<U>>(2).execute(domainObjectProvider.get())
        domainObjectProvider
    }
}


internal
inline fun <reified T : Any> mockDomainObjectProviderFor(domainObject: T): NamedDomainObjectProvider<T> =
    mock {
        on { get() } doReturn domainObject
        on { configure(any()) } doAnswer {
            it.getArgument<Action<T>>(0).execute(domainObject)
        }
    }

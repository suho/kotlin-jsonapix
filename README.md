# **JsonApiX**

- JSON API X is a Kotlin multiplatform, annotation processor library
- Implements a parser between Kotlin classes and JSON API specification strings in both directions
- Includes Retrofit module for easy API implementations

### List of libraries used in the project

This chapter lists all the libraries that make a bigger contribution to the project. It is essential for any future contributor to get
familiar with them, before starting any work on the project. For that reason, links to the documentation are a part of the list.

- Kotlinx serialization - [kotlinx.serialization/serialization-guide.md](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
- Kotlin poet - https://square.github.io/kotlinpoet/

Along with the libraries documentation, the developer should also know his way around the JSON API, so here is a link to
the [specification](https://jsonapi.org/).

## Getting started

Firstly, make sure to include `mavenCentral()` in your buildscript and add the serialization plugin:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}
```

Then add the following dependencies and enable `kapt` and `kotlinx-serialization` plugins:

```groovy
plugins {
    id "kotlinx-serialization"
    id "kotlin-kapt"
}
```

```groovy
// Serialization API. Check the docs link above for newer versions
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

// Json API X
implementation("com.infinum.jsonapix:core:1.0.0-alpha02")
kapt("com.infinum.jsonapix:processor:1.0.0-alpha02")

// Optional: For Retrofit support
implementation("com.infinum.jsonapix:retrofit:1.0.0-alpha02")
```

## Usage

Let's take the example of a class named `Person`.

```kotlin
@Serializable
@JsonApiX(type = "person")
data class Person(
    val firstName: String,
    val lastName: String,
    val age: Int,
    @HasMany(type = "friend")
    val friends: List<Friend>,
    @HasOne(type = "dog")
    val dog: Dog?
)
```

`@JsonApiX` annotation takes a String type parameter and it must be combined with `@Serializable` from KotlinX Serialization. Type is later
used as a value for the `type` key in the JSON API specification.

`@HasOne` and `@HasMany` indicate the class relationships. They also take the type parameter, that must be the same as the one indicated in
the class definition.

In this case, `Dog` must also be annotated with `@Serializable` and `@JsonApiX(type = "dog")`, where `"dog"` is the type defined for
the `Dog` class.

All non-primitive fields of a class should be some kind of relationship.

Hit build and wrappers will be generated. You are now ready to serialize and deserialize your class in the form of JSON API string.

To access the serialization and deserialization features, you need to use the `TypeAdapter` interface. To get the type adapter for your
specific class, use the generated `TypeAdapterFactory`:

```kotlin
// Gets adapter for a single instance of Person
val adapter = TypeAdapterFactory().getAdapter(Person::class)

// Gets adapter for a list of Person instances
val listAdapter = TypeAdapterFactory().getListAdapter(Person::class)

adapter.convertToString(person) // Produces JSON API String from a Person instance
adapter.convertFromString(inputJsonString) // Produces Person instance from JSON API String

listAdapter.convertToString(person) // Produces JSON API String from a Person list
listAdapter.convertFromString(inputJsonString) // Produces Person list from JSON API String
```

### Nullability

JsonApiX relies on the `included` JSON array from the JSON API specification when deserializing relationships. If the input JSON API string
is missing the data of a non-nullable relationship in the `included` array, deserialization attempt will throw
a `JsonApiXMissingArgumentException`. On the other hand, nullable relationships will be evaluated as `null`.

### Errors

According to the JSON API specification, each object has an optional error array. The following interface is used as a root-level wrapper
for JSON API responses. It contains a nullable `errors` list, that is used to parse the errors.

```kotlin
interface JsonApiX<out Model> {
    val data: ResourceObject<Model>?
    val included: List<ResourceObject<*>>?
    val errors: List<Error>?
    val links: Links?
    val meta: Meta?
}
```

A single error is modeled to wrap the most common arguments of an error. Developer needs to make sure to model the error responses to match
this model.

```kotlin
class Error(
    val code: String,
    val title: String,
    val detail: String,
    val status: String
)
```

When using Retrofit, in the event of a network error a `HttpException` will be thrown. To extract the `Error` model from a response, you can
use the `HttpException.asJsonXHttpException()` extension, which will return a `JsonXHttpException`, containing the original `response` as
well as `errors` list.

```kotlin
try {
    val person = io { sampleApiService.fetchPerson() }
} catch (exception: HttpException) {
    val errors = exception.asJsonXHttpException().errors
    // Handle errors
}
```

## JsonApiModel - Handling `links` and `meta` JSON API fields

JSON API specification includes resources that are not necessarily a part of the original models.
JsonApiX provides a way to retrieve the `links` and `meta` values from the JSON API input, without including those fields in your model.

To achieve this, your model need to extend `JsonApiModel` abstract class.


```kotlin
@Serializable
@JsonApiX("person")
data class Person(
    val name: String?,
    val surname: String,
    val age: Int,
    @HasOne("dog")
    val myFavoriteDog: Dog? = null
) : JsonApiModel()
```

`JsonApiModel` is an abstract class which will provide you with getters and setters for links and meta objects.

### Links

Links can be included in the root object, relationships objects and resource objects (`data` key). 
By default, they have the following implementation:

```kotlin
class DefaultLinks(
    val self: String? = null,
    val related: String? = null,
    val first: String? = null,
    val last: String? = null,
    val next: String? = null,
    val prev: String? = null
) : Links
```

And the can be retrieved in the following way:

```kotlin
// Get root level links
person.rootLinks()

// Get relationships links
person.relationshipsLinks()

// Get resource object links
person.resourceLinks()
```

##### Custom links

Developers can define their own custom link models to adapt to the specific requirements.

Let's take this custom person links model as an example. 
Every custom links model must extend the `Links` interface and have a `JsonApiXLinks` annotation. 

```kotlin
@Serializable
@JsonApiXLinks(type = "person", placementStrategy = LinksPlacementStrategy.ROOT)
data class PersonLinks(
    val bioLink: String,
    val socialLink: String
) : Links
```

In this example, the annotation processor will automatically make the root-links type of a `Person` class to be `PersonLinks`. 
Developer needs to make sure that the `type` parameter value in `JsonApiXLinks` matches the one in the `JsonApiX` above the original model.
To retrieve them, a generic variant of the `rootLinks()` method is used.

```kotlin
// Gets root level links as a `PersonLinks` instance
person.rootLinks<PersonLinks>()
```

`LinksPlacementStartegy` enum is used to determine which links from the whole JSON API object will be replaced by a custom model.
It currently supports `ROOT`, `RELATIONSHIPS` and `DATA` links.


### Meta

In JSON API specification, Meta is an optional, free-format key-value pair. It doesn't have a predefined default model.
For that reason, in order to use the meta feature, developer must define a custom meta model for each class, where he wants to use it. 

Let's take this `PersonMeta` model as an example.
Every custom meta model must extend the `Meta` interface and have a `JsonApiXMeta` annotation.

```kotlin
@Serializable
@JsonApiXMeta(type = "person")
data class PersonMeta(val owner: String) : Meta
```

In this example, the annotation processor will automatically make the meta type of a `Person` class to be `PersonMeta`.
Developer needs to make sure that the `type` parameter value in `JsonApiXMeta` matches the one in the `JsonApiX` above the original model.
To retrieve a meta object, a generic variant of the `meta()` method is used.

```kotlin
// Gets meta object
person.meta<PersonMeta>()
```

## Retrofit

To enable Retrofit support, you need to add our custom converter in your retrofit builder. It takes an instance of `TypeAdapterFactory` as a
parameter. No additional steps needed.

```kotlin
Retrofit.Builder()
    .addConverterFactory(JsonXConverterFactory(TypeAdapterFactory()))
    .baseUrl("https://www.example.com")
    .build()
```

## Contributing

Feedback and code contributions are very much welcome. Just make a pull request with a short description of your changes. By making
contributions to this project you give permission for your code to be used under the same [license](LICENSE).

## License

```
Copyright 2021 Infinum

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Credits

Maintained and sponsored by [Infinum](http://www.infinum.com).

<p align="center">
  <a href='https://infinum.com'>
    <picture>
        <source srcset="https://assets.infinum.com/brand/logo/static/white.svg" media="(prefers-color-scheme: dark)">
        <img src="https://assets.infinum.com/brand/logo/static/default.svg">
    </picture>
  </a>
</p>


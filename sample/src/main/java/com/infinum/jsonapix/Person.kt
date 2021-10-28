package com.infinum.jsonapix

import com.infinum.jsonapix.annotations.HasMany
import com.infinum.jsonapix.annotations.HasOne
import com.infinum.jsonapix.annotations.JsonApiX
import kotlinx.serialization.Serializable

@Serializable
@JsonApiX("person")
data class Person(
    val name: String,
    val surname: String,
    val age: Int,
    @HasMany("dog")
    val allMyDogs: List<Dog>,
    @HasOne("dog")
    val myFavoriteDog: Dog
)

val personTestJsonString = """
    {
        "errors":null,
        "data":{
            "type":"person",
            "id":"1",
            "attributes":{
                "age":28,
                "name":"Stef",
                "surname":"Banek"
            },
            "relationships":{
                "myFavoriteDog":{
                    "data":{
                        "type":"dog",
                        "id":"1"
                    }
                },
                "allMyDogs":{
                    "data":[
                        { "type":"dog", "id":"2" },
                        { "type":"dog", "id":"3" }
                    ]
                }
            }
        },
        "included":[
            {
                "type":"dog",
                "id":"1",
                "attributes":{
                    "age":1,
                    "name":"Bella"
                }
            },
            {
                "type":"dog",
                "id":"2",
                "attributes":{
                    "age":2,
                    "name":"Bongo"
                }
            },
            {
                "type":"dog",
                "id":"3",
                "attributes":{
                    "age":3,
                    "name":"Sonic"
                }
            }
        ]
    }
""".trimIndent()

val personWithMissingDogTestJsonString = """
    {
        "errors":null,
        "data":{
            "type":"person",
            "id":"1",
            "attributes":{
                "age":28,
                "name":"Stef",
                "surname":"Banek"
            },
            "relationships":{
                "myFavoriteDog":{
                    "data":{
                        "type":"dog",
                        "id":"1"
                    }
                },
                "allMyDogs":{
                    "data":[
                        { "type":"dog", "id":"2" },
                        { "type":"dog", "id":"3" }
                    ]
                }
            }
        },
        "included":[
            {
                "type":"dog",
                "id":"2",
                "attributes":{
                    "age":2,
                    "name":"Bongo"
                }
            },
            {
                "type":"dog",
                "id":"3",
                "attributes":{
                    "age":3,
                    "name":"Sonic"
                }
            }
        ]
    }
""".trimIndent()

val personWithWrongTypeTestJsonString = """
    {
        "errors":null,
        "data":{
            "type":"dog",
            "id":"1",
            "attributes":{
                "age":28,
                "name":"Stef",
                "surname":"Banek"
            },
            "relationships":{
                "myFavoriteDog":{
                    "data":{
                        "type":"dog",
                        "id":"1"
                    }
                },
                "allMyDogs":{
                    "data":[
                        { "type":"dog", "id":"2" },
                        { "type":"dog", "id":"3" }
                    ]
                }
            }
        },
        "included":[
            {
                "type":"dog",
                "id":"1",
                "attributes":{
                    "age":1,
                    "name":"Bella"
                }
            },
            {
                "type":"dog",
                "id":"2",
                "attributes":{
                    "age":2,
                    "name":"Bongo"
                }
            },
            {
                "type":"dog",
                "id":"3",
                "attributes":{
                    "age":3,
                    "name":"Sonic"
                }
            }
        ]
    }
""".trimIndent()

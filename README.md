# Solid Spring Demo

A simple Solid application that can read and write pod data, built using Spring and Kotlin. To run, clone the repository and execute `./mvnw spring-boot:run` from the repository root. The application will start up on http://localhost:8080, which will redirect you to a login page. To login, enter either your WebID or your identity provider. Once logged in, you can update your profile name and read resources to which you have access, including those from other pods and/or hosts.

This application demonstrates how to implement the [Solid OIDC flow](https://solid.github.io/authentication-panel/solid-oidc/) (to my best ability and understanding) using [Spring](https://spring.io/). For more information, see [this blog post](https://voidstarzero.ca/post/653158759952269312/dpop-with-spring-boot-and-spring-security).

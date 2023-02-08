# JManager

A WebApp that allow you to manage your personal budget.

## Features

- You can register and login, so you can manipulate your own data
- You can add an "account" which be containers of your actual budget
- You can add sheets associated by an account, sheet is defined by a name, an amount a date and binary input of if this corresponds to an income or an outcome.
- You can easily visualize feedback to your budget state by months.
- You can easily visualize feedback to all of your registered accounts.


## Implementation

This architecture does separate the project in 2 distinct submodules :

* **Domain**, which contains all the business models and logics.
* **Infrastructure**, which briefly, contains all the dependencies that support the application. Such as the database and the api-rest implementation.

Annotation documentation in the common package is available to easily read which class is used for.

for example : 
```kotlin
@Service
@DatasourceAdapter
class LoginTransactionAdapter(val userRepository: UserRepository, val loginRepository: LoginRepository) : LoginTransactor
```
This class is an adapter to the dataSource, so here we are currently in the right-side of the hexagon.
This class implements **LoginTransactor** which is part of the domain : 
```kotlin
@PortToRight
interface LoginTransactor {
    fun login(userPseudonym: String, password: Password): Ticket?
    fun logout(userId: UserId, token: Token): Ticket?
    fun refresh(userId: UserId, token: Token): Ticket?
}
```
This class is a port which goes to the right-side of the hexagon.

## Advantages

Those are the main reasons I implemented this architecture in this project : 

* To be as flexible as possible by implementing each side separately.
* To feel free to change my dependencies at any time for future releases without break all the architecture. 
* To test each side independently, or only centered on the domain-side. 
* To make a first step with DDD (Domain driven design).
* To train the port/adapter design pattern.

## Dependencies : 

* [Kotlin](https://kotlinlang.org/), modern programming language based on the JVM.
* [Spring](https://spring.io/) 
* [PostgreSQL](https://www.postgresql.org/) for the data persistence.

## More

This project is my first attempt to build a **business-based application**. More precisely, an app centered by the customer business expectation.
I also wanted it to be as flexible as possible.
To do this, I tried to implement the concept of **Hexagonal architecture**, at least my version of this architecture.

Here you can find a good topic about this architecture [In french](https://blog.octo.com/architecture-hexagonale-trois-principes-et-un-exemple-dimplementation/) or another topic in [english](https://medium.com/ssense-tech/hexagonal-architecture-there-are-always-two-sides-to-every-story-bc0780ed7d9c).



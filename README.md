# JManager

A Web App that allow you to manage your personal budget and increase the global visual over it.

## Build

Run the following task to generate the complete JAR using the shadow plugin.
```shell
#On linux
gradle assemble

#On windows
.\gradlew assemble
```


## Features

- You can register and login, so you can manipulate your own data
- You can add an "account" which be containers of your actual budget
- You can add sheets associated by an account, sheet is defined by a name, an amount a date and binary input of if this corresponds to an income or an outcome.
- You can easily visualize feedback to your budget state by months.
- You can easily visualize feedback to all of your registered accounts.

## Dependencies

* [Kotlin](https://kotlinlang.org/), modern programming language based on the JVM.
* [Spring](https://spring.io/) 
* [PostgreSQL](https://www.postgresql.org/) for the data persistence.

## More

This project is my first attempt to build a **business-based application**. More precisely, an app centered by the customer business expectation.
I also wanted it to be as flexible as possible.
To do this, I tried to implement the concept of **Hexagonal architecture**, at least my version of this architecture.

Here you can find a good topic about this architecture [In french](https://blog.octo.com/architecture-hexagonale-trois-principes-et-un-exemple-dimplementation/) or another topic in [english](https://medium.com/ssense-tech/hexagonal-architecture-there-are-always-two-sides-to-every-story-bc0780ed7d9c).


